package com.hybridtempo.android.data

import java.time.Instant
import java.time.LocalDate

data class AthleteProfile(
    val name: String = "",
    val raceDate: String = "",
    val trainingStyle: String = "hybrid",
    val goals: List<String> = listOf("recovery", "race prep"),
    val preferredSessionLength: Int = 5,
)

data class DailyCheckIn(
    val date: String = LocalDate.now().toString(),
    val energy: Int = 6,
    val soreness: Int = 4,
    val stress: Int = 5,
    val mood: String = "steady",
    val timeAvailable: Int = 5,
    val workoutType: String = "Hybrid",
    val workoutDurationMinutes: Int = 45,
    val workoutIntensity: Int = 7,
    val createdAt: String = Instant.now().toString(),
)

data class BreathworkRecommendation(
    val protocol: String = "Post-training recovery",
    val durationMinutes: Int = 5,
    val rationale: String = "A steady recovery protocol fits the current training context.",
    val cadence: String = "4 second inhale · 5 second exhale",
)

data class BreathworkSession(
    val id: String = "",
    val protocol: String,
    val durationMinutes: Int,
    val cadence: String,
    val completed: Boolean,
    val completedAt: String = Instant.now().toString(),
)

data class SaveResult(
    val persisted: Boolean,
    val message: String,
)
