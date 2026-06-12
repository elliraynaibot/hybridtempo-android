package com.hybridtempo.android.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class LocalProfileEntity(
    @PrimaryKey val id: String = LOCAL_PROFILE_ID,
    val name: String,
    val raceName: String,
    val raceDate: String,
    val trainingStyle: String,
    val weeklyTrainingFrequency: Int,
    val goalsCsv: String,
    val preferredSessionLength: Int,
    val eveningReminderEnabled: Boolean,
    val eveningReminderHour: Int,
    val eveningReminderMinute: Int,
    val healthConnectEnabled: Boolean,
    val syncStatus: String = SyncStatus.Pending.value,
    val remoteId: String = "",
    val lastSyncedAt: String = "",
    val lastSyncError: String = "",
)

@Entity(tableName = "check_ins")
data class LocalCheckInEntity(
    @PrimaryKey val date: String,
    val energy: Int,
    val soreness: Int,
    val stress: Int,
    val mood: String,
    val timeAvailable: Int,
    val workoutType: String,
    val workoutDurationMinutes: Int,
    val workoutIntensity: Int,
    val sessionIntent: String,
    val createdAt: String,
    val syncStatus: String = SyncStatus.Pending.value,
    val remoteId: String = "",
    val lastSyncedAt: String = "",
    val lastSyncError: String = "",
)

@Entity(tableName = "breathwork_sessions")
data class LocalSessionEntity(
    @PrimaryKey val id: String,
    val protocol: String,
    val durationMinutes: Int,
    val cadence: String,
    val completed: Boolean,
    val completedAt: String,
    val breathSkillId: String,
    val perceivedControl: Int,
    val perceivedRecovery: Int,
    val reflectionFeeling: String,
    val reflectionNotes: String,
    val sessionStartedAt: String,
    val sessionEndedAt: String,
    val heartRateBeforeBpm: Int?,
    val heartRateAfterBpm: Int?,
    val heartRateDeltaBpm: Int?,
    val breathRhythmBeforePercent: Int?,
    val breathRhythmAfterPercent: Int?,
    val breathRhythmImprovementPercent: Int?,
    val syncStatus: String = SyncStatus.Pending.value,
    val remoteId: String = "",
    val lastSyncedAt: String = "",
    val lastSyncError: String = "",
)

enum class SyncStatus(val value: String) {
    Pending("pending"),
    Synced("synced"),
    Failed("failed"),
}

const val LOCAL_PROFILE_ID = "current"
