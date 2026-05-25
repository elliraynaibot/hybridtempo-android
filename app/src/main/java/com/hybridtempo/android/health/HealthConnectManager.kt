package com.hybridtempo.android.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.hybridtempo.android.readiness.HealthMetricsSnapshot
import java.time.Duration
import java.time.Instant

enum class HealthConnectAvailability {
    Available,
    UpdateRequired,
    Unavailable,
}

data class HealthConnectUiStatus(
    val availability: HealthConnectAvailability = HealthConnectAvailability.Unavailable,
    val enabled: Boolean = false,
    val grantedPermissions: Set<String> = emptySet(),
    val metrics: HealthMetricsSnapshot? = null,
    val message: String = "Health Connect is optional.",
) {
    val hasRequiredPermissions: Boolean
        get() = grantedPermissions.containsAll(HealthConnectManager.PERMISSIONS)
}

class HealthConnectManager(private val context: Context) {
    fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UpdateRequired
        else -> HealthConnectAvailability.Unavailable
    }

    suspend fun grantedPermissions(): Set<String> {
        if (availability() != HealthConnectAvailability.Available) return emptySet()
        return client().permissionController.getGrantedPermissions()
    }

    suspend fun readSnapshot(grantedPermissions: Set<String>? = null): HealthMetricsSnapshot? {
        if (availability() != HealthConnectAvailability.Available) return null
        val client = client()
        val permissions = grantedPermissions ?: grantedPermissions()
        val now = Instant.now()
        val sleepMinutes = if (SLEEP_PERMISSION in permissions) {
            readSleepMinutes(client, now)
        } else {
            null
        }
        val workouts = if (EXERCISE_PERMISSION in permissions) {
            readWorkoutCount(client, now)
        } else {
            null
        }
        val restingHeartRate = if (RESTING_HEART_RATE_PERMISSION in permissions) {
            readRestingHeartRate(client, now)
        } else {
            null
        }

        val snapshot = HealthMetricsSnapshot(
            sleepMinutesLastNight = sleepMinutes,
            workoutsLast7Days = workouts,
            restingHeartRateBpm = restingHeartRate,
        )

        return snapshot.takeIf { it.hasData }
    }

    private suspend fun readSleepMinutes(
        client: HealthConnectClient,
        now: Instant,
    ): Int? = runCatching {
        client.readRecords(
            ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(Duration.ofHours(36)), now),
            ),
        ).records
            .maxByOrNull { it.endTime }
            ?.let { Duration.between(it.startTime, it.endTime).toMinutes().toInt() }
    }.getOrNull()

    private suspend fun readWorkoutCount(
        client: HealthConnectClient,
        now: Instant,
    ): Int? = runCatching {
        client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(Duration.ofDays(7)), now),
            ),
        ).records.size
    }.getOrNull()

    private suspend fun readRestingHeartRate(
        client: HealthConnectClient,
        now: Instant,
    ): Long? = runCatching {
        client.readRecords(
            ReadRecordsRequest(
                recordType = RestingHeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(Duration.ofDays(7)), now),
            ),
        ).records
            .maxByOrNull { it.time }
            ?.beatsPerMinute
    }.getOrNull()

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    companion object {
        private val SLEEP_PERMISSION = HealthPermission.getReadPermission(SleepSessionRecord::class)
        private val EXERCISE_PERMISSION = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        private val RESTING_HEART_RATE_PERMISSION = HealthPermission.getReadPermission(RestingHeartRateRecord::class)

        val PERMISSIONS: Set<String> = setOf(
            SLEEP_PERMISSION,
            EXERCISE_PERMISSION,
            RESTING_HEART_RATE_PERMISSION,
        )

        fun permissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
            PermissionController.createRequestPermissionResultContract()
    }
}
