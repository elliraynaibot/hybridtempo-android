package com.hybridtempo.android.ui

enum class SessionReflectionFeeling(
    val value: String,
    val label: String,
) {
    Calmer("calmer", "Felt calmer"),
    Same("same", "About the same"),
    MoreActivated("more_activated", "More activated"),
}

data class SessionReflectionDraft(
    val perceivedControl: Int = 0,
    val perceivedRecovery: Int = 0,
    val feeling: SessionReflectionFeeling = SessionReflectionFeeling.Calmer,
    val notes: String = "",
) {
    val isComplete: Boolean
        get() = perceivedControl in 1..10 && perceivedRecovery in 1..10
}

data class SessionReflectionPresentation(
    val scoreSummary: String,
    val feelingLabel: String,
    val notes: String,
)

fun SessionReflectionDraft.toSessionReflectionPresentation(): SessionReflectionPresentation =
    SessionReflectionPresentation(
        scoreSummary = "Control $perceivedControl/10 - Recovery $perceivedRecovery/10",
        feelingLabel = feeling.label,
        notes = notes.trim(),
    )
