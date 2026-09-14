package org.mass.combat

import org.mass.connection.ConnectionEvent
import org.mass.connection.ConnectionStateStore
import org.mass.enums.Disciplines
import org.mass.session.SessionPhase
import org.mass.session.SessionSnapshot
import org.mass.session.SessionStateStore
import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class KerugiCommandControllerTest {
    @Test
    fun runningKerugiSessionPersistsTypedCommandsWithAdjustedTimestampAndSequence() {
        val storage = FakeStorage()
        val connection = connectedStore(clockOffsetMillis = 250)
        val session = runningKerugiSession()
        val eventIds = ArrayDeque(listOf("event-1", "event-2"))
        val controller = KerugiCommandController(
            connection = connection,
            session = session,
            outbox = DurableEventOutbox(storage),
            nowMillis = { 1_000 },
            eventId = { eventIds.removeFirst() }
        )

        val first = assertIs<KerugiCommandOutcome.Pending>(
            controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        ).event
        val second = assertIs<KerugiCommandOutcome.Pending>(
            controller.submit(KerugiParticipant.RED, KerugiTarget.BODY)
        ).event

        assertEquals("event-1", first.eventId)
        assertEquals(0, first.clientSequence)
        assertEquals("1970-01-01T00:00:01.250Z", first.clientTimestamp)
        assertEquals("session-1", first.sessionId)
        assertEquals("{\"type\":\"kerugi_score\",\"participant\":\"BLUE\",\"target\":\"HEAD\"}", first.payload)
        assertEquals(1, second.clientSequence)
        assertEquals("{\"type\":\"kerugi_score\",\"participant\":\"RED\",\"target\":\"BODY\"}", second.payload)
        assertEquals(listOf(first, second), DurableEventOutbox(storage).pendingEvents())
    }

    @Test
    fun unavailableOutsideRunningKerugiSessionWithoutPersistingAnEvent() {
        val storage = FakeStorage()
        val controller = KerugiCommandController(
            connection = connectedStore(clockOffsetMillis = 0),
            session = SessionStateStore(),
            outbox = DurableEventOutbox(storage),
            nowMillis = { 1_000 },
            eventId = { "event-1" }
        )

        assertFalse(controller.isAvailable)
        assertEquals(
            KerugiCommandOutcome.Unavailable,
            controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        )
        assertEquals(emptyList(), DurableEventOutbox(storage).pendingEvents())
    }

    @Test
    fun sequenceContinuesAfterTheOutboxIsRecreated() {
        val storage = FakeStorage()
        val connection = connectedStore(clockOffsetMillis = 0)
        val session = runningKerugiSession()
        val first = KerugiCommandController(
            connection, session, DurableEventOutbox(storage), { 1_000 }, { "event-1" }
        ).submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        val second = KerugiCommandController(
            connection, session, DurableEventOutbox(storage), { 2_000 }, { "event-2" }
        ).submit(KerugiParticipant.RED, KerugiTarget.HEAD)

        assertEquals(0, assertIs<KerugiCommandOutcome.Pending>(first).event.clientSequence)
        assertEquals(1, assertIs<KerugiCommandOutcome.Pending>(second).event.clientSequence)
    }

    @Test
    fun sequenceDoesNotResetAfterAcknowledgementAndOutboxRecreation() {
        val storage = FakeStorage()
        val connection = connectedStore(clockOffsetMillis = 0)
        val session = runningKerugiSession()
        val outbox = DurableEventOutbox(storage)
        val first = KerugiCommandController(
            connection, session, outbox, { 1_000 }, { "event-1" }
        ).submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        outbox.acknowledge(assertIs<KerugiCommandOutcome.Pending>(first).event.eventId)

        val second = KerugiCommandController(
            connection, session, DurableEventOutbox(storage), { 2_000 }, { "event-2" }
        ).submit(KerugiParticipant.RED, KerugiTarget.HEAD)

        assertEquals(1, assertIs<KerugiCommandOutcome.Pending>(second).event.clientSequence)
    }

    private fun connectedStore(clockOffsetMillis: Long) = ConnectionStateStore().apply {
        dispatch(ConnectionEvent.StartDiscovery)
        dispatch(ConnectionEvent.SelectServer("server-1"))
        dispatch(ConnectionEvent.ValidateMetadata(metadata()))
        dispatch(ConnectionEvent.RequestPairing)
        dispatch(ConnectionEvent.AcceptPairing("device-1", clockOffsetMillis))
    }

    private fun runningKerugiSession() = SessionStateStore().apply {
        update(
            SessionSnapshot(
                sessionId = "session-1",
                discipline = Disciplines.KERUGI,
                boutLabel = "1",
                blueParticipantLabel = "Blue",
                redParticipantLabel = "Red"
            ),
            SessionPhase.RUNNING
        )
    }

    private fun metadata() = org.mass.connection.ServerMetadata(
        protocolMajor = 1,
        protocolMinor = 0,
        capabilities = emptySet(),
        peerId = "peer-1",
        courtId = "court-1",
        serverName = "Court 1",
        pairingPolicy = "approval",
        serverTimeMillis = 0
    )

    private class FakeStorage : EventOutboxStorage {
        private var value: String? = null

        override fun load(): String? = value

        override fun save(value: String) {
            this.value = value
        }
    }
}
