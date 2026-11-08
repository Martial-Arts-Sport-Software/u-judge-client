package org.mass.session

import org.mass.State
import org.mass.enums.Disciplines
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionStateStoreTest {
    @Test
    fun resyncReplacesTheWholeStateEvenWithALowerCursor() {
        val store = SessionFixtures.synchronizedStore(cursor = 118)

        store.applyResync(57, null)

        assertEquals(57, store.cursor)
        assertNull(store.snapshot)
        assertTrue(store.isSynchronized)
    }

    @Test
    fun pushesBeforeASnapshotOnThisSocketAreIgnored() {
        val store = SessionStateStore()

        assertEquals(
            PushOutcome.IGNORED,
            store.applyPush(SessionPush.Snapshot(200, SessionFixtures.snapshot()))
        )
        assertNull(store.snapshot)
    }

    @Test
    fun pushWithACursorNotGreaterThanTheHeldOneIsDropped() {
        val store = SessionFixtures.synchronizedStore(cursor = 118)

        listOf(117L, 118L).forEach { cursor ->
            assertEquals(
                PushOutcome.STALE,
                store.applyPush(SessionPush.StateUpdated(SessionFixtures.SESSION_ID, cursor, SessionPhase.PAUSED, null))
            )
            assertEquals(
                PushOutcome.STALE,
                store.applyPush(SessionPush.ScoreUpdated(SessionFixtures.SESSION_ID, cursor, score(9, 9)))
            )
            assertEquals(PushOutcome.STALE, store.applyPush(SessionPush.Snapshot(cursor, null)))
        }

        assertEquals(SessionFixtures.snapshot(), store.snapshot)
        assertEquals(118, store.cursor)
    }

    @Test
    fun newerUpdatesChangeStateResultAndScoreAndAdvanceTheCursor() {
        val store = SessionFixtures.synchronizedStore(cursor = 118)
        val result = BoutResult(Competitor.BLUE, "final_score")

        assertEquals(
            PushOutcome.APPLIED,
            store.applyPush(SessionPush.ScoreUpdated(SessionFixtures.SESSION_ID, 124, score(5, 1)))
        )
        assertEquals(
            PushOutcome.APPLIED,
            store.applyPush(SessionPush.StateUpdated(SessionFixtures.SESSION_ID, 125, SessionPhase.COMPLETED, result))
        )

        assertEquals(score(5, 1), store.snapshot?.score)
        assertEquals(SessionPhase.COMPLETED, store.snapshot?.state)
        assertEquals(result, store.snapshot?.result)
        assertEquals(125, store.cursor)
    }

    @Test
    fun updateForAnotherSessionIsAMismatchAndChangesNothing() {
        val store = SessionFixtures.synchronizedStore(cursor = 118)

        assertEquals(
            PushOutcome.SESSION_MISMATCH,
            store.applyPush(SessionPush.StateUpdated("other-session", 130, SessionPhase.PAUSED, null))
        )
        assertEquals(
            PushOutcome.SESSION_MISMATCH,
            SessionFixtures.synchronizedStore(null).applyPush(
                SessionPush.ScoreUpdated(SessionFixtures.SESSION_ID, 130, score(1, 0))
            )
        )
        assertEquals(SessionFixtures.snapshot(), store.snapshot)
        assertEquals(118, store.cursor)
    }

    @Test
    fun newSessionSnapshotReplacesTheBoutAndTheSeat() {
        val store = SessionFixtures.synchronizedStore(cursor = 118)
        val next = SessionFixtures.snapshot(sessionId = "next-session", boutNumber = 8, seat = null)

        assertEquals(PushOutcome.APPLIED, store.applyPush(SessionPush.Snapshot(140, next)))

        assertEquals(next, store.snapshot)
        assertNull(store.snapshot?.auditContext())
    }

    @Test
    fun detachKeepsTheBoutVisibleButNotSynchronizedUntilTheNextResync() {
        val store = SessionFixtures.synchronizedStore(cursor = 118)

        store.detach()

        assertFalse(store.isSynchronized)
        assertEquals(SessionFixtures.snapshot(), store.snapshot)
        assertEquals(118, store.cursor)
        assertEquals(PushOutcome.IGNORED, store.applyPush(SessionPush.Snapshot(200, null)))
    }

    @Test
    fun activeSessionChangesOnlyThroughSessionStore() {
        State.session.clear()
        try {
            State.session.applyResync(118, SessionFixtures.snapshot())

            State.selectDiscipline(Disciplines.HOSINSOOL)

            assertEquals(SessionFixtures.snapshot(), State.session.snapshot)
        } finally {
            State.session.clear()
            State.currentDiscipline = null
        }
    }

    @Test
    fun clearForgetsSessionAndCursor() {
        val store = SessionFixtures.synchronizedStore()

        store.clear()

        assertNull(store.snapshot)
        assertNull(store.cursor)
        assertFalse(store.isSynchronized)
    }

    private fun score(blue: Int, red: Int) = KerugiScore(blue, red, 0, 0, emptyList(), null)
}
