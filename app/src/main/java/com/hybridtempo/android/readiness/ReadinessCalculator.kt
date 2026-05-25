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
)

object ReadinessCalculator {
    fun calculate(
        latestCheckIn: DailyCheckIn?,
        recentSessions: List<BreathworkSession>,
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
            )
        }

        val consistencyBonus = recentSessions
            .count { it.completed && it.completedAt.toLocalDateOrNull()?.let { date -> date >= today.minusDays(6) } == true }
            .coerceAtMost(6)

        val rawScore = 50 +
            ((latestCheckIn.energy - 5) * 5) +
            ((5 - latestCheckIn.stress) * 4) +
            ((5 - latestCheckIn.soreness) * 3) +
            consistencyBonus

        val score = rawScore.coerceIn(20, 95)
        return ReadinessScore(
            score = score,
            label = score.toReadinessLabel(),
            summary = latestCheckIn.toReadinessSummary(),
            nudge = buildNudge(score, consistencyBonus, raceName, raceDate, today),
        )
    }

    private fun DailyCheckIn.toReadinessSummary(): String {
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

        return "$energySignal; $loadSignal."
    }

    private fun buildNudge(
        score: Int,
        consistencyBonus: Int,
        raceName: String,
        raceDate: String,
        today: LocalDate,
    ): String {
        val raceContext = raceDate.toLocalDateOrNull()?.let { date ->
            val days = ChronoUnit.DAYS.between(today, date).toInt().coerceAtLeast(0)
            val name = raceName.takeIf { it.isNotBlank() } ?: "race day"
            " ${days} days until $name."
        }.orEmpty()

        return when {
            score >= 75 && consistencyBonus >= 3 -> "Keep the rhythm: today's signal supports normal training and a short recovery session.$raceContext"
            score >= 75 -> "Good signal today. Add a short session to keep recovery consistency moving.$raceContext"
            score >= 60 -> "Stay steady: use breathwork to downshift before adding more training stress.$raceContext"
            else -> "Prioritize recovery today: choose a longer downregulation or sleep-transition session.$raceContext"
        }
    }

    private fun Int.toReadinessLabel(): String = when {
        this >= 75 -> "Ready"
        this >= 60 -> "Building"
        this >= 45 -> "Manage load"
        else -> "Recover first"
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
