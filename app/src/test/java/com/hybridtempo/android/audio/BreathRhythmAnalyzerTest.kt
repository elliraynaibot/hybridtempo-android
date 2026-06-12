package com.hybridtempo.android.audio

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class BreathRhythmAnalyzerTest {
    @Test
    fun `consistent breath envelope returns high rhythm match`() {
        val result = BreathRhythmAnalyzer.analyze(
            amplitudeSamples = repeatingBreathEnvelope(cycles = 5),
            durationSeconds = 25,
        )

        assertTrue(result.rhythmMatchedPercent >= 80)
        assertTrue(result.detectedBreaths >= 4)
    }

    @Test
    fun `irregular breath envelope returns lower rhythm match`() {
        val result = BreathRhythmAnalyzer.analyze(
            amplitudeSamples = listOf(
                0.02f, 0.04f, 0.92f, 0.10f, 0.04f,
                0.06f, 0.76f, 0.18f,
                0.04f, 0.05f, 0.12f, 0.88f, 0.19f,
                0.05f, 0.91f, 0.12f,
                0.03f, 0.04f, 0.05f, 0.08f, 0.95f,
            ),
            durationSeconds = 25,
        )

        assertTrue(result.rhythmMatchedPercent < 75)
    }

    @Test
    fun `impact summary compares before and after breath checks`() {
        val summary = BreathRhythmAnalyzer.compare(
            before = BreathRhythmCheckResult(
                rhythmMatchedPercent = 72,
                detectedBreaths = 4,
                durationSeconds = 20,
            ),
            after = BreathRhythmCheckResult(
                rhythmMatchedPercent = 84,
                detectedBreaths = 5,
                durationSeconds = 20,
            ),
        )

        assertEquals("Breath control improved +12%.", summary)
    }

    private fun repeatingBreathEnvelope(cycles: Int): List<Float> =
        List(cycles) {
            listOf(0.02f, 0.05f, 0.22f, 0.78f, 0.24f, 0.06f)
        }.flatten()
}
