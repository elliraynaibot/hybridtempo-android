package com.hybridtempo.android.ui

data class BreathWindowPresentation(
    val number: Int,
    val title: String,
    val cue: String,
    val colorRole: BreathWindowColorRole,
)

enum class BreathWindowColorRole {
    Settle,
    Effort,
    Reset,
    Recover,
}

data class BreathWindowsPresentation(
    val eyebrow: String = "Breath Windows",
    val title: String = "Win the\nbreath windows",
    val subtitle: String = "Do not try to breathe perfectly every second. Use the small moments that actually matter.",
    val windows: List<BreathWindowPresentation> = defaultBreathWindows(),
    val reminder: String = "Before the set. During the set. Between reps. After the set.",
    val primaryAction: String = "VIEW PROTOCOL",
    val secondaryAction: String = "ADJUST CHECK-IN",
)

fun defaultBreathWindows(): List<BreathWindowPresentation> = listOf(
    BreathWindowPresentation(
        number = 1,
        title = "Before the set",
        cue = "Settle. Start controlled.",
        colorRole = BreathWindowColorRole.Settle,
    ),
    BreathWindowPresentation(
        number = 2,
        title = "During the set",
        cue = "Stay rhythmic or brace.",
        colorRole = BreathWindowColorRole.Effort,
    ),
    BreathWindowPresentation(
        number = 3,
        title = "Between reps",
        cue = "Reset before the next rep.",
        colorRole = BreathWindowColorRole.Reset,
    ),
    BreathWindowPresentation(
        number = 4,
        title = "After the set",
        cue = "Recover. Bring breathing down.",
        colorRole = BreathWindowColorRole.Recover,
    ),
)
