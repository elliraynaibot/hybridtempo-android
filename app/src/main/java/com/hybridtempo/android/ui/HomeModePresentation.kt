package com.hybridtempo.android.ui

data class HomePresentation(
    val eyebrow: String = "HybridTempo",
    val title: String = "Breathwork for where you are in the workout",
    val subtitle: String = "Choose before training to prime your body, or after training to downshift and recover.",
    val primaryAction: String = "Choose your breathwork",
    val flowSummary: String = "Start with the moment: before workout or after workout. HybridTempo recommends the breathwork that fits.",
    val secondaryActions: List<HomeModePresentation> = defaultHomeModes(),
)

data class HomeModePresentation(
    val label: String,
    val action: String,
    val body: String,
    val sessionIntent: String = "",
    val sessionTitle: String = "",
)

fun defaultHomeModes(): List<HomeModePresentation> = listOf(
    HomeModePresentation(
        label = "Before workout",
        action = "Prime before training",
        body = "Use Brace & Breathe to organize your body before hard effort.",
        sessionIntent = "pre_workout",
        sessionTitle = "Brace & Breathe",
    ),
    HomeModePresentation(
        label = "After workout",
        action = "Reset after training",
        body = "Use Reset After the Session to bring your system down.",
        sessionIntent = "post_workout",
        sessionTitle = "Reset After the Session",
    ),
)
