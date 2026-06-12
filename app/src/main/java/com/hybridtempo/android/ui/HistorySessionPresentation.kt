package com.hybridtempo.android.ui

import com.hybridtempo.android.data.BreathworkSession

data class HistorySessionPresentation(
    val title: String,
    val durationLabel: String,
    val reflectionSummary: String,
    val feelingLabel: String,
    val notes: String,
    val heartRateSummary: String,
    val hasReflection: Boolean,
)

fun BreathworkSession.toHistorySessionPresentation(): HistorySessionPresentation {
    val hasReflection = perceivedControl in 1..10 && perceivedRecovery in 1..10

    return HistorySessionPresentation(
        title = protocol,
        durationLabel = "${durationMinutes}m",
        reflectionSummary = if (hasReflection) {
            "Control $perceivedControl/10 - Recovery $perceivedRecovery/10"
        } else {
            ""
        },
        feelingLabel = if (hasReflection) reflectionFeeling.toFeelingLabel() else "",
        notes = reflectionNotes.trim(),
        heartRateSummary = toHeartRateSummary(),
        hasReflection = hasReflection,
    )
}

private fun String.toFeelingLabel(): String = when (this) {
    SessionReflectionFeeling.Calmer.value -> SessionReflectionFeeling.Calmer.label
    SessionReflectionFeeling.Same.value -> SessionReflectionFeeling.Same.label
    SessionReflectionFeeling.MoreActivated.value -> SessionReflectionFeeling.MoreActivated.label
    else -> ifBlank { "Reflection saved" }
}

private fun BreathworkSession.toHeartRateSummary(): String {
    val before = heartRateBeforeBpm
    val after = heartRateAfterBpm
    val delta = heartRateDeltaBpm

    return if (before != null && after != null && delta != null) {
        "HR $before -> $after bpm (${delta.toSignedString()})"
    } else if (before != null) {
        "Starting HR locked: $before bpm. No post-session HR sample yet."
    } else {
        "No heart-rate samples found for this session."
    }
}

private fun Int.toSignedString(): String = if (this > 0) "+$this" else toString()
