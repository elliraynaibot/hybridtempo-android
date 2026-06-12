package com.hybridtempo.android.ui

data class BreathRhythmCheckPresentation(
    val privacy: String = "HybridTempo listens only during this check. Raw audio is not stored. The app saves only rhythm consistency metadata.",
    val limitations: String = "This is not a medical respiratory-rate measurement. Results are best in a quiet space with the phone near your face and no audio playing from the speaker. The score estimates rhythm consistency, not inhale/exhale accuracy.",
) {
    fun recordingState(remainingSeconds: Int): BreathRhythmRecordingPresentation {
        return BreathRhythmRecordingPresentation(
            title = "Recording breath",
            countdownLabel = remainingSeconds.coerceAtLeast(0).toString(),
            instruction = "Keep the phone steady and breathe normally.",
            privacyReminder = "Raw audio is not stored.",
        )
    }
}

data class BreathRhythmRecordingPresentation(
    val title: String,
    val countdownLabel: String,
    val instruction: String,
    val privacyReminder: String,
)
