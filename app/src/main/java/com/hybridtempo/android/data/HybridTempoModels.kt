package com.hybridtempo.android.data

import java.time.Instant
import java.time.LocalDate

data class AthleteProfile(
    val name: String = "",
    val raceName: String = "",
    val raceDate: String = "",
    val trainingStyle: String = "hybrid",
    val weeklyTrainingFrequency: Int = 5,
    val goals: List<String> = listOf("recovery", "race prep"),
    val preferredSessionLength: Int = 5,
    val eveningReminderEnabled: Boolean = false,
    val eveningReminderHour: Int = 20,
    val eveningReminderMinute: Int = 30,
    val healthConnectEnabled: Boolean = false,
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
    val sessionIntent: String = "post_workout",
    val createdAt: String = Instant.now().toString(),
)

data class BreathworkRecommendation(
    val protocol: String = "Post-training recovery",
    val durationMinutes: Int = 5,
    val rationale: String = "A steady recovery protocol fits the current training context.",
    val cadence: String = "4 second inhale · 5 second exhale",
    val breathworkProtocol: BreathworkProtocol = BreathworkProtocol.postTrainingRecovery(durationMinutes = 5),
    val breathSkillId: String = "",
    val trainingCue: String = "",
    val measurementFocus: String = "",
    val fallbackReason: String = "",
)

data class BreathworkProtocol(
    val category: String,
    val title: String,
    val durationMinutes: Int,
    val phases: List<BreathPhase>,
    val ambientTrackName: String = "ambient_loop",
) {
    val cycleSeconds: Int
        get() = phases.sumOf { it.seconds }.coerceAtLeast(1)

    val totalSeconds: Int
        get() = (durationMinutes * 60).coerceAtLeast(cycleSeconds)

    companion object {
        fun downregulation(durationMinutes: Int): BreathworkProtocol = BreathworkProtocol(
            category = "downregulation",
            title = "Downregulation",
            durationMinutes = durationMinutes,
            phases = listOf(
                BreathPhase(label = "Inhale", seconds = 4, instruction = "Draw air in through the nose", scaleTarget = 1.12f),
                BreathPhase(label = "Exhale", seconds = 6, instruction = "Let the exhale do the work", scaleTarget = 0.72f),
            ),
        )

        fun sleepTransition(durationMinutes: Int): BreathworkProtocol = BreathworkProtocol(
            category = "sleep_transition",
            title = "Sleep transition",
            durationMinutes = durationMinutes,
            phases = listOf(
                BreathPhase(label = "Inhale", seconds = 4, instruction = "Slow nasal inhale", scaleTarget = 1.08f),
                BreathPhase(label = "Exhale", seconds = 7, instruction = "Long quiet exhale", scaleTarget = 0.68f),
                BreathPhase(label = "Rest", seconds = 1, instruction = "Soften the jaw", scaleTarget = 0.68f),
            ),
        )

        fun recoveryReset(durationMinutes: Int): BreathworkProtocol = BreathworkProtocol(
            category = "recovery",
            title = "Recovery reset",
            durationMinutes = durationMinutes,
            phases = listOf(
                BreathPhase(label = "Inhale", seconds = 4, instruction = "Expand the ribs", scaleTarget = 1.08f),
                BreathPhase(label = "Exhale", seconds = 4, instruction = "Relax the shoulders", scaleTarget = 0.78f),
            ),
        )

        fun activation(durationMinutes: Int): BreathworkProtocol = BreathworkProtocol(
            category = "activation",
            title = "Activation",
            durationMinutes = durationMinutes,
            phases = listOf(
                BreathPhase(label = "Inhale", seconds = 3, instruction = "Crisp controlled inhale", scaleTarget = 1.14f),
                BreathPhase(label = "Exhale", seconds = 3, instruction = "Controlled reset", scaleTarget = 0.82f),
            ),
        )

        fun postTrainingRecovery(durationMinutes: Int): BreathworkProtocol = BreathworkProtocol(
            category = "post_training_recovery",
            title = "Post-training recovery",
            durationMinutes = durationMinutes,
            phases = listOf(
                BreathPhase(label = "Inhale", seconds = 4, instruction = "Breathe into the low ribs", scaleTarget = 1.1f),
                BreathPhase(label = "Exhale", seconds = 5, instruction = "Drop the heart rate down", scaleTarget = 0.74f),
            ),
        )
    }
}

data class BreathPhase(
    val label: String,
    val seconds: Int,
    val instruction: String,
    val scaleTarget: Float,
)

data class BreathworkSession(
    val id: String = "",
    val protocol: String,
    val durationMinutes: Int,
    val cadence: String,
    val completed: Boolean,
    val completedAt: String = Instant.now().toString(),
    val breathSkillId: String = "",
    val perceivedControl: Int = 0,
    val perceivedRecovery: Int = 0,
    val reflectionFeeling: String = "",
    val reflectionNotes: String = "",
)

data class SaveResult(
    val persisted: Boolean,
    val message: String,
)
