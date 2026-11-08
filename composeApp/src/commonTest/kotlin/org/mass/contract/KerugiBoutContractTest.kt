package org.mass.contract

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mass.State
import org.mass.combat.KerugiCommandController
import org.mass.combat.KerugiCommandOutcome
import org.mass.combat.KerugiParticipant
import org.mass.combat.KerugiTarget
import org.mass.connection.ConnectionEvent
import org.mass.connection.ConnectionFailure
import org.mass.connection.ConnectionState
import org.mass.connection.ConnectionStateStore
import org.mass.connection.MetadataFetchResult
import org.mass.connection.RealtimeCommandClient
import org.mass.connection.RealtimeCommandResult
import org.mass.connection.RealtimeRequestChannel
import org.mass.connection.ServerMetadata
import org.mass.connection.ServerMetadataClient
import org.mass.session.BoutPeriod
import org.mass.session.BoutResult
import org.mass.session.BoutTimer
import org.mass.session.Competitor
import org.mass.session.KerugiScore
import org.mass.session.ResyncReply
import org.mass.session.ScoreWarning
import org.mass.session.SessionFixtures
import org.mass.session.SessionMessages
import org.mass.session.SessionPhase
import org.mass.session.SessionPush
import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Client side of the Kerugi bout contract (server ADR-009): every server fixture at `contract/server-ref` decodes, and the
 * client encodes its requests exactly as the request fixtures.
 */
class KerugiBoutContractTest {
    @Test
    fun theMetadataFixtureOffersTheKerugiBoutCapabilityTheClientRequires() = runTest {
        val result = ServerMetadataClient(
            HttpClient(MockEngine { respond(ServerContractFixtures.message("metadata_response"), HttpStatusCode.OK) }),
            Url("https://court.local:8443")
        ).fetch()
        val metadata = assertIs<MetadataFetchResult.Success>(result).metadata
        assertTrue(State.KERUGI_BOUT_CAPABILITY in metadata.capabilities)

        assertIs<ConnectionState.MetadataValidated>(validate(metadata))
        assertEquals(
            ConnectionFailure.MissingRequiredCapabilities(setOf("kerugi_bout", "tanbon_bout", "technical_performance")),
            assertIs<ConnectionState.Rejected>(validate(metadata.copy(capabilities = setOf("metadata")))).failure
        )
    }

    @Test
    fun sessionSnapshotFixtureDecodesEveryField() {
        val push = assertIs<SessionPush.Snapshot>(SessionMessages.decodePush(fixture("session_snapshot")))

        assertEquals(118, push.cursor)
        assertEquals(
            SessionFixtures.snapshot(
                state = SessionPhase.PREPARED,
                score = KerugiScore(0, 0, 0, 0, emptyList(), null),
                timer = SessionFixtures.preparedTimer
            ),
            push.session
        )
    }

    @Test
    fun resyncRequestMatchesTheFixture() {
        val request = fixture("resync_request")

        assertEquals(request, json(SessionMessages.resyncRequest(request.text("requestId"), null)))
        assertEquals(JsonPrimitive(0), json(SessionMessages.resyncRequest(request.text("requestId"), 0))["cursor"])
    }

    @Test
    fun resyncReplyFixturesDecode() {
        val requestId = fixture("resync_request").text("requestId")

        assertEquals(
            ResyncReply.Response(requestId, 124, SessionFixtures.resyncFixtureSession()),
            SessionMessages.decodeResyncReply(fixture("resync_response"))
        )
        assertEquals(
            ResyncReply.Response(requestId, 0, null),
            SessionMessages.decodeResyncReply(fixture("resync_response_idle"))
        )
        assertEquals(
            ResyncReply.Rejected(requestId, "invalid_resync_request"),
            SessionMessages.decodeResyncReply(fixture("resync_rejected"))
        )
    }

    @Test
    fun stateAndScorePushFixturesDecode() {
        assertEquals(
            SessionPush.StateUpdated(
                SessionFixtures.SESSION_ID,
                125,
                SessionPhase.COMPLETED,
                BoutResult(Competitor.BLUE, "final_score"),
                SessionFixtures.preparedTimer.copy(round = 2, remainingMillis = 0)
            ),
            SessionMessages.decodePush(fixture("session_state_updated"))
        )
        assertEquals(
            SessionPush.ScoreUpdated(
                SessionFixtures.SESSION_ID,
                124,
                KerugiScore(12, 0, 0, 10, listOf(ScoreWarning("disqualification_pending", Competitor.RED, 10)), null)
            ),
            SessionMessages.decodePush(fixture("kerugi_score_updated"))
        )
    }

    @Test
    fun timerPushFixtureDecodes() {
        assertEquals(
            SessionPush.TimerUpdated(
                SessionFixtures.SESSION_ID,
                127,
                BoutTimer(BoutPeriod.BREAK, 2, false, 60_000, 0, running = false, runningSinceMillis = null)
            ),
            SessionMessages.decodePush(fixture("kerugi_timer_updated"))
        )
    }

    @Test
    fun readySignalIsEncodedExactlyAsTheFixture() = runTest {
        val command = fixture("judge_ready_command")
        val outbox = outbox()
        // The fixture carries sequence 2: two earlier commands of this device.
        listOf("earlier-1", "earlier-2").forEach {
            outbox.enqueueNew(it, 0, "2026-11-08T10:15:00Z", SessionFixtures.SESSION_ID, "{}")
            outbox.acknowledge(it)
        }
        val controller = KerugiCommandController(
            connection = ConnectionStateStore().apply {
                dispatch(ConnectionEvent.ResumePairedServer(SessionFixtures.DEVICE_ID))
                dispatch(ConnectionEvent.AcceptPairing(SessionFixtures.DEVICE_ID, clockOffsetMillis = 0))
            },
            session = SessionFixtures.synchronizedStore(SessionFixtures.snapshot(state = SessionPhase.PREPARED)),
            outbox = outbox,
            nowMillis = { Instant.parse(command.text("clientTimestamp")).toEpochMilliseconds() },
            eventId = { command.text("eventId") }
        )
        val event = assertIs<KerugiCommandOutcome.Pending>(controller.signalReady()).event
        val channel = CapturingChannel("""{"type":"command_ack","eventId":"${command.text("eventId")}"}""")

        RealtimeCommandClient(outbox).send(event, channel, nowMillis = 0)

        assertEquals(command, json(channel.sent.single()))
    }

    @Test
    fun serverMessagesMayGainFieldsButNotLoseListedOnes() {
        val update = fixture("session_state_updated")
        assertIs<SessionPush.StateUpdated>(SessionMessages.decodePush(JsonObject(update + ("next" to json("""{"a":1}""")))))
        assertEquals(null, SessionMessages.decodePush(JsonObject(update + ("timer" to json("""{"a":1}""")))))
        assertEquals(null, SessionMessages.decodePush(JsonObject(update - "result")))
        assertEquals(null, SessionMessages.decodePush(JsonObject(update + ("cursor" to JsonPrimitive("125")))))
        assertEquals(null, SessionMessages.decodePush(json("""{"type":"kerugi_timer_updated","cursor":1}""")))
    }

    @Test
    fun aDisqualificationResultDecodes() {
        val update = JsonObject(
            fixture("session_state_updated") + ("result" to json("""{"winner":"BLUE","reason":"disqualification"}"""))
        )

        assertEquals(
            BoutResult(Competitor.BLUE, "disqualification"),
            assertIs<SessionPush.StateUpdated>(SessionMessages.decodePush(update)).result
        )
    }

    @Test
    fun scoreTapIsEncodedExactlyAsTheFixtureAndTheAckFixtureSettlesIt() = runTest {
        val command = fixture("kerugi_score_command")
        val outbox = outbox()
        val event = assertIs<KerugiCommandOutcome.Pending>(
            controller(outbox, command).submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        ).event
        val channel = CapturingChannel(ServerContractFixtures.message("command_ack"))

        assertEquals(
            RealtimeCommandResult.Accepted(command.text("eventId")),
            RealtimeCommandClient(outbox).send(event, channel, nowMillis = 0)
        )
        assertEquals(command, json(channel.sent.single()))
        assertEquals(emptyList(), outbox.pendingEvents())
    }

    @Test
    fun attentionSignalIsEncodedExactlyAsTheFixture() = runTest {
        val command = fixture("judge_attention_command")
        val outbox = outbox()
        // The fixture carries sequence 1: one earlier tap of this device.
        outbox.enqueueNew("earlier-tap", 0, "2026-11-08T10:15:30.120Z", SessionFixtures.SESSION_ID, "{}")
        outbox.acknowledge("earlier-tap")
        val event = assertIs<KerugiCommandOutcome.Pending>(controller(outbox, command).signalAttention()).event
        val channel = CapturingChannel("""{"type":"command_ack","eventId":"${command.text("eventId")}"}""")

        RealtimeCommandClient(outbox).send(event, channel, nowMillis = 0)

        assertEquals(command, json(channel.sent.single()))
    }

    @Test
    fun theRejectionFixtureIsTerminal() = runTest {
        val command = fixture("kerugi_score_command")
        val outbox = outbox()
        val event = assertIs<KerugiCommandOutcome.Pending>(
            controller(outbox, command).submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        ).event

        assertEquals(
            RealtimeCommandResult.Rejected(command.text("eventId"), "event_id_conflict"),
            RealtimeCommandClient(outbox).send(
                event,
                CapturingChannel(ServerContractFixtures.message("command_rejected")),
                nowMillis = 0
            )
        )
        assertEquals("event_id_conflict", outbox.rejectedEvents().single().reason)
    }

    /** A controller seated as in the fixtures that taps at the fixture's time with the fixture's event ID. */
    private fun controller(outbox: DurableEventOutbox, command: JsonObject) = KerugiCommandController(
        connection = ConnectionStateStore().apply {
            dispatch(ConnectionEvent.ResumePairedServer(SessionFixtures.DEVICE_ID))
            dispatch(ConnectionEvent.AcceptPairing(SessionFixtures.DEVICE_ID, clockOffsetMillis = 0))
        },
        session = SessionFixtures.synchronizedStore(),
        outbox = outbox,
        nowMillis = { Instant.parse(command.text("clientTimestamp")).toEpochMilliseconds() },
        eventId = { command.text("eventId") }
    )

    private fun validate(metadata: ServerMetadata) = ConnectionStateStore(
        requiredCapabilities = State.REQUIRED_SERVER_CAPABILITIES
    ).apply {
        dispatch(ConnectionEvent.StartDiscovery)
        dispatch(ConnectionEvent.SelectServer("court"))
        dispatch(ConnectionEvent.ValidateMetadata(metadata))
    }.state

    private fun fixture(name: String) = json(ServerContractFixtures.message(name))

    private fun json(value: String): JsonObject = Json.parseToJsonElement(value).jsonObject

    private fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content

    private fun outbox() = DurableEventOutbox(object : EventOutboxStorage {
        private var value: String? = null
        override fun load() = value
        override fun save(value: String) { this.value = value }
    })

    private class CapturingChannel(private val reply: String) : RealtimeRequestChannel {
        val sent = mutableListOf<String>()
        override suspend fun exchange(payload: String): String {
            sent += payload
            return reply
        }
        override suspend fun close() = Unit
    }
}
