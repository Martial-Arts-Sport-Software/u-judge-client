package org.mass.connection

import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import org.mass.transport.OutboxEvent
import org.mass.transport.RejectedOutboxEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class RealtimeOutboxReplayTest {
    @Test
    fun replaysDueCommandsInSequenceUntilTheirTerminalOutcomes() = runTest {
        val outbox = DurableEventOutbox(FakeStorage())
        val first = event("event-1", 1)
        val second = event("event-2", 2)
        outbox.enqueue(second)
        outbox.enqueue(first)
        val socket = ResponseSocket(
            """{"type":"command_ack","eventId":"event-1"}""",
            """{"type":"command_rejected","eventId":"event-2","code":"session_completed"}"""
        )

        assertEquals(true, DueRealtimeOutboxReplay(outbox, nowMillis = { 100 }).replay(socket))

        assertEquals(
            listOf("event-1", "event-2"),
            socket.sent.map { "\"eventId\":\"([^\"]+)\"".toRegex().find(it)!!.groupValues[1] }
        )
        assertEquals(emptyList(), outbox.pendingEvents())
        assertEquals(listOf(RejectedOutboxEvent(second, "session_completed")), outbox.rejectedEvents())
    }

    @Test
    fun doesNotSendLaterCommandsWhenTheEarliestCommandIsNotDue() = runTest {
        val outbox = DurableEventOutbox(FakeStorage())
        val first = event("event-1", 1, timestampMillis = 200)
        val second = event("event-2", 2, timestampMillis = 0)
        outbox.enqueue(first)
        outbox.enqueue(second)
        val socket = ResponseSocket()

        assertEquals(true, DueRealtimeOutboxReplay(outbox, nowMillis = { 100 }).replay(socket))

        assertEquals(emptyList(), socket.sent)
        assertEquals(listOf(first, second), outbox.pendingEvents())
    }

    @Test
    fun invalidResponseKeepsTheCurrentAndLaterCommandsPending() = runTest {
        val outbox = DurableEventOutbox(FakeStorage())
        val first = event("event-1", 1)
        val second = event("event-2", 2)
        outbox.enqueue(first)
        outbox.enqueue(second)
        val socket = ResponseSocket("""{"type":"command_ack","eventId":"event-2"}""")

        assertEquals(false, DueRealtimeOutboxReplay(outbox, nowMillis = { 100 }).replay(socket))

        assertEquals(1, socket.sent.size)
        assertEquals(listOf(first, second), outbox.pendingEvents())
    }

    private fun event(eventId: String, sequence: Long, timestampMillis: Long = 0) = OutboxEvent(
        eventId = eventId,
        clientSequence = sequence,
        clientTimestampMillis = timestampMillis,
        clientTimestamp = "2026-09-01T10:00:00Z",
        sessionId = "session-1",
        payload = """{"type":"attention"}"""
    )

    private class ResponseSocket(vararg responses: String) : RealtimeSocket {
        val sent = mutableListOf<String>()
        private val responses = responses.toMutableList()

        override suspend fun send(payload: String) {
            sent += payload
        }

        override suspend fun receive(): String = responses.removeFirst()

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
