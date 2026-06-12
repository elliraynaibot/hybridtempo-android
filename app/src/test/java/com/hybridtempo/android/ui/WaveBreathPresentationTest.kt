package com.hybridtempo.android.ui

import com.hybridtempo.android.data.BreathPhase
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class WaveBreathPresentationTest {
    @Test
    fun `inhale phase cues the athlete to ride the wave up`() {
        val phase = BreathPhase(
            label = "Inhale",
            seconds = 4,
            instruction = "Draw air in through the nose",
            scaleTarget = 1.12f,
        )

        val presentation = phase.toWaveBreathPresentation(progress = 0.25f)

        assertEquals("Ride the wave", presentation.title)
        assertEquals("Wave rising", presentation.phaseCue)
        assertEquals("Draw air in through the nose", presentation.instruction)
        assertEquals(0.25f, presentation.progress)
        assertTrue(presentation.emphasizesControl)
    }

    @Test
    fun `exhale phase cues a longer downward ride`() {
        val phase = BreathPhase(
            label = "Exhale",
            seconds = 6,
            instruction = "Let the exhale do the work",
            scaleTarget = 0.72f,
        )

        val presentation = phase.toWaveBreathPresentation(progress = 1.4f)

        assertEquals("Ride the exhale down", presentation.phaseCue)
        assertEquals("Let the exhale do the work", presentation.instruction)
        assertEquals(1f, presentation.progress)
        assertTrue(presentation.emphasizesControl)
    }

    @Test
    fun `rest phase keeps the cue calm and clamps low progress`() {
        val phase = BreathPhase(
            label = "Rest",
            seconds = 1,
            instruction = "Soften the jaw",
            scaleTarget = 0.68f,
        )

        val presentation = phase.toWaveBreathPresentation(progress = -0.2f)

        assertEquals("Hold the rhythm", presentation.phaseCue)
        assertEquals(0f, presentation.progress)
        assertTrue(presentation.emphasizesControl)
    }

    @Test
    fun `phase animation duration uses remaining smooth progress instead of one second steps`() {
        assertEquals(
            6000,
            wavePhaseAnimationDurationMillis(phaseSeconds = 6, currentProgress = 0f),
        )
        assertEquals(
            3000,
            wavePhaseAnimationDurationMillis(phaseSeconds = 6, currentProgress = 0.5f),
        )
        assertEquals(
            0,
            wavePhaseAnimationDurationMillis(phaseSeconds = 6, currentProgress = 1f),
        )
    }

    @Test
    fun `wave focus stays anchored to avoid phase reset jumps`() {
        assertEquals(0.5f, anchoredWaveFocusProgress())
    }

    @Test
    fun `canvas motion identity stays stable while phase amplitude changes`() {
        val inhale = "Inhale".toContinuousWaveCanvasPresentation()
        val exhale = "Exhale".toContinuousWaveCanvasPresentation()

        assertEquals(inhale.animationKey, exhale.animationKey)
        assertEquals(0.26f, inhale.amplitudeScale)
        assertEquals(0.18f, exhale.amplitudeScale)
    }

    @Test
    fun `ambient breath field is stable and not phase driven`() {
        val presentation = breathFieldCanvasPresentation()

        assertEquals("breath_field", presentation.animationKey)
        assertEquals(3, presentation.waveCount)
        assertEquals(0.5f, presentation.focusProgress)
    }

    @Test
    fun `guided visualization styles map to distinct visual motion concepts`() {
        val gather = BreathworkVisualizationStyle.Gather.toGuidedVisualizationPresentation()
        val brace = BreathworkVisualizationStyle.Brace.toGuidedVisualizationPresentation()
        val reset = BreathworkVisualizationStyle.Reset.toGuidedVisualizationPresentation()

        assertEquals("gather_particles", gather.animationKey)
        assertEquals("stacked_rings_expand_evenly", brace.primaryMotion)
        assertEquals("reset_waves", reset.animationKey)
    }
}
