package com.hybridtempo.android.ui

data class GuidedBreathworkSession(
    val id: String,
    val title: String,
    val intention: String,
    val category: String,
    val audioOnlyTrackName: String,
    val guidedAudioTrackName: String = audioOnlyTrackName,
    val durationSeconds: Int,
    val visualizationStyle: BreathworkVisualizationStyle,
    val segments: List<GuidedBreathworkSegment>,
) {
    val audioTrackName: String
        get() = audioOnlyTrackName
}

enum class BreathworkVisualizationStyle {
    Gather,
    Brace,
    Reset,
}

data class GuidedBreathworkSegment(
    val startSeconds: Int,
    val endSeconds: Int,
    val title: String,
    val cue: String,
    val visualMode: String,
    val inhaleSeconds: Int? = null,
    val exhaleSeconds: Int? = null,
)

fun GuidedBreathworkSession.segmentAt(elapsedSeconds: Int): GuidedBreathworkSegment {
    val safeElapsed = elapsedSeconds.coerceAtLeast(0)
    return segments.firstOrNull { segment ->
        safeElapsed >= segment.startSeconds && safeElapsed < segment.endSeconds
    } ?: segments.last()
}

fun GuidedBreathworkSession.audioTrackNameFor(guidedNarrationEnabled: Boolean): String {
    return if (guidedNarrationEnabled) guidedAudioTrackName else audioOnlyTrackName
}

fun arriveOrganizeGuidedSession(): GuidedBreathworkSession = GuidedBreathworkSession(
    id = "arrive_organize",
    title = "Arrive & Organize",
    intention = "Shift from distracted or stressed into physically ready.",
    category = "pre_session",
    audioOnlyTrackName = "arrive_organize_audio_only",
    guidedAudioTrackName = "arrive_organize_mixed",
    durationSeconds = 198,
    visualizationStyle = BreathworkVisualizationStyle.Gather,
    segments = listOf(
        GuidedBreathworkSegment(
            startSeconds = 0,
            endSeconds = 28,
            title = "Arrive",
            cue = "Drop the shoulders. Soften the jaw.",
            visualMode = "settle_wave",
        ),
        GuidedBreathworkSegment(
            startSeconds = 28,
            endSeconds = 43,
            title = "Organize",
            cue = "Hands on ribs and lower abdomen.",
            visualMode = "rib_expansion",
        ),
        GuidedBreathworkSegment(
            startSeconds = 43,
            endSeconds = 76,
            title = "Find the rhythm",
            cue = "Inhale 4. Exhale 6.",
            visualMode = "breath_wave",
            inhaleSeconds = 4,
            exhaleSeconds = 6,
        ),
        GuidedBreathworkSegment(
            startSeconds = 76,
            endSeconds = 113,
            title = "Low and wide",
            cue = "Keep the chest quiet. Breathe into the ribs.",
            visualMode = "wide_rib_wave",
            inhaleSeconds = 4,
            exhaleSeconds = 6,
        ),
        GuidedBreathworkSegment(
            startSeconds = 113,
            endSeconds = 158,
            title = "Release tension",
            cue = "Shoulders relax. Ribs come down without collapse.",
            visualMode = "long_exhale_wave",
            inhaleSeconds = 4,
            exhaleSeconds = 6,
        ),
        GuidedBreathworkSegment(
            startSeconds = 158,
            endSeconds = 180,
            title = "Organized before intense",
            cue = "Get organized before you get intense.",
            visualMode = "steady_ready_wave",
        ),
        GuidedBreathworkSegment(
            startSeconds = 180,
            endSeconds = 198,
            title = "Carry it forward",
            cue = "Calm body. Clear rhythm. Controlled effort.",
            visualMode = "steady_ready_wave",
        ),
    ),
)

fun braceBreatheGuidedSession(): GuidedBreathworkSession = GuidedBreathworkSession(
    id = "brace_breathe",
    title = "Brace & Breathe",
    intention = "Stay mechanically organized before hard work starts.",
    category = "pre_workout",
    audioOnlyTrackName = "breathe_brace_audio_only",
    guidedAudioTrackName = "breathe_brace",
    durationSeconds = 279,
    visualizationStyle = BreathworkVisualizationStyle.Brace,
    segments = listOf(
        GuidedBreathworkSegment(
            startSeconds = 0,
            endSeconds = 55,
            title = "Set posture",
            cue = "Stand tall and organize your brace before intensity.",
            visualMode = "settle_wave",
        ),
        GuidedBreathworkSegment(
            startSeconds = 55,
            endSeconds = 130,
            title = "Brace with breath",
            cue = "Build pressure without losing control.",
            visualMode = "bracing_wave",
        ),
        GuidedBreathworkSegment(
            startSeconds = 130,
            endSeconds = 215,
            title = "Breathe again",
            cue = "Reclaim rhythm between efforts.",
            visualMode = "breath_wave",
        ),
        GuidedBreathworkSegment(
            startSeconds = 215,
            endSeconds = 279,
            title = "Carry the cue",
            cue = "Stay organized when the session gets heavy.",
            visualMode = "steady_ready_wave",
        ),
    ),
)

fun resetAfterSessionGuidedSession(): GuidedBreathworkSession = GuidedBreathworkSession(
    id = "reset_after_session",
    title = "Reset After the Session",
    intention = "Downshift after training and reinforce recovery control.",
    category = "post_workout",
    audioOnlyTrackName = "reset_after_session_after_workout",
    guidedAudioTrackName = "reset_after_session_with_narration",
    durationSeconds = 200,
    visualizationStyle = BreathworkVisualizationStyle.Reset,
    segments = listOf(
        GuidedBreathworkSegment(
            startSeconds = 0,
            endSeconds = 40,
            title = "Arrive after effort",
            cue = "Let the work be done before you leave the session.",
            visualMode = "settle_wave",
        ),
        GuidedBreathworkSegment(
            startSeconds = 40,
            endSeconds = 105,
            title = "Lengthen the exhale",
            cue = "Use the exhale to bring the system down.",
            visualMode = "long_exhale_wave",
        ),
        GuidedBreathworkSegment(
            startSeconds = 105,
            endSeconds = 165,
            title = "Recover control",
            cue = "Let breathing settle before you move on.",
            visualMode = "recovery_wave",
        ),
        GuidedBreathworkSegment(
            startSeconds = 165,
            endSeconds = 200,
            title = "Close the session",
            cue = "Leave with a calmer rhythm than you finished with.",
            visualMode = "steady_ready_wave",
        ),
    ),
)

fun selectGuidedSessionForIntent(sessionIntent: String): GuidedBreathworkSession {
    return when (sessionIntent) {
        "pre_workout" -> braceBreatheGuidedSession()
        "post_workout" -> resetAfterSessionGuidedSession()
        else -> arriveOrganizeGuidedSession()
    }
}
