package com.hybridtempo.android.ui

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class BreathWindowsPresentationTest {
    @Test
    fun `default windows teach the four breath moments in order`() {
        val windows = defaultBreathWindows()

        assertEquals(4, windows.size)
        assertEquals(
            listOf("Before the set", "During the set", "Between reps", "After the set"),
            windows.map { it.title },
        )
        assertEquals(listOf(1, 2, 3, 4), windows.map { it.number })
    }

    @Test
    fun `presentation frames breath windows as bridge to protocol`() {
        val presentation = BreathWindowsPresentation()

        assertTrue(presentation.subtitle.contains("small moments"))
        assertEquals("VIEW PROTOCOL", presentation.primaryAction)
        assertEquals("ADJUST CHECK-IN", presentation.secondaryAction)
    }
}
