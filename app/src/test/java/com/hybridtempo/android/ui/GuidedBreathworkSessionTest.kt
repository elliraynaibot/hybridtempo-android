package com.hybridtempo.android.ui

import kotlin.test.assertEquals
import org.junit.Test

class GuidedBreathworkSessionTest {
    @Test
    fun `arrive organize session matches the mixed audio metadata`() {
        val session = arriveOrganizeGuidedSession()

        assertEquals("arrive_organize", session.id)
        assertEquals("Arrive & Organize", session.title)
        assertEquals("arrive_organize_mixed", session.audioTrackName)
        assertEquals(198, session.durationSeconds)
        assertEquals(7, session.segments.size)
    }

    @Test
    fun `arrive organize timeline resolves current segment by elapsed seconds`() {
        val session = arriveOrganizeGuidedSession()

        assertEquals("Arrive", session.segmentAt(elapsedSeconds = 0).title)
        assertEquals("Organize", session.segmentAt(elapsedSeconds = 28).title)
        assertEquals("Find the rhythm", session.segmentAt(elapsedSeconds = 43).title)
        assertEquals("Low and wide", session.segmentAt(elapsedSeconds = 76).title)
        assertEquals("Release tension", session.segmentAt(elapsedSeconds = 113).title)
        assertEquals("Organized before intense", session.segmentAt(elapsedSeconds = 158).title)
        assertEquals("Carry it forward", session.segmentAt(elapsedSeconds = 180).title)
        assertEquals("Carry it forward", session.segmentAt(elapsedSeconds = 999).title)
    }

    @Test
    fun `brace breathe session matches the before workout audio metadata`() {
        val session = braceBreatheGuidedSession()

        assertEquals("brace_breathe", session.id)
        assertEquals("Brace & Breathe", session.title)
        assertEquals("breathe_brace", session.audioTrackName)
        assertEquals(279, session.durationSeconds)
        assertEquals(4, session.segments.size)
    }

    @Test
    fun `reset after session matches the after workout audio metadata`() {
        val session = resetAfterSessionGuidedSession()

        assertEquals("reset_after_session", session.id)
        assertEquals("Reset After the Session", session.title)
        assertEquals("reset_after_session_after_workout", session.audioTrackName)
        assertEquals(200, session.durationSeconds)
        assertEquals(4, session.segments.size)
    }

    @Test
    fun `pre workout intent selects brace breathe guided session`() {
        val session = selectGuidedSessionForIntent(sessionIntent = "pre_workout")

        assertEquals("brace_breathe", session.id)
    }

    @Test
    fun `post workout intent selects reset after session guided session`() {
        val session = selectGuidedSessionForIntent(sessionIntent = "post_workout")

        assertEquals("reset_after_session", session.id)
    }

    @Test
    fun `guided sessions declare distinct visualization styles`() {
        assertEquals(BreathworkVisualizationStyle.Gather, arriveOrganizeGuidedSession().visualizationStyle)
        assertEquals(BreathworkVisualizationStyle.Brace, braceBreatheGuidedSession().visualizationStyle)
        assertEquals(BreathworkVisualizationStyle.Reset, resetAfterSessionGuidedSession().visualizationStyle)
    }
}
