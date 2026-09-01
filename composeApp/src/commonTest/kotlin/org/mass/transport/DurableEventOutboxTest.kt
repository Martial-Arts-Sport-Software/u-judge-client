package org.mass.transport

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DurableEventOutboxTest {
    @Test
    fun pendingEventSurvivesRepositoryRecreationWithItsOriginalIdentity() {
        val storage = FakeEventOutboxStorage()
        val event = OutboxEvent(
            eventId = "event-1",
            clientSequence = 4,
            clientTimestampMillis = 1_000,
            payload = "{\"kind\":\"kerugi\"}"
        )

        DurableEventOutbox(storage).enqueue(event)

        assertEquals(listOf(event), DurableEventOutbox(storage).pendingEvents())
    }

    @Test
    fun acknowledgementRemovesOnlyTheMatchingPendingEvent() {
        val outbox = DurableEventOutbox(FakeEventOutboxStorage())
        val first = event("event-1", 1)
        val second = event("event-2", 2)
        outbox.enqueue(first)
        outbox.enqueue(second)

        assertTrue(outbox.acknowledge("event-1"))

        assertEquals(listOf(second), outbox.pendingEvents())
        assertFalse(outbox.acknowledge("missing-event"))
    }

    @Test
    fun terminalRejectionPersistsWithoutBecomingEligibleForRetry() {
        val storage = FakeEventOutboxStorage()
        val event = event("event-1", 1)
        val outbox = DurableEventOutbox(storage)
        outbox.enqueue(event)

        assertTrue(outbox.reject("event-1", "session_completed"))

        val recreated = DurableEventOutbox(storage)
        assertNull(recreated.nextEventDue(10_000))
        assertEquals(listOf(RejectedOutboxEvent(event, "session_completed")), recreated.rejectedEvents())
    }

    @Test
    fun retriesUseBoundedExponentialBackoffWithoutReorderingEvents() {
        val outbox = DurableEventOutbox(
            storage = FakeEventOutboxStorage(),
            retryBaseMillis = 100,
            retryMaximumMillis = 250
        )
        val first = event("event-1", 1)
        val second = event("event-2", 2)
        outbox.enqueue(first)
        outbox.enqueue(second)

        assertEquals(first, outbox.nextEventDue(0))
        assertEquals(100, outbox.recordAttempt("event-1", 0)?.nextAttemptAtMillis)
        assertNull(outbox.nextEventDue(99))
        assertEquals(first, outbox.nextEventDue(100))
        assertEquals(300, outbox.recordAttempt("event-1", 100)?.nextAttemptAtMillis)
        assertEquals(first, outbox.nextEventDue(300))
        assertEquals(550, outbox.recordAttempt("event-1", 300)?.nextAttemptAtMillis)
        assertEquals(first, outbox.nextEventDue(550))
        assertEquals(800, outbox.recordAttempt("event-1", 550)?.nextAttemptAtMillis)
        assertEquals(first, outbox.nextEventDue(800))

        outbox.acknowledge("event-1")

        assertEquals(second, outbox.nextEventDue(800))
    }

    private fun event(eventId: String, clientSequence: Long) = OutboxEvent(
        eventId = eventId,
        clientSequence = clientSequence,
        clientTimestampMillis = 0,
        payload = "payload-$eventId"
    )

    private class FakeEventOutboxStorage : EventOutboxStorage {
        private var value: String? = null

        override fun load(): String? = value

        override fun save(value: String) {
            this.value = value
        }
    }
}
