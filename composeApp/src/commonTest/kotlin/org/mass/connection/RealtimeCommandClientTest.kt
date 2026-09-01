package org.mass.connection

import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import org.mass.transport.OutboxEvent
import org.mass.transport.RejectedOutboxEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class RealtimeCommandClientTest {
    @Test
    fun acknowledgementRemovesOnlyTheMatchingPersistedCommand() = runTest {
        val outbox = DurableEventOutbox(FakeStorage())
        val event = event("event-1", 1)
        val laterEvent = event("event-2", 2)
        outbox.enqueue(laterEvent)
        val socket = FakeSocket("""{"type":"command_ack","eventId":"event-1"}""")

        assertEquals(RealtimeCommandResult.Accepted("event-1"), RealtimeCommandClient(outbox).send(event, socket, 100))

        assertEquals(
            "{\"type\":\"command\",\"eventId\":\"event-1\",\"sequence\":1,\"clientTimestamp\":\"2026-09-01T10:00:00Z\",\"sessionId\":\"session-1\",\"payload\":{\"type\":\"attention\"}}",
            socket.sent.single()
        )
        assertEquals(listOf(laterEvent), outbox.pendingEvents())
    }

    @Test
    fun terminalRejectionPersistsServerCodeWithoutRetryingCommand() = runTest {
        val outbox = DurableEventOutbox(FakeStorage())
        val event = event("event-1", 1)
        val socket = FakeSocket("""{"type":"command_rejected","eventId":"event-1","code":"invalid_session"}""")

        assertEquals(RealtimeCommandResult.Rejected("event-1", "invalid_session"), RealtimeCommandClient(outbox).send(event, socket, 100))

        assertEquals(emptyList(), outbox.pendingEvents())
        assertEquals(listOf(RejectedOutboxEvent(event, "invalid_session")), outbox.rejectedEvents())
    }

    @Test
    fun malformedResponseLeavesPersistedCommandPendingForRetry() = runTest {
        val outbox = DurableEventOutbox(FakeStorage())
        val event = event("event-1", 1)
        val socket = FakeSocket("""{"type":"unknown"}""")

        assertEquals(RealtimeCommandResult.InvalidResponse, RealtimeCommandClient(outbox).send(event, socket, 100))

        assertEquals(listOf(event), outbox.pendingEvents())
    }

    private fun event(eventId: String, sequence: Long) = OutboxEvent(
        eventId = eventId,
        clientSequence = sequence,
        clientTimestampMillis = 0,
        clientTimestamp = "2026-09-01T10:00:00Z",
        sessionId = "session-1",
        payload = """{"type":"attention"}"""
    )

    private class FakeSocket(private val response: String) : RealtimeSocket {
        val sent = mutableListOf<String>()

        override suspend fun send(payload: String) {
            sent += payload
        }

        override suspend fun receive(): String = response

        override suspend fun close() = Unit
    }

    private class FakeStorage : EventOutboxStorage {
        private var value: String? = null

        override fun load(): String? = value

        override fun save(value: String) {
            this.value = value
        }
    }
}
