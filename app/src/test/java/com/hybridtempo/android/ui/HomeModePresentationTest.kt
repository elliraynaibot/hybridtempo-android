package com.hybridtempo.android.ui

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class HomeModePresentationTest {
    @Test
    fun `home presentation frames one guided primary action`() {
        val presentation = HomePresentation()

        assertEquals("Choose your breathwork", presentation.primaryAction)
        assertTrue(presentation.flowSummary.contains("before workout", ignoreCase = true))
        assertTrue(presentation.flowSummary.contains("after workout", ignoreCase = true))
    }

    @Test
    fun `home hero explains the app purpose in workout terms`() {
        val presentation = HomePresentation()

        assertEquals("Breathwork for where you are in the workout", presentation.title)
        assertTrue(presentation.subtitle.contains("before", ignoreCase = true))
        assertTrue(presentation.subtitle.contains("after", ignoreCase = true))
    }

    @Test
    fun `home presentation exposes the two primary library routes`() {
        val presentation = HomePresentation()

        assertEquals(2, presentation.secondaryActions.size)
        assertEquals("Before workout", presentation.secondaryActions[0].label)
        assertEquals("pre_workout", presentation.secondaryActions[0].sessionIntent)
        assertEquals("Brace & Breathe", presentation.secondaryActions[0].sessionTitle)
        assertEquals("After workout", presentation.secondaryActions[1].label)
        assertEquals("post_workout", presentation.secondaryActions[1].sessionIntent)
        assertEquals("Reset After the Session", presentation.secondaryActions[1].sessionTitle)
    }
}
