package org.mass.session

import org.mass.State
import org.mass.enums.Disciplines
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionStateStoreTest {
    @Test
    fun serverSnapshotsRepresentEachSessionPhase() {
        val store = SessionStateStore()
        val snapshot = snapshot()

        store.update(snapshot, SessionPhase.PREPARED)
        assertEquals(SessionState.Prepared(snapshot), store.state)

        store.update(snapshot, SessionPhase.RUNNING)
        assertEquals(SessionState.Running(snapshot), store.state)

        store.update(snapshot, SessionPhase.PAUSED)
        assertEquals(SessionState.Paused(snapshot), store.state)

        store.update(snapshot, SessionPhase.COMPLETED)
        assertEquals(SessionState.Completed(snapshot), store.state)
    }

    @Test
    fun activeSessionChangesOnlyThroughSessionStore() {
        val snapshot = snapshot()
        State.session.clear()
        try {
            State.session.update(snapshot, SessionPhase.RUNNING)

            State.selectDiscipline(Disciplines.HOSINSOOL)

            assertEquals(SessionState.Running(snapshot), State.session.state)
        } finally {
            State.session.clear()
            State.currentDiscipline = null
        }
    }

    @Test
    fun clearExplicitlyRemovesTheActiveSession() {
        val store = SessionStateStore()
        store.update(snapshot(), SessionPhase.RUNNING)

        store.clear()

        assertEquals(SessionState.NoActiveSession, store.state)
    }

    private fun snapshot() = SessionSnapshot(
        sessionId = "session-1",
        discipline = Disciplines.KERUGI,
        boutLabel = "Bout 3",
        blueParticipantLabel = "Blue athlete",
        redParticipantLabel = "Red athlete"
    )
}
