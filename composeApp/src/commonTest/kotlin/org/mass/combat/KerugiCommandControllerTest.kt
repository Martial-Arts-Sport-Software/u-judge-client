package org.mass.combat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mass.connection.ConnectionEvent
import org.mass.connection.ConnectionFailure
import org.mass.connection.ConnectionStateStore
import org.mass.connection.RealtimeCommandClient
import org.mass.connection.RealtimeCommandDispatcher
import org.mass.connection.RealtimeCommandResult
import org.mass.connection.RealtimeRequestChannel
import org.mass.session.SessionFixtures
import org.mass.session.SessionPhase
import org.mass.session.SessionPush
import org.mass.session.SessionStateStore
import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KerugiCommandControllerTest {
    @Test
    fun tapPersistsTheTypedCommandWithTheAuditContextOfTheSnapshotBeforeSending() {
        val storage = FakeStorage()
        val eventIds = ArrayDeque(listOf("event-1", "event-2"))
        val controller = KerugiCommandController(
            connection = connectedStore(clockOffsetMillis = 250),
            session = SessionFixtures.synchronizedStore(),
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
        assertEquals(SessionFixtures.SESSION_ID, first.sessionId)
        assertEquals(
            Json.parseToJsonElement(
                """{"type":"kerugi_score_command","competitionId":"${SessionFixtures.COMPETITION_ID}",""" +
                    """"peerId":"${SessionFixtures.PEER_ID}","courtId":"${SessionFixtures.COURT_ID}",""" +
                    """"bracketId":"${SessionFixtures.BRACKET_ID}","sessionId":"${SessionFixtures.SESSION_ID}",""" +
                    """"judgeId":"${SessionFixtures.JUDGE_ID}","deviceId":"${SessionFixtures.DEVICE_ID}",""" +
                    """"competitor":"BLUE","area":"HEAD"}"""
            ),
            Json.parseToJsonElement(first.payload)
        )
        assertEquals(1, second.clientSequence)
        assertEquals("RED", second.field("competitor"))
        assertEquals("BODY", second.field("area"))
        assertEquals(listOf(first, second), DurableEventOutbox(storage).pendingEvents())
        assertEquals(2, controller.pendingCount)
    }

    @Test
    fun controlsAreEnabledOnlyForASeatedJudgeOfARunningKerugiBoutSynchronizedOnThisSocket() {
        fun availability(connection: ConnectionStateStore = connectedStore(0), session: SessionStateStore) =
            controller(connection, session).availability

        assertEquals(KerugiAvailability.AVAILABLE, availability(session = SessionFixtures.synchronizedStore()))
        assertEquals(
            KerugiAvailability.NOT_CONNECTED,
            availability(connection = reconnectingStore(), session = SessionFixtures.synchronizedStore())
        )
        assertEquals(KerugiAvailability.SYNCHRONIZING, availability(session = SessionStateStore()))
        assertEquals(
            KerugiAvailability.SYNCHRONIZING,
            availability(session = SessionFixtures.synchronizedStore().apply { detach() })
        )
        assertEquals(KerugiAvailability.NO_BOUT, availability(session = SessionFixtures.synchronizedStore(null)))
        assertEquals(
            KerugiAvailability.OTHER_DISCIPLINE,
            availability(session = SessionFixtures.synchronizedStore(SessionFixtures.snapshot(discipline = "tanbon")))
        )
        assertEquals(
            KerugiAvailability.NOT_SEATED,
            availability(session = SessionFixtures.synchronizedStore(SessionFixtures.snapshot(seat = null)))
        )
        listOf(SessionPhase.PREPARED, SessionPhase.PAUSED, SessionPhase.COMPLETED, SessionPhase.CANCELLED).forEach {
            assertEquals(
                KerugiAvailability.NOT_RUNNING,
                availability(session = SessionFixtures.synchronizedStore(SessionFixtures.snapshot(state = it)))
            )
        }
    }

    @Test
    fun aDisabledTapPersistsNothing() {
        val storage = FakeStorage()
        val controller = KerugiCommandController(
            connection = connectedStore(clockOffsetMillis = 0),
            session = SessionFixtures.synchronizedStore(SessionFixtures.snapshot(state = SessionPhase.PAUSED)),
            outbox = DurableEventOutbox(storage),
            nowMillis = { 1_000 },
            eventId = { "event-1" }
        )

        assertFalse(controller.isAvailable)
        assertEquals(KerugiCommandOutcome.Unavailable, controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD))
        assertEquals(emptyList(), DurableEventOutbox(storage).pendingEvents())
    }

    @Test
    fun aStateUpdateToPausedDisablesTheControls() {
        val session = SessionFixtures.synchronizedStore()
        val controller = controller(connectedStore(0), session)
        assertTrue(controller.isAvailable)

        session.applyPush(SessionPush.StateUpdated(SessionFixtures.SESSION_ID, 119, SessionPhase.PAUSED, null))

        assertFalse(controller.isAvailable)
        assertTrue(controller.isAttentionAvailable)
    }

    @Test
    fun attentionSignalIsATypedCommandWithoutScoringFieldsAllowedUntilTheBoutEnds() {
        listOf(SessionPhase.PREPARED, SessionPhase.RUNNING, SessionPhase.PAUSED).forEach { state ->
            val controller = controller(
                connectedStore(0),
                SessionFixtures.synchronizedStore(SessionFixtures.snapshot(state = state))
            )

            val event = assertIs<KerugiCommandOutcome.Pending>(controller.signalAttention()).event

            assertEquals("judge_attention_command", event.field("type"))
            assertEquals(SessionFixtures.JUDGE_ID, event.field("judgeId"))
            assertEquals(SessionFixtures.DEVICE_ID, event.field("deviceId"))
            assertFalse("competitor" in Json.parseToJsonElement(event.payload).jsonObject)
            assertIs<KerugiCommandOutcome.Pending>(controller.latestAttention)
            assertEquals(null, controller.latestOutcome)
        }
        listOf(SessionPhase.COMPLETED, SessionPhase.CANCELLED).forEach { state ->
            val controller = controller(
                connectedStore(0),
                SessionFixtures.synchronizedStore(SessionFixtures.snapshot(state = state))
            )
            assertFalse(controller.isAttentionAvailable)
            assertEquals(KerugiCommandOutcome.Unavailable, controller.signalAttention())
        }
    }

    @Test
    fun aQueuedTapKeepsTheAuditContextOfItsBoutAfterTheCourtMovesOn() {
        val storage = FakeStorage()
        val session = SessionFixtures.synchronizedStore()
        val ids = ArrayDeque(listOf("event-1", "event-2"))
        val controller = KerugiCommandController(connectedStore(0), session, DurableEventOutbox(storage), { 1_000 }, {
            ids.removeFirst()
        })
        controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)

        session.applyPush(SessionPush.Snapshot(130, SessionFixtures.snapshot(sessionId = "next-session", boutNumber = 8)))
        controller.submit(KerugiParticipant.RED, KerugiTarget.HEAD)

        val (old, new) = DurableEventOutbox(storage).pendingEvents()
        assertEquals(SessionFixtures.SESSION_ID, old.field("sessionId"))
        assertEquals("next-session", new.field("sessionId"))
        assertEquals(SessionFixtures.JUDGE_ID, new.field("judgeId"))
    }

    @Test
    fun feedbackChangesOnlyOnTheServerReplyForItsOwnEvent() = runTest {
        val outbox = DurableEventOutbox(FakeStorage())
        val channel = ScriptedChannel { """{"type":"command_ack","eventId":"${it.eventId()}"}""" }
        val controller = dispatchedController(outbox, channel, "event-1")

        controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        assertIs<KerugiCommandOutcome.Pending>(controller.latestOutcome)
        controller.recordTerminalOutcome(RealtimeCommandResult.Accepted("another-event"))
        assertIs<KerugiCommandOutcome.Pending>(controller.latestOutcome)
        advanceUntilIdle()

        assertEquals(KerugiCommandOutcome.Accepted("event-1"), controller.latestOutcome)
        assertEquals(emptyList(), outbox.pendingEvents())
        assertEquals(0, controller.pendingCount)
    }

    @Test
    fun terminalRejectionIsRetainedShownAndNeverResent() = runTest {
        val outbox = DurableEventOutbox(FakeStorage())
        val channel = ScriptedChannel {
            """{"type":"command_rejected","code":"session_closed","eventId":"${it.eventId()}"}"""
        }
        val controller = dispatchedController(outbox, channel, "event-1")

        controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        advanceTimeBy(120_000)

        assertEquals(KerugiCommandOutcome.Rejected("event-1", "session_closed"), controller.latestOutcome)
        assertEquals("session_closed", outbox.rejectedEvents().single().reason)
        assertEquals(emptyList(), outbox.pendingEvents())
        assertEquals(1, channel.sent.size)
    }

    @Test
    fun journalUnavailableKeepsTheTapPendingAndRetriesTheSameEventIdAfterBackoff() = runTest {
        val outbox = DurableEventOutbox(FakeStorage(), retryBaseMillis = 1_000)
        val replies = ArrayDeque(
            listOf(
                """{"type":"command_rejected","code":"journal_unavailable","eventId":"event-1"}""",
                """{"type":"command_ack","eventId":"event-1"}"""
            )
        )
        val channel = ScriptedChannel { replies.removeFirst() }
        val controller = dispatchedController(outbox, channel, "event-1")

        controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        runCurrent()
        assertIs<KerugiCommandOutcome.Pending>(controller.latestOutcome)
        assertEquals("event-1", outbox.pendingEvents().single().eventId)
        assertEquals(emptyList(), outbox.rejectedEvents())
        assertEquals(1, channel.sent.size)

        advanceTimeBy(1_001)

        assertEquals(KerugiCommandOutcome.Accepted("event-1"), controller.latestOutcome)
        assertEquals(listOf("event-1", "event-1"), channel.sent.map { it.eventId() })
    }

    @Test
    fun aLaterTapWaitsBehindAnEarlierTapInBackoff() = runTest {
        val outbox = DurableEventOutbox(FakeStorage(), retryBaseMillis = 1_000)
        val replies = ArrayDeque(
            listOf(
                """{"type":"command_rejected","code":"journal_unavailable","eventId":"event-1"}""",
                """{"type":"command_ack","eventId":"event-1"}""",
                """{"type":"command_ack","eventId":"event-2"}"""
            )
        )
        val channel = ScriptedChannel { replies.removeFirst() }
        val ids = ArrayDeque(listOf("event-1", "event-2"))
        lateinit var controller: KerugiCommandController
        val client = RealtimeCommandClient(outbox) { controller.recordTerminalOutcome(it) }
        val dispatcher = RealtimeCommandDispatcher(this, outbox, client) { currentTime }
        dispatcher.activate(channel)
        controller = KerugiCommandController(
            connectedStore(0), SessionFixtures.synchronizedStore(), outbox, { currentTime }, { ids.removeFirst() }
        ) { dispatcher }

        controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        runCurrent()
        controller.submit(KerugiParticipant.RED, KerugiTarget.BODY)
        runCurrent()
        assertEquals(listOf("event-1"), channel.sent.map { it.eventId() })

        advanceUntilIdle()

        assertEquals(listOf("event-1", "event-1", "event-2"), channel.sent.map { it.eventId() })
        assertEquals(emptyList(), outbox.pendingEvents())
    }

    private fun TestScope.dispatchedController(
        outbox: DurableEventOutbox,
        channel: RealtimeRequestChannel,
        eventId: String
    ): KerugiCommandController {
        lateinit var controller: KerugiCommandController
        val client = RealtimeCommandClient(outbox) { controller.recordTerminalOutcome(it) }
        val dispatcher = RealtimeCommandDispatcher(this, outbox, client) { currentTime }
        dispatcher.activate(channel)
        controller = KerugiCommandController(
            connectedStore(0), SessionFixtures.synchronizedStore(), outbox, { currentTime }, { eventId }
        ) { dispatcher }
        return controller
    }

    private fun controller(connection: ConnectionStateStore, session: SessionStateStore) = KerugiCommandController(
        connection, session, DurableEventOutbox(FakeStorage()), { 1_000 }, { "event-1" }
    )

    private fun org.mass.transport.OutboxEvent.field(name: String) =
        Json.parseToJsonElement(payload).jsonObject.getValue(name).jsonPrimitive.content

    private fun String.eventId() = Json.parseToJsonElement(this).jsonObject.getValue("eventId").jsonPrimitive.content

    private fun connectedStore(clockOffsetMillis: Long) = ConnectionStateStore().apply {
        dispatch(ConnectionEvent.StartDiscovery)
        dispatch(ConnectionEvent.SelectServer("server-1"))
        dispatch(ConnectionEvent.ValidateMetadata(metadata()))
        dispatch(ConnectionEvent.RequestPairing)
        dispatch(ConnectionEvent.AcceptPairing("device-1", clockOffsetMillis))
    }

    private fun reconnectingStore() = connectedStore(0).apply {
        dispatch(ConnectionEvent.HeartbeatFailed(ConnectionFailure.HeartbeatUnavailable))
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

    /** Answers each request with [reply] and records what was sent. */
    private class ScriptedChannel(private val reply: (String) -> String) : RealtimeRequestChannel {
        val sent = mutableListOf<String>()

        override suspend fun exchange(payload: String): String {
            sent += payload
            return reply(payload)
        }

        override suspend fun close() = Unit
    }
}
