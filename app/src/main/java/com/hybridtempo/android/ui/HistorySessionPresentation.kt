package com.hybridtempo.android.ui

import com.hybridtempo.android.data.BreathworkSession
import com.hybridtempo.android.audio.BreathRhythmAnalyzer
import com.hybridtempo.android.audio.BreathRhythmCheckResult

data class HistorySessionPresentation(
    val title: String,
    val durationLabel: String,
    val reflectionSummary: String,
    val feelingLabel: String,
    val notes: String,
    val breathRhythmSummary: String,
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
        breathRhythmSummary = toBreathRhythmSummary(),
        hasReflection = hasReflection,
    )
}

private fun String.toFeelingLabel(): String = when (this) {
    SessionReflectionFeeling.Calmer.value -> SessionReflectionFeeling.Calmer.label
    SessionReflectionFeeling.Same.value -> SessionReflectionFeeling.Same.label
    SessionReflectionFeeling.MoreActivated.value -> SessionReflectionFeeling.MoreActivated.label
    else -> ifBlank { "Reflection saved" }
}

private fun BreathworkSession.toBreathRhythmSummary(): String = BreathRhythmAnalyzer.compare(
    before = breathRhythmBeforePercent?.toBreathCheckResult(),
    after = breathRhythmAfterPercent?.toBreathCheckResult(),
)

private fun Int.toBreathCheckResult(): BreathRhythmCheckResult = BreathRhythmCheckResult(
    rhythmMatchedPercent = this,
    detectedBreaths = 0,
    durationSeconds = 20,
)
