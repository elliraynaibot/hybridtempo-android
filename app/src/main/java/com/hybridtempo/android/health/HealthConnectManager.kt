package com.hybridtempo.android.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.hybridtempo.android.domain.model.ImportedWorkout
import com.hybridtempo.android.domain.model.WorkoutType
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
        val snapshot = HealthMetricsSnapshot(
            sleepMinutesLastNight = sleepMinutes,
            workoutsLast7Days = workouts,
        )

        return snapshot.takeIf { it.hasData }
    }

    suspend fun readRecentWorkouts(grantedPermissions: Set<String>? = null): List<ImportedWorkout> {
        if (availability() != HealthConnectAvailability.Available) return emptyList()
        val permissions = grantedPermissions ?: grantedPermissions()
        if (EXERCISE_PERMISSION !in permissions) return emptyList()

        val now = Instant.now()
        return readRecentExerciseSessions(client(), now)
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

    private suspend fun readRecentExerciseSessions(
        client: HealthConnectClient,
        now: Instant,
    ): List<ImportedWorkout> = runCatching {
        client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(now.minus(Duration.ofDays(2)), now),
            ),
        ).records
            .sortedByDescending { it.endTime }
            .take(3)
            .map { record ->
                ImportedWorkout(
                    id = record.metadata.id.ifBlank { "${record.startTime}-${record.endTime}-${record.exerciseType}" },
                    source = record.metadata.dataOrigin.packageName.toHealthSourceLabel(),
                    workoutType = record.exerciseType.toWorkoutType(),
                    startedAt = record.startTime,
                    endedAt = record.endTime,
                )
            }
    }.getOrDefault(emptyList())

    private fun client(): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    companion object {
        private val SLEEP_PERMISSION = HealthPermission.getReadPermission(SleepSessionRecord::class)
        private val EXERCISE_PERMISSION = HealthPermission.getReadPermission(ExerciseSessionRecord::class)

        val PERMISSIONS: Set<String> = setOf(
            SLEEP_PERMISSION,
            EXERCISE_PERMISSION,
        )

        fun permissionsContract(): ActivityResultContract<Set<String>, Set<String>> =
            PermissionController.createRequestPermissionResultContract()
    }
}

private fun String.toHealthSourceLabel(): String = when {
    contains("fitbit", ignoreCase = true) -> "Google Health"
    contains("google", ignoreCase = true) -> "Google Health"
    contains("samsung", ignoreCase = true) -> "Samsung Health"
    contains("strava", ignoreCase = true) -> "Strava"
    else -> "Health Connect"
}

private fun Int.toWorkoutType(): WorkoutType = when (this) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> WorkoutType.RUNNING
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> WorkoutType.CYCLING
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> WorkoutType.ROWING
    ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> WorkoutType.INTERVALS
    ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> WorkoutType.STRENGTH
    ExerciseSessionRecord.EXERCISE_TYPE_YOGA,
    ExerciseSessionRecord.EXERCISE_TYPE_PILATES,
    ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> WorkoutType.MOBILITY_RECOVERY
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> WorkoutType.EASY_RECOVERY
    else -> WorkoutType.CONDITIONING
}
