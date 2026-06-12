package com.hybridtempo.android.health

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class HeartRateImpactCalculatorTest {
    private val start = Instant.parse("2026-06-12T14:00:00Z")
    private val end = Instant.parse("2026-06-12T14:05:00Z")

    @Test
    fun `calculates before and after heart rate impact around session window`() {
        val impact = HeartRateImpactCalculator.calculate(
            sessionStartedAt = start,
            sessionEndedAt = end,
            samples = listOf(
                HeartRateSample(start.minusSeconds(75), 92),
                HeartRateSample(end.plusSeconds(45), 78),
            ),
        )

        assertEquals(92, impact?.beforeBpm)
        assertEquals(78, impact?.afterBpm)
        assertEquals(-14, impact?.deltaBpm)
        assertEquals("HR 92 -> 78 bpm (-14)", impact?.summary)
    }

    @Test
    fun `uses nearest samples inside before and after windows`() {
        val impact = HeartRateImpactCalculator.calculate(
            sessionStartedAt = start,
            sessionEndedAt = end,
            samples = listOf(
                HeartRateSample(start.minusSeconds(115), 101),
                HeartRateSample(start.minusSeconds(15), 94),
                HeartRateSample(end.plusSeconds(90), 82),
                HeartRateSample(end.plusSeconds(20), 79),
            ),
        )

        assertEquals(94, impact?.beforeBpm)
        assertEquals(79, impact?.afterBpm)
        assertEquals(-15, impact?.deltaBpm)
    }

    @Test
    fun `returns null when both windows do not have samples`() {
        val impact = HeartRateImpactCalculator.calculate(
            sessionStartedAt = start,
            sessionEndedAt = end,
            samples = listOf(
                HeartRateSample(start.minusSeconds(300), 92),
                HeartRateSample(end.plusSeconds(300), 78),
            ),
        )

        assertNull(impact)
    }

    @Test
    fun `calculates impact from locked pre session heart rate`() {
        val impact = HeartRateImpactCalculator.calculateFromLockedStart(
            lockedStart = HeartRateLock(
                sampledAt = start.minusSeconds(40),
                bpm = 91,
            ),
            sessionEndedAt = end,
            samples = listOf(
                HeartRateSample(start.minusSeconds(15), 88),
                HeartRateSample(end.plusSeconds(25), 77),
            ),
        )

        assertEquals(91, impact?.beforeBpm)
        assertEquals(77, impact?.afterBpm)
        assertEquals(-14, impact?.deltaBpm)
    }

    @Test
    fun `locks nearest recent heart rate sample before session`() {
        val lock = HeartRateImpactCalculator.lockRecentStart(
            sessionStartedAt = start,
            samples = listOf(
                HeartRateSample(start.minusSeconds(130), 98),
                HeartRateSample(start.minusSeconds(50), 92),
                HeartRateSample(start.plusSeconds(1), 90),
            ),
        )

        assertEquals(92, lock?.bpm)
        assertEquals(start.minusSeconds(50), lock?.sampledAt)
    }
}
