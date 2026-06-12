package com.hybridtempo.android.domain.model

import java.time.Instant
import java.time.LocalDate

enum class TrainingStyle {
    RUNNER,
    LIFTER,
    HYBRID,
    CYCLIST,
    OTHER,
}

enum class TrainingIntensity {
    EASY,
    MODERATE,
    HARD,
    MAX_EFFORT_COMPETITION,
}

data class UserProfile(
    val id: String,
    val name: String,
    val trainingStyle: TrainingStyle,
    val weeklyTrainingSessions: Int,
    val goals: Set<String>,
    val preferredSessionMinutes: Int,
    val raceName: String? = null,
    val raceDate: LocalDate? = null,
    val healthConnectEnabled: Boolean = false,
)

data class WorkoutPlan(
    val id: String,
    val scheduledFor: LocalDate,
    val workoutType: WorkoutType,
    val expectedIntensity: TrainingIntensity,
    val expectedDurationMinutes: Int,
    val notes: String? = null,
)

data class ImportedWorkout(
    val id: String,
    val source: String,
    val workoutType: WorkoutType,
    val startedAt: Instant,
    val endedAt: Instant,
    val intensity: TrainingIntensity? = null,
)

data class HeartRateSample(
    val workoutId: String,
    val measuredAt: Instant,
    val beatsPerMinute: Int,
)
