package org.mass.combat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.mass.connection.ConnectionEvent
import org.mass.connection.ConnectionStateStore
import org.mass.connection.RealtimeCommandClient
import org.mass.connection.RealtimeCommandDispatcher
import org.mass.connection.RealtimeCommandResult
import org.mass.connection.RealtimeSocket
import org.mass.connection.SerializedRealtimeRequestChannel
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

@OptIn(ExperimentalCoroutinesApi::class)
class TanbonCommandControllerTest {
    @Test
    fun runningTanbonSessionPersistsAllTypedCommandsWithAdjustedTimestampAndSequence() {
        val storage = FakeStorage()
        val eventIds = ArrayDeque(listOf("event-1", "event-2", "event-3", "event-4", "event-5"))
        val controller = TanbonCommandController(
            connectedStore(clockOffsetMillis = 250),
            runningTanbonSession(),
            DurableEventOutbox(storage),
            nowMillis = { 1_000 },
            eventId = { eventIds.removeFirst() }
        )

        val events = listOf(
            TanbonAction.BlueHead,
            TanbonAction.RedHead,
            TanbonAction.BlueBody,
            TanbonAction.Cross,
            TanbonAction.RedBody
        ).map { assertIs<TanbonCommandOutcome.Pending>(controller.submit(it)).event }

        assertEquals((0L..4L).toList(), events.map { it.clientSequence })
        assertEquals("1970-01-01T00:00:01.250Z", events.first().clientTimestamp)
        assertEquals("session-1", events.first().sessionId)
        assertEquals(
            listOf(
                "{\"type\":\"tanbon_score\",\"participant\":\"BLUE\",\"target\":\"HEAD\"}",
                "{\"type\":\"tanbon_score\",\"participant\":\"RED\",\"target\":\"HEAD\"}",
                "{\"type\":\"tanbon_score\",\"participant\":\"BLUE\",\"target\":\"BODY\"}",
                "{\"type\":\"tanbon_score\",\"participant\":\"NEUTRAL\",\"target\":\"CROSS\"}",
                "{\"type\":\"tanbon_score\",\"participant\":\"RED\",\"target\":\"BODY\"}"
            ),
            events.map { it.payload }
        )
        assertEquals(events, DurableEventOutbox(storage).pendingEvents())
    }

    @Test
    fun unavailableOutsideRunningTanbonSessionWithoutPersistingAnEvent() {
        val storage = FakeStorage()
        val controller = TanbonCommandController(
            connectedStore(clockOffsetMillis = 0),
            SessionStateStore(),
            DurableEventOutbox(storage),
            nowMillis = { 1_000 },
            eventId = { "event-1" }
        )

        assertFalse(controller.isAvailable)
        assertEquals(TanbonCommandOutcome.Unavailable, controller.submit(TanbonAction.Cross))
        assertEquals(emptyList(), DurableEventOutbox(storage).pendingEvents())
    }

    @Test
    fun matchingAcknowledgementUpdatesFeedbackAndRemovesTheDurableCommand() = runTest {
        val storage = FakeStorage()
        val outbox = DurableEventOutbox(storage)
        val dispatcher = RealtimeCommandDispatcher(this, RealtimeCommandClient(outbox), nowMillis = { 1_000 })
        dispatcher.activate(SerializedRealtimeRequestChannel(ResponseSocket("""{"type":"command_ack","eventId":"event-1"}""")))
        val controller = TanbonCommandController(
            connectedStore(0), runningTanbonSession(), outbox, { 1_000 }, { "event-1" }
        ) { dispatcher }

        controller.submit(TanbonAction.Cross)
        advanceUntilIdle()

        assertEquals(TanbonCommandOutcome.Accepted("event-1"), controller.latestOutcome)
        assertEquals(emptyList(), outbox.pendingEvents())
    }

    @Test
    fun terminalRejectionRemainsDurableAndShowsRejectedFeedback() = runTest {
        val storage = FakeStorage()
        val outbox = DurableEventOutbox(storage)
        val dispatcher = RealtimeCommandDispatcher(this, RealtimeCommandClient(outbox), nowMillis = { 1_000 })
        dispatcher.activate(SerializedRealtimeRequestChannel(ResponseSocket("""{"type":"command_rejected","eventId":"event-1","code":"invalid_session"}""")))
        val controller = TanbonCommandController(
            connectedStore(0), runningTanbonSession(), outbox, { 1_000 }, { "event-1" }
        ) { dispatcher }

        controller.submit(TanbonAction.BlueHead)
        advanceUntilIdle()

        assertEquals(TanbonCommandOutcome.Rejected("event-1", "invalid_session"), controller.latestOutcome)
        assertEquals("invalid_session", outbox.rejectedEvents().single().reason)
    }

    @Test
    fun transportFailureKeepsPendingFeedbackAndTheOriginalDurableCommandForReplay() = runTest {
        val storage = FakeStorage()
        val outbox = DurableEventOutbox(storage)
        val dispatcher = RealtimeCommandDispatcher(this, RealtimeCommandClient(outbox), nowMillis = { 1_000 })
        dispatcher.activate(SerializedRealtimeRequestChannel(FailingSocket()))
        val controller = TanbonCommandController(
            connectedStore(0), runningTanbonSession(), outbox, { 1_000 }, { "event-1" }
        ) { dispatcher }

        controller.submit(TanbonAction.RedBody)
        advanceUntilIdle()

        assertIs<TanbonCommandOutcome.Pending>(controller.latestOutcome)
        assertEquals("event-1", outbox.pendingEvents().single().eventId)
    }

    @Test
    fun unrelatedTerminalResultDoesNotReplaceLatestPendingFeedback() {
        val controller = TanbonCommandController(
            connectedStore(0), runningTanbonSession(), DurableEventOutbox(FakeStorage()), { 1_000 }, { "event-1" }
        )
        controller.submit(TanbonAction.RedHead)

        controller.recordTerminalOutcome(RealtimeCommandResult.Accepted("another-event"))

        assertIs<TanbonCommandOutcome.Pending>(controller.latestOutcome)
    }

    private fun connectedStore(clockOffsetMillis: Long) = ConnectionStateStore().apply {
        dispatch(ConnectionEvent.StartDiscovery)
        dispatch(ConnectionEvent.SelectServer("server-1"))
        dispatch(ConnectionEvent.ValidateMetadata(metadata()))
        dispatch(ConnectionEvent.RequestPairing)
        dispatch(ConnectionEvent.AcceptPairing("device-1", clockOffsetMillis))
    }

    private fun runningTanbonSession() = SessionStateStore().apply {
        update(
            SessionSnapshot(
                sessionId = "session-1",
                discipline = Disciplines.TANBON,
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

    private class ResponseSocket(private val response: String) : RealtimeSocket {
        override suspend fun send(payload: String) = Unit

        override suspend fun receive(): String = response

        override suspend fun close() = Unit
    }

    private class FailingSocket : RealtimeSocket {
        override suspend fun send(payload: String): Nothing = error("connection dropped")

        override suspend fun receive(): String = error("connection dropped")

        override suspend fun close() = Unit
    }
}
