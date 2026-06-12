package com.hybridtempo.android.ui

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class GuideModePresentationTest {
    @Test
    fun `default guide modes match the breath windows product flow`() {
        val modes = defaultGuideModes()

        assertEquals(
            listOf("Learn", "Pre-set", "Between sets", "Post-workout"),
            modes.map { it.label },
        )
        assertEquals(
            listOf("learn", "pre_set", "between_sets", "post_workout"),
            modes.map { it.value },
        )
    }

    @Test
    fun `selected guide mode provides session and cue copy`() {
        val mode = defaultGuideModes().first { it.value == "between_sets" }
        val presentation = mode.toActiveGuidePresentation()

        assertEquals("Recovery", presentation.eyebrow)
        assertTrue(presentation.title.contains("between", ignoreCase = true))
        assertTrue(presentation.primaryCue.contains("Exhale", ignoreCase = true))
        assertTrue(presentation.duringSetCue.isNotBlank())
    }
}
