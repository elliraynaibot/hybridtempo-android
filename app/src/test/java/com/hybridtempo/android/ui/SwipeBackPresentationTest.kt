package com.hybridtempo.android.ui

import kotlin.test.assertEquals
import org.junit.Test

class SwipeBackPresentationTest {
    @Test
    fun `swipe back indicator progress ignores left drags and caps at threshold`() {
        assertEquals(0f, swipeBackIndicatorProgress(-40f))
        assertEquals(0f, swipeBackIndicatorProgress(0f))
        assertEquals(0.5f, swipeBackIndicatorProgress(45f))
        assertEquals(1f, swipeBackIndicatorProgress(90f))
        assertEquals(1f, swipeBackIndicatorProgress(140f))
    }
}
