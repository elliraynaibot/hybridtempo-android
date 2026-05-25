package com.hybridtempo.android.readiness

import com.hybridtempo.android.data.BreathworkSession
import com.hybridtempo.android.data.DailyCheckIn
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

data class ReadinessScore(
    val score: Int,
    val label: String,
    val summary: String,
    val nudge: String,
    val sourceLabel: String = "Manual inputs",
    val confidenceLabel: String = "Low",
    val basedOn: List<String> = emptyList(),
    val missingSignals: List<String> = emptyList(),
    val nextAction: String = "Complete today's check-in to calibrate readiness.",
)

data class HealthMetricsSnapshot(
    val sleepMinutesLastNight: Int? = null,
    val workoutsLast7Days: Int? = null,
    val restingHeartRateBpm: Long? = null,
) {
    val hasData: Boolean
        get() = sleepMinutesLastNight != null || workoutsLast7Days != null || restingHeartRateBpm != null
}

data class RaceCountdown(
    val title: String,
    val label: String,
)

data class ManualDetailsPrompt(
    val title: String,
    val body: String,
    val actionLabel: String = "Start check-in",
)

object ReadinessCalculator {
    fun calculate(
        latestCheckIn: DailyCheckIn?,
        recentSessions: List<BreathworkSession>,
        healthMetrics: HealthMetricsSnapshot? = null,
        today: LocalDate = LocalDate.now(),
        raceName: String = "",
        raceDate: String = "",
    ): ReadinessScore {
        if (latestCheckIn == null) {
            return ReadinessScore(
                score = 50,
                label = "Needs check-in",
                summary = "Readiness will update after your first daily check-in.",
                nudge = "Complete a check-in so HybridTempo can calibrate today's recovery signal.",
                confidenceLabel = "Low",
                basedOn = recentSessions.toBreathworkSignal(),
                missingSignals = healthMetrics.toMissingSignals(hasCheckIn = false),
                nextAction = "Complete today's check-in to replace the baseline with your current energy, stress, soreness, and workout context.",
            )
        }

        val consistencyBonus = recentSessions
            .count { it.completed && it.completedAt.toLocalDateOrNull()?.let { date -> date >= today.minusDays(6) } == true }
            .coerceAtMost(6)
        val basedOn = listOf(
            "Today's manual check-in: energy ${latestCheckIn.energy}/10, stress ${latestCheckIn.stress}/10, soreness ${latestCheckIn.soreness}/10",
        ) + recentSessions.toBreathworkSignal() + healthMetrics.toHealthSignals()
        val missingSignals = healthMetrics.toMissingSignals(hasCheckIn = true)

        val rawScore = 50 +
            ((latestCheckIn.energy - 5) * 5) +
            ((5 - latestCheckIn.stress) * 4) +
            ((5 - latestCheckIn.soreness) * 3) +
            consistencyBonus +
            healthMetrics.toHealthScoreAdjustment()

        val score = rawScore.coerceIn(20, 95)
        return ReadinessScore(
            score = score,
            label = score.toReadinessLabel(),
            summary = latestCheckIn.toReadinessSummary(healthMetrics),
            nudge = buildNudge(score, consistencyBonus),
            sourceLabel = if (healthMetrics?.hasData == true) "Manual + Health Connect" else "Manual inputs",
            confidenceLabel = confidenceLabel(hasCheckIn = true, healthMetrics = healthMetrics),
            basedOn = basedOn,
            missingSignals = missingSignals,
            nextAction = buildNextAction(score, consistencyBonus, missingSignals),
        )
    }

    private fun List<BreathworkSession>.toBreathworkSignal(): List<String> {
        val completedCount = count { it.completed }
        return if (completedCount > 0) {
            listOf("$completedCount breathwork sessions in your recent history")
        } else {
            emptyList()
        }
    }

    private fun HealthMetricsSnapshot?.toHealthSignals(): List<String> {
        if (this?.hasData != true) return emptyList()

        return listOfNotNull(
            sleepMinutesLastNight?.let { "Health Connect sleep: ${it / 60}h ${it % 60}m last night" },
            workoutsLast7Days?.let { "Health Connect workouts: $it sessions in 7 days" },
            restingHeartRateBpm?.let { "Health Connect resting HR: $it bpm" },
        )
    }

    private fun HealthMetricsSnapshot?.toMissingSignals(hasCheckIn: Boolean): List<String> {
        val missing = mutableListOf<String>()
        if (!hasCheckIn) {
            missing += "Manual check-in today"
        }
        if (this?.sleepMinutesLastNight == null) {
            missing += "Recent sleep data from Health Connect"
        }
        if (this?.workoutsLast7Days == null) {
            missing += "Recent workout data from Health Connect"
        }
        if (this?.restingHeartRateBpm == null) {
            missing += "Recent resting HR from Health Connect"
        }
        return missing
    }

    private fun confidenceLabel(
        hasCheckIn: Boolean,
        healthMetrics: HealthMetricsSnapshot?,
    ): String {
        if (!hasCheckIn) return "Low"
        val healthSignalCount = listOf(
            healthMetrics?.sleepMinutesLastNight,
            healthMetrics?.workoutsLast7Days,
            healthMetrics?.restingHeartRateBpm,
        ).count { it != null }

        return when {
            healthSignalCount >= 2 -> "High"
            healthSignalCount == 1 -> "Medium"
            else -> "Medium"
        }
    }

    private fun buildNextAction(
        score: Int,
        consistencyBonus: Int,
        missingSignals: List<String>,
    ): String = when {
        missingSignals.any { it.contains("sleep", ignoreCase = true) } -> "Complete today's check-in and keep Health Connect connected so sleep data can improve the estimate."
        score < 60 -> "Choose a downregulation session today and protect sleep tonight."
        consistencyBonus < 3 -> "Complete a short recovery session to build a stronger weekly regulation habit."
        else -> "Keep the current recovery rhythm and check in again tomorrow."
    }

    private fun DailyCheckIn.toReadinessSummary(healthMetrics: HealthMetricsSnapshot?): String {
        val energySignal = if (energy >= 7) {
            "Energy is supporting readiness"
        } else if (energy <= 4) {
            "Energy is the main limiter"
        } else {
            "Energy is steady"
        }

        val loadSignal = when {
            stress >= 7 && soreness >= 7 -> "stress and soreness are both elevated"
            stress >= 7 -> "stress is elevated"
            soreness >= 7 -> "soreness is elevated"
            else -> "stress and soreness are manageable"
        }

        val healthSignal = healthMetrics?.sleepMinutesLastNight?.let { sleepMinutes ->
            if (sleepMinutes >= 420) {
                " sleep data supports recovery."
            } else if (sleepMinutes < 360) {
                " sleep was short, so recovery needs attention."
            } else {
                " sleep was moderate."
            }
        }.orEmpty()

        return "$energySignal; $loadSignal.$healthSignal"
    }

    private fun HealthMetricsSnapshot?.toHealthScoreAdjustment(): Int {
        if (this?.hasData != true) return 0

        val sleepAdjustment = sleepMinutesLastNight?.let {
            when {
                it >= 420 -> 5
                it < 360 -> -5
                else -> 0
            }
        } ?: 0

        val workoutAdjustment = workoutsLast7Days?.let {
            when {
                it >= 6 -> -2
                it in 2..5 -> 2
                else -> 0
            }
        } ?: 0

        val heartRateAdjustment = restingHeartRateBpm?.let {
            when {
                it <= 55 -> 2
                it >= 75 -> -3
                else -> 0
            }
        } ?: 0

        return sleepAdjustment + workoutAdjustment + heartRateAdjustment
    }

    private fun buildNudge(
        score: Int,
        consistencyBonus: Int,
    ): String {
        return when {
            score >= 75 && consistencyBonus >= 3 -> "Keep the rhythm: today's signal supports normal training and a short recovery session."
            score >= 75 -> "Good signal today. Add a short session to keep recovery consistency moving."
            score >= 60 -> "Stay steady: use breathwork to downshift before adding more training stress."
            else -> "Prioritize recovery today: choose a longer downregulation or sleep-transition session."
        }
    }

    private fun Int.toReadinessLabel(): String = when {
        this >= 75 -> "Ready"
        this >= 60 -> "Building"
        this >= 45 -> "Manage load"
        else -> "Recover first"
    }
}

object ManualDetailsPromptCalculator {
    fun calculate(
        latestCheckIn: DailyCheckIn?,
        healthMetrics: HealthMetricsSnapshot?,
        healthConnectEnabled: Boolean,
    ): ManualDetailsPrompt? {
        if (latestCheckIn != null) return null

        return if (healthConnectEnabled && healthMetrics?.hasData != true) {
            ManualDetailsPrompt(
                title = "Add today's manual check-in",
                body = "Health Connect is connected, but there is not enough recent signal yet. Energy, stress, and soreness help calibrate readiness today.",
            )
        } else {
            ManualDetailsPrompt(
                title = "Calibrate today's readiness",
                body = "Add energy, stress, soreness, and workout context so the recommendation reflects how you feel right now.",
            )
        }
    }
}

object RaceCountdownCalculator {
    fun calculate(
        raceName: String,
        raceDate: String,
        today: LocalDate = LocalDate.now(),
    ): RaceCountdown? {
        val date = raceDate.toLocalDateOrNull() ?: return null
        val title = raceName.takeIf { it.isNotBlank() } ?: "Race day"
        val days = ChronoUnit.DAYS.between(today, date).toInt()
        val label = when {
            days > 1 -> "$days days out"
            days == 1 -> "1 day out"
            days == 0 -> "Race day is today"
            else -> "Race date has passed"
        }

        return RaceCountdown(title = title, label = label)
    }
}

private fun String.toLocalDateOrNull(): LocalDate? = try {
    OffsetDateTime.parse(this).toLocalDate()
} catch (_: DateTimeParseException) {
    try {
        LocalDate.parse(take(10))
    } catch (_: DateTimeParseException) {
        null
    }
}
