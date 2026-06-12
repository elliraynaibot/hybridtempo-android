package com.hybridtempo.android.ui

import com.hybridtempo.android.data.BreathworkSession
import java.util.Locale

data class HistoryImpactPresentation(
    val hasReflectionData: Boolean,
    val sampleLabel: String,
    val averageControlLabel: String,
    val averageRecoveryLabel: String,
    val message: String,
)

fun List<BreathworkSession>.toHistoryImpactPresentation(): HistoryImpactPresentation {
    val reflected = filter {
        it.completed &&
            it.perceivedControl in 1..10 &&
            it.perceivedRecovery in 1..10
    }

    if (reflected.isEmpty()) {
        return HistoryImpactPresentation(
            hasReflectionData = false,
            sampleLabel = "No reflections yet",
            averageControlLabel = "--",
            averageRecoveryLabel = "--",
            message = "Complete a session reflection to start seeing breathwork impact.",
        )
    }

    val averageControl = reflected.map { it.perceivedControl }.average()
    val averageRecovery = reflected.map { it.perceivedRecovery }.average()

    return HistoryImpactPresentation(
        hasReflectionData = true,
        sampleLabel = "${reflected.size} reflected ${if (reflected.size == 1) "session" else "sessions"}",
        averageControlLabel = averageControl.oneDecimal(),
        averageRecoveryLabel = averageRecovery.oneDecimal(),
        message = when {
            averageControl >= 7.0 && averageRecovery >= 7.0 ->
                "Breathwork is trending useful when control and recovery stay near 7+."
            averageControl >= averageRecovery ->
                "Control is improving faster than recovery. Keep watching cooldown response."
            else ->
                "Recovery is the stronger signal. Keep logging reflections after sessions."
        },
    )
}

private fun Double.oneDecimal(): String =
    String.format(Locale.US, "%.1f", this)
