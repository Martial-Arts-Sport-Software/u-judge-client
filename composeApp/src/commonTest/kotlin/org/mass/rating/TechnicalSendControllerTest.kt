package org.mass.rating

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mass.connection.ConnectionEvent
import org.mass.connection.ConnectionStateStore
import org.mass.connection.DueRealtimeOutboxReplay
import org.mass.connection.RealtimeCommandClient
import org.mass.connection.RealtimeCommandDispatcher
import org.mass.connection.RealtimeCommandResult
import org.mass.connection.RealtimeSocket
import org.mass.connection.SerializedRealtimeRequestChannel
import org.mass.enums.Disciplines
import org.mass.session.SentVector
import org.mass.session.SessionFixtures
import org.mass.session.SessionPhase
import org.mass.session.SessionStateStore
import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `Send` of a technical rating (server ADR-011 §7): `CLI-048`, `CLI-050`-`CLI-053`. */
@OptIn(ExperimentalCoroutinesApi::class)
class TechnicalSendControllerTest {
    private val vector = TechnicalVector(listOf(8, 7, 9, 6), listOf(7, 8, 8, 9), 3)

    @Test
    fun sendIsAvailableOnlyForASeatedJudgeOfTheRunningPerformanceOfTheScreen() {
        fun availability(
            session: SessionStateStore,
            connection: ConnectionStateStore = connected(),
            discipline: Disciplines = Disciplines.HOSINSOOL,
            vector: TechnicalVector = this.vector
        ) = TechnicalSendController(connection, session, DurableEventOutbox(FakeStorage())).availability(discipline, vector)

        assertEquals(TechnicalAvailability.AVAILABLE, availability(SessionFixtures.technicalStore()))
        assertEquals(TechnicalAvailability.NOT_CONNECTED, availability(SessionFixtures.technicalStore(), ConnectionStateStore()))
        assertEquals(TechnicalAvailability.SYNCHRONIZING, availability(SessionFixtures.technicalStore().apply { detach() }))
        assertEquals(TechnicalAvailability.NO_PERFORMANCE, availability(SessionFixtures.synchronizedStore()))
        assertEquals(TechnicalAvailability.OTHER_DISCIPLINE, availability(SessionFixtures.technicalStore(), discipline = Disciplines.FREESTYLE_PAIR))
        assertEquals(
            TechnicalAvailability.NOT_SEATED,
            availability(SessionFixtures.technicalStore(SessionFixtures.technical(state = SessionPhase.RUNNING, seat = null)))
        )
        assertEquals(TechnicalAvailability.NOT_RUNNING, availability(SessionFixtures.technicalStore(SessionFixtures.technical())))
        assertEquals(
            TechnicalAvailability.ALREADY_SENT,
            availability(SessionFixtures.technicalStore(SessionFixtures.technical(state = SessionPhase.RUNNING, sent = sent())))
        )
        assertEquals(
            TechnicalAvailability.INVALID_SHAPE,
            availability(SessionFixtures.technicalStore(), vector = TechnicalVector(List(6) { 8 }, List(4) { 8 }, 0)),
            "the snapshot announces four technique criteria"
        )
    }

    @Test
    fun anAcknowledgedSendMakesTheRatingReadOnly() = runTest {
        val outbox = DurableEventOutbox(FakeStorage())
        lateinit var controller: TechnicalSendController
        val client = RealtimeCommandClient(outbox) { controller.recordTerminalOutcome(it) }
        val dispatcher = RealtimeCommandDispatcher(this, outbox, client, nowMillis = { 1_000 })
        dispatcher.activate(SerializedRealtimeRequestChannel(ResponseSocket("""{"type":"command_ack","eventId":"send-1"}""")))
        val session = SessionFixtures.technicalStore()
        controller = TechnicalSendController(connected(), session, outbox, { 1_000 }, { "send-1" }) { dispatcher }

        val pending = assertIs<TechnicalSendOutcome.Pending>(controller.send(Disciplines.HOSINSOOL, vector))
        val body = Json.parseToJsonElement(pending.event.payload).jsonObject
        assertEquals(listOf(8, 7, 9, 6), body.getValue("techniqueTenths").jsonArray.map { it.jsonPrimitive.int })
        assertEquals(3, body.getValue("deductionTenths").jsonPrimitive.int)
        assertEquals(59, body.getValue("totalTenths").jsonPrimitive.int)
        assertTrue(controller.isReadOnly(), "a pending Send is already final")
        advanceUntilIdle()

        assertEquals(TechnicalSendOutcome.Accepted(SessionFixtures.PERFORMANCE_ID, "send-1", 59), controller.latestOutcome)
        assertEquals(emptyList(), outbox.pendingEvents())
        assertTrue(controller.isReadOnly())
        assertEquals(59, controller.sentTotalTenths())
        assertEquals(TechnicalAvailability.ALREADY_SENT, controller.availability(Disciplines.HOSINSOOL, vector))
        assertNull(controller.send(Disciplines.HOSINSOOL, vector), "a second Send of the same performance is impossible")
    }

    @Test
    fun aSendDuringADisconnectStaysPendingAcrossAnAppKillAndReplaysTheSamePayloadAndId() = runTest {
        val storage = FakeStorage()
        val outbox = DurableEventOutbox(storage)
        val dispatcher = RealtimeCommandDispatcher(this, outbox, RealtimeCommandClient(outbox), nowMillis = { 1_000 })
        dispatcher.activate(SerializedRealtimeRequestChannel(FailingSocket()))
        val controller = TechnicalSendController(connected(), SessionFixtures.technicalStore(), outbox, { 1_000 }, { "send-1" }) { dispatcher }
        val event = assertIs<TechnicalSendOutcome.Pending>(controller.send(Disciplines.HOSINSOOL, vector)).event
        advanceUntilIdle()
        assertIs<TechnicalSendOutcome.Pending>(controller.latestOutcome)

        // The app is killed: a new process restores the pending Send and keeps the rating read-only.
        val restoredOutbox = DurableEventOutbox(storage)
        val restored = TechnicalSendController(connected(), SessionFixtures.technicalStore(), restoredOutbox)
        assertEquals(TechnicalSendOutcome.Pending(event), restored.latestOutcome)
        assertTrue(restored.isReadOnly())

        // On the next socket the replay sends exactly the stored event and the ACK settles it.
        val socket = RecordingSocket("""{"type":"command_ack","eventId":"send-1"}""")
        val client = RealtimeCommandClient(restoredOutbox) { restored.recordTerminalOutcome(it) }
        assertTrue(DueRealtimeOutboxReplay(restoredOutbox, client, nowMillis = { 60_000 }).replay(socket))
        val frame = Json.parseToJsonElement(socket.sent.single()).jsonObject
        assertEquals("send-1", frame.getValue("eventId").jsonPrimitive.content)
        assertEquals(event.clientSequence, frame.getValue("sequence").jsonPrimitive.content.toLong())
        assertEquals(59, frame.getValue("totalTenths").jsonPrimitive.int)
        assertEquals(TechnicalSendOutcome.Accepted(SessionFixtures.PERFORMANCE_ID, "send-1", 59), restored.latestOutcome)
    }

    @Test
    fun aTerminalRejectionKeepsThePayloadAndItsCode() = runTest {
        val storage = FakeStorage()
        val outbox = DurableEventOutbox(storage)
        lateinit var controller: TechnicalSendController
        val client = RealtimeCommandClient(outbox) { controller.recordTerminalOutcome(it) }
        val dispatcher = RealtimeCommandDispatcher(this, outbox, client, nowMillis = { 1_000 })
        dispatcher.activate(
            SerializedRealtimeRequestChannel(ResponseSocket("""{"type":"command_rejected","code":"session_closed","eventId":"send-1"}"""))
        )
        controller = TechnicalSendController(connected(), SessionFixtures.technicalStore(), outbox, { 1_000 }, { "send-1" }) { dispatcher }

        controller.send(Disciplines.HOSINSOOL, vector)
        advanceUntilIdle()

        assertEquals(TechnicalSendOutcome.Rejected(SessionFixtures.PERFORMANCE_ID, "send-1", "session_closed"), controller.latestOutcome)
        val rejected = outbox.rejectedEvents().single()
        assertEquals("session_closed", rejected.reason)
        assertEquals(59, Json.parseToJsonElement(rejected.event.payload).jsonObject.getValue("totalTenths").jsonPrimitive.int)
        assertEquals(
            TechnicalSendOutcome.Rejected(SessionFixtures.PERFORMANCE_ID, "send-1", "session_closed"),
            TechnicalSendController(connected(), SessionFixtures.technicalStore(), DurableEventOutbox(storage)).latestOutcome,
            "the rejection survives a restart"
        )
        assertFalse(controller.isReadOnly(), "a refused Send is not final")
    }

    @Test
    fun anAlreadySentRejectionOrTheSnapshotsSentVectorAreReadOnly() {
        val outbox = DurableEventOutbox(FakeStorage())
        val controller = TechnicalSendController(connected(), SessionFixtures.technicalStore(), outbox, { 1_000 }, { "send-2" })
        controller.send(Disciplines.HOSINSOOL, vector)
        outbox.reject("send-2", "score_already_sent")
        controller.recordTerminalOutcome(RealtimeCommandResult.Rejected("send-2", "score_already_sent"))
        assertTrue(controller.isReadOnly())

        val reinstalled = TechnicalSendController(
            connected(),
            SessionFixtures.technicalStore(SessionFixtures.technical(state = SessionPhase.RUNNING, sent = sent())),
            DurableEventOutbox(FakeStorage())
        )
        assertTrue(reinstalled.isReadOnly())
        assertEquals(59, reinstalled.sentTotalTenths())
    }

    @Test
    fun anUnrelatedReplyDoesNotSettleThePendingSend() {
        val controller = TechnicalSendController(connected(), SessionFixtures.technicalStore(), DurableEventOutbox(FakeStorage()), { 1_000 }, { "send-1" })
        controller.send(Disciplines.HOSINSOOL, vector)
        controller.recordTerminalOutcome(RealtimeCommandResult.Accepted("another-event"))
        assertIs<TechnicalSendOutcome.Pending>(controller.latestOutcome)
    }

    private fun sent() = SentVector("sent-1", listOf(8, 7, 9, 6), listOf(7, 8, 8, 9), 3, 59)

    private fun connected() = ConnectionStateStore().apply {
        dispatch(ConnectionEvent.ResumePairedServer(SessionFixtures.DEVICE_ID))
        dispatch(ConnectionEvent.AcceptPairing(SessionFixtures.DEVICE_ID, clockOffsetMillis = 0))
    }

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

    private class RecordingSocket(private val response: String) : RealtimeSocket {
        val sent = mutableListOf<String>()
        override suspend fun send(payload: String) {
            sent += payload
        }
        override suspend fun receive(): String = response
        override suspend fun close() = Unit
    }

    private class FailingSocket : RealtimeSocket {
        override suspend fun send(payload: String): Nothing = error("connection dropped")
        override suspend fun receive(): String = error("connection dropped")
        override suspend fun close() = Unit
    }
}
