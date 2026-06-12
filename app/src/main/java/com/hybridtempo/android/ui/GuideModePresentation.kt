package com.hybridtempo.android.ui

data class GuideModePresentation(
    val label: String,
    val value: String,
    val purpose: String,
    val windowTitle: String,
)

data class ActiveGuidePresentation(
    val eyebrow: String,
    val title: String,
    val primaryCue: String,
    val supportingCue: String,
    val duringSetCue: String,
)

fun defaultGuideModes(): List<GuideModePresentation> = listOf(
    GuideModePresentation(
        label = "Learn",
        value = "learn",
        purpose = "Practice the rhythm while calm.",
        windowTitle = "Learn the skill",
    ),
    GuideModePresentation(
        label = "Pre-set",
        value = "pre_set",
        purpose = "Settle before effort.",
        windowTitle = "Before the set",
    ),
    GuideModePresentation(
        label = "Between sets",
        value = "between_sets",
        purpose = "Regain control during rest.",
        windowTitle = "Between reps",
    ),
    GuideModePresentation(
        label = "Post-workout",
        value = "post_workout",
        purpose = "Downshift after training.",
        windowTitle = "After the set",
    ),
)

fun GuideModePresentation.toActiveGuidePresentation(): ActiveGuidePresentation = when (value) {
    "learn" -> ActiveGuidePresentation(
        eyebrow = "Learn mode",
        title = "Practice the rhythm",
        primaryCue = "Inhale easy. Exhale longer.",
        supportingCue = "Learn it now so it is simple under fatigue.",
        duringSetCue = "Start smooth.",
    )

    "pre_set" -> ActiveGuidePresentation(
        eyebrow = "Pre-set",
        title = "Settle before effort",
        primaryCue = "Exhale longer.",
        supportingCue = "Start controlled instead of chasing air early.",
        duringSetCue = "Start controlled.",
    )

    "between_sets" -> ActiveGuidePresentation(
        eyebrow = "Recovery",
        title = "Recover between sets",
        primaryCue = "Exhale longer.",
        supportingCue = "First part of rest: regain control.",
        duringSetCue = "Stay rhythmic.",
    )

    "post_workout" -> ActiveGuidePresentation(
        eyebrow = "Cooldown",
        title = "Downshift after training",
        primaryCue = "Slow down.",
        supportingCue = "Bring the system down before you leave the session.",
        duringSetCue = "Finish calm.",
    )

    else -> ActiveGuidePresentation(
        eyebrow = "Guide",
        title = windowTitle,
        primaryCue = "Breathe where you can.",
        supportingCue = purpose,
        duringSetCue = "Stay controlled.",
    )
}
