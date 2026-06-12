package com.hybridtempo.android.data.local

import com.hybridtempo.android.data.AthleteProfile
import com.hybridtempo.android.data.BreathworkSession
import com.hybridtempo.android.data.DailyCheckIn
import kotlin.test.assertEquals
import org.junit.Test

class LocalEntityMapperTest {
    @Test
    fun `profile entity round trips app profile fields`() {
        val profile = AthleteProfile(
            name = "Alex",
            raceName = "HYROX Toronto",
            raceDate = "2026-10-01",
            trainingStyle = "Hybrid",
            weeklyTrainingFrequency = 5,
            goals = listOf("recovery", "race prep"),
            preferredSessionLength = 5,
            eveningReminderEnabled = true,
            healthConnectEnabled = true,
        )

        val restored = profile.toLocalProfileEntity().toAthleteProfile()

        assertEquals(profile, restored)
    }

    @Test
    fun `check in entity round trips training context`() {
        val checkIn = DailyCheckIn(
            date = "2026-06-05",
            energy = 6,
            soreness = 4,
            stress = 5,
            timeAvailable = 5,
            workoutType = "Hybrid",
            workoutIntensity = 8,
            sessionIntent = "post_workout",
            createdAt = "2026-06-05T20:10:00Z",
        )

        val restored = checkIn.toLocalCheckInEntity().toDailyCheckIn()

        assertEquals(checkIn, restored)
    }

    @Test
    fun `session entity round trips reflection data`() {
        val session = BreathworkSession(
            id = "session-1",
            protocol = "Cooldown HR Recovery",
            durationMinutes = 5,
            cadence = "4 second inhale - 6 second exhale",
            completed = true,
            completedAt = "2026-06-05T20:10:00Z",
            breathSkillId = "cooldown-hr-recovery",
            perceivedControl = 7,
            perceivedRecovery = 8,
            reflectionFeeling = "calmer",
            reflectionNotes = "Settled faster than usual.",
        )

        val restored = session.toLocalSessionEntity().toBreathworkSession()

        assertEquals(session, restored)
    }
}
