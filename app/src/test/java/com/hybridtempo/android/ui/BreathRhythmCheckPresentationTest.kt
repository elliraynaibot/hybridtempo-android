package com.hybridtempo.android.ui

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class BreathRhythmCheckPresentationTest {
    @Test
    fun `limitations explain environment accuracy and non medical use`() {
        val presentation = BreathRhythmCheckPresentation()

        assertTrue(presentation.limitations.contains("quiet", ignoreCase = true))
        assertTrue(presentation.limitations.contains("phone near your face", ignoreCase = true))
        assertTrue(presentation.limitations.contains("not a medical", ignoreCase = true))
        assertTrue(presentation.limitations.contains("rhythm consistency", ignoreCase = true))
    }

    @Test
    fun `privacy copy states raw audio is not stored`() {
        val presentation = BreathRhythmCheckPresentation()

        assertTrue(presentation.privacy.contains("Raw audio is not stored", ignoreCase = true))
        assertTrue(presentation.privacy.contains("metadata", ignoreCase = true))
    }

    @Test
    fun `recording state gives a countdown and clear breathing instruction`() {
        val presentation = BreathRhythmCheckPresentation()

        val recording = presentation.recordingState(remainingSeconds = 12)

        assertEquals("12", recording.countdownLabel)
        assertTrue(recording.title.contains("Recording", ignoreCase = true))
        assertTrue(recording.instruction.contains("breathe normally", ignoreCase = true))
        assertTrue(recording.privacyReminder.contains("not stored", ignoreCase = true))
    }
}
