package com.hybridtempo.android.readiness

import com.hybridtempo.android.data.BreathworkSession
import com.hybridtempo.android.data.DailyCheckIn
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class ReadinessCalculatorTest {
    @Test
    fun `scores latest check-in using energy stress soreness and session consistency`() {
        val today = LocalDate.of(2026, 5, 25)
        val checkIn = DailyCheckIn(
            energy = 8,
            stress = 3,
            soreness = 4,
            createdAt = "${today}T10:00:00Z",
        )
        val sessions = listOf(
            session("Post-training recovery", today),
            session("Downregulation", today.minusDays(1)),
            session("Recovery reset", today.minusDays(2)),
        )

        val readiness = ReadinessCalculator.calculate(
            latestCheckIn = checkIn,
            recentSessions = sessions,
            today = today,
        )

        assertEquals(79, readiness.score)
        assertEquals("Ready", readiness.label)
        assertTrue(readiness.summary.contains("Energy is supporting readiness"))
        assertTrue(readiness.nudge.contains("Keep the rhythm"))
    }

    @Test
    fun `labels readiness as manual plus health connect when health data is available`() {
        val today = LocalDate.of(2026, 5, 25)
        val readiness = ReadinessCalculator.calculate(
            latestCheckIn = DailyCheckIn(
                energy = 6,
                stress = 5,
                soreness = 5,
                createdAt = "${today}T10:00:00Z",
            ),
            recentSessions = emptyList(),
            healthMetrics = HealthMetricsSnapshot(
                sleepMinutesLastNight = 470,
                workoutsLast7Days = 4,
                restingHeartRateBpm = 52,
            ),
            today = today,
        )

        assertEquals("Manual + Health Connect", readiness.sourceLabel)
        assertTrue(readiness.summary.contains("sleep data supports recovery"))
    }

    @Test
    fun `returns setup state when no check-in exists`() {
        val readiness = ReadinessCalculator.calculate(
            latestCheckIn = null,
            recentSessions = emptyList(),
            today = LocalDate.of(2026, 5, 25),
        )

        assertEquals(50, readiness.score)
        assertEquals("Needs check-in", readiness.label)
        assertTrue(readiness.nudge.contains("Complete a check-in"))
    }

    @Test
    fun `readiness nudge does not include race countdown`() {
        val readiness = ReadinessCalculator.calculate(
            latestCheckIn = DailyCheckIn(energy = 6, stress = 5, soreness = 5),
            recentSessions = emptyList(),
            today = LocalDate.of(2026, 5, 25),
            raceName = "HYROX Toronto",
            raceDate = "2026-05-20",
        )

        assertTrue(!readiness.nudge.contains("HYROX Toronto"))
    }

    @Test
    fun `builds future race countdown label`() {
        val countdown = RaceCountdownCalculator.calculate(
            raceName = "HYROX Toronto",
            raceDate = "2026-06-20",
            today = LocalDate.of(2026, 5, 25),
        )

        assertEquals("HYROX Toronto", countdown?.title)
        assertEquals("26 days out", countdown?.label)
    }

    @Test
    fun `builds today and past race countdown labels`() {
        val todayCountdown = RaceCountdownCalculator.calculate(
            raceName = "",
            raceDate = "2026-05-25",
            today = LocalDate.of(2026, 5, 25),
        )
        val pastCountdown = RaceCountdownCalculator.calculate(
            raceName = "HYROX Toronto",
            raceDate = "2026-05-20",
            today = LocalDate.of(2026, 5, 25),
        )

        assertEquals("Race day", todayCountdown?.title)
        assertEquals("Race day is today", todayCountdown?.label)
        assertEquals("HYROX Toronto", pastCountdown?.title)
        assertEquals("Race date has passed", pastCountdown?.label)
    }

    @Test
    fun `returns null without a valid race date`() {
        val countdown = RaceCountdownCalculator.calculate(
            raceName = "HYROX Toronto",
            raceDate = "",
            today = LocalDate.of(2026, 5, 25),
        )

        assertEquals(null, countdown)
    }

    private fun session(protocol: String, date: LocalDate): BreathworkSession = BreathworkSession(
        protocol = protocol,
        durationMinutes = 5,
        cadence = "4 second inhale · 5 second exhale",
        completed = true,
        completedAt = "${date}T12:00:00Z",
    )
}
