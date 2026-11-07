package org.mass.contract

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.mass.State
import org.mass.combat.KerugiCommandController
import org.mass.combat.KerugiParticipant
import org.mass.combat.KerugiTarget
import org.mass.connection.ConnectionEvent
import org.mass.connection.ConnectionFailure
import org.mass.connection.ConnectionState
import org.mass.connection.ConnectionStateStore
import org.mass.connection.RealtimeCommandClient
import org.mass.connection.RealtimeCommandResult
import org.mass.connection.RealtimeRequestChannel
import org.mass.connection.ServerMetadata
import org.mass.session.BoutResult
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
import kotlin.time.Instant

/** Client side of the Kerugi bout contract of server ADR-009, checked against the ADR's own messages. */
class KerugiBoutContractTest {
    private val examples = KerugiBoutContractExamples

    @Test
    fun anI2aClientRequiresTheKerugiBoutCapability() {
        fun validate(capabilities: Set<String>) = ConnectionStateStore(
            requiredCapabilities = State.REQUIRED_SERVER_CAPABILITIES
        ).apply {
            dispatch(ConnectionEvent.StartDiscovery)
            dispatch(ConnectionEvent.SelectServer("court"))
            dispatch(ConnectionEvent.ValidateMetadata(metadata(capabilities)))
        }.state

        assertIs<ConnectionState.MetadataValidated>(validate(setOf("metadata", "kerugi_bout")))
        assertEquals(
            ConnectionFailure.MissingRequiredCapabilities(setOf("kerugi_bout")),
            assertIs<ConnectionState.Rejected>(validate(setOf("metadata"))).failure
        )
    }

    @Test
    fun sessionSnapshotDecodesEveryField() {
        val push = assertIs<SessionPush.Snapshot>(SessionMessages.decodePush(json(examples.sessionSnapshot)))

        assertEquals(118, push.cursor)
        assertEquals(SessionFixtures.snapshot(), push.session)
        assertEquals(
            SessionPush.Snapshot(57, null),
            SessionMessages.decodePush(json(KerugiBoutContractExamples.SESSION_SNAPSHOT_IDLE))
        )
    }

    @Test
    fun resyncRequestMatchesTheContract() {
        assertEquals(
            json(KerugiBoutContractExamples.RESYNC_REQUEST),
            json(SessionMessages.resyncRequest("1e2d3c4b-5a69-4788-9a0b-c1d2e3f4a5b6", 112))
        )
        assertEquals(
            json("""{"type":"resync_request","requestId":"r","cursor":null}"""),
            json(SessionMessages.resyncRequest("r", null))
        )
    }

    @Test
    fun resyncRepliesDecode() {
        assertEquals(
            ResyncReply.Response("1e2d3c4b-5a69-4788-9a0b-c1d2e3f4a5b6", 118, SessionFixtures.snapshot()),
            SessionMessages.decodeResyncReply(json(examples.resyncResponse))
        )
        assertEquals(
            ResyncReply.Response("1e2d3c4b-5a69-4788-9a0b-c1d2e3f4a5b6", 57, null),
            SessionMessages.decodeResyncReply(json(KerugiBoutContractExamples.RESYNC_RESPONSE_IDLE))
        )
        assertEquals(
            ResyncReply.Rejected("1e2d3c4b-5a69-4788-9a0b-c1d2e3f4a5b6", "journal_unavailable"),
            SessionMessages.decodeResyncReply(json(KerugiBoutContractExamples.RESYNC_REJECTED))
        )
        assertEquals(
            ResyncReply.Rejected(null, "invalid_resync_request"),
            SessionMessages.decodeResyncReply(
                json("""{"type":"resync_rejected","requestId":null,"code":"invalid_resync_request"}""")
            )
        )
    }

    @Test
    fun stateAndScorePushesDecode() {
        assertEquals(
            SessionPush.StateUpdated(
                SessionFixtures.SESSION_ID, 119, SessionPhase.COMPLETED, BoutResult(Competitor.BLUE, "final_score")
            ),
            SessionMessages.decodePush(json(examples.sessionStateUpdated))
        )
        assertEquals(
            SessionPush.ScoreUpdated(
                SessionFixtures.SESSION_ID,
                124,
                KerugiScore(
                    blue = 5,
                    red = 1,
                    blueGamjeom = 0,
                    redGamjeom = 10,
                    warnings = listOf(ScoreWarning("disqualification_pending", Competitor.RED, 10)),
                    disqualified = null
                )
            ),
            SessionMessages.decodePush(json(examples.kerugiScoreUpdated))
        )
    }

    @Test
    fun serverMessagesMayGainFieldsButNotLoseListedOnes() {
        val extended = json(examples.sessionStateUpdated).let { JsonObject(it + ("timer" to json("""{"a":1}"""))) }
        assertIs<SessionPush.StateUpdated>(SessionMessages.decodePush(extended))

        val missing = JsonObject(json(examples.sessionStateUpdated) - "result")
        assertEquals(null, SessionMessages.decodePush(missing))
        val stringCursor = JsonObject(json(examples.sessionStateUpdated) + ("cursor" to Json.parseToJsonElement("\"119\"")))
        assertEquals(null, SessionMessages.decodePush(stringCursor))
        assertEquals(null, SessionMessages.decodePush(json("""{"type":"kerugi_timer_updated","cursor":1}""")))
    }

    @Test
    fun scoreTapIsEncodedExactlyAsTheContractCommandAndItsAckSettlesTheOutbox() = runTest {
        val outbox = outbox()
        val event = assertIs<org.mass.combat.KerugiCommandOutcome.Pending>(
            controller(outbox, "2026-11-08T10:15:30.120Z", "6f1c8a52-3c1e-4c55-9a3e-2b9f0d6e4a10")
                .submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        ).event
        val channel = CapturingChannel(KerugiBoutContractExamples.COMMAND_ACK)

        assertEquals(
            RealtimeCommandResult.Accepted("6f1c8a52-3c1e-4c55-9a3e-2b9f0d6e4a10"),
            RealtimeCommandClient(outbox).send(event, channel, nowMillis = 0)
        )
        assertEquals(json(examples.kerugiScoreCommand), json(channel.sent.single()))
        assertEquals(emptyList(), outbox.pendingEvents())
    }

    @Test
    fun attentionSignalIsEncodedExactlyAsTheContractCommand() = runTest {
        val outbox = outbox()
        // The attention example carries sequence 1: one earlier tap of this device.
        outbox.enqueueNew("earlier-tap", 0, "2026-11-08T10:15:30.120Z", SessionFixtures.SESSION_ID, "{}")
        outbox.acknowledge("earlier-tap")
        val event = assertIs<org.mass.combat.KerugiCommandOutcome.Pending>(
            controller(outbox, "2026-11-08T10:15:41.500Z", "8b3e5d7f-1a2c-4e4b-9d6f-3c5e7a9b1d20").signalAttention()
        ).event
        val channel = CapturingChannel("""{"type":"command_ack","eventId":"8b3e5d7f-1a2c-4e4b-9d6f-3c5e7a9b1d20"}""")

        RealtimeCommandClient(outbox).send(event, channel, nowMillis = 0)

        assertEquals(json(examples.judgeAttentionCommand), json(channel.sent.single()))
    }

    @Test
    fun contractRejectionIsTerminal() = runTest {
        val outbox = outbox()
        val event = assertIs<org.mass.combat.KerugiCommandOutcome.Pending>(
            controller(outbox, "2026-11-08T10:15:30.120Z", "6f1c8a52-3c1e-4c55-9a3e-2b9f0d6e4a10")
                .submit(KerugiParticipant.BLUE, KerugiTarget.HEAD)
        ).event

        assertEquals(
            RealtimeCommandResult.Rejected("6f1c8a52-3c1e-4c55-9a3e-2b9f0d6e4a10", "session_closed"),
            RealtimeCommandClient(outbox).send(event, CapturingChannel(KerugiBoutContractExamples.COMMAND_REJECTED), 0)
        )
        assertEquals("session_closed", outbox.rejectedEvents().single().reason)
    }

    private fun controller(outbox: DurableEventOutbox, tapTime: String, eventId: String) = KerugiCommandController(
        connection = ConnectionStateStore().apply {
            dispatch(ConnectionEvent.ResumePairedServer(SessionFixtures.DEVICE_ID))
            dispatch(ConnectionEvent.AcceptPairing(SessionFixtures.DEVICE_ID, clockOffsetMillis = 0))
        },
        session = SessionFixtures.synchronizedStore(),
        outbox = outbox,
        nowMillis = { Instant.parse(tapTime).toEpochMilliseconds() },
        eventId = { eventId }
    )

    private fun metadata(capabilities: Set<String>) = ServerMetadata(
        protocolMajor = 1,
        protocolMinor = 0,
        capabilities = capabilities,
        peerId = SessionFixtures.PEER_ID,
        courtId = "court-local",
        serverName = "U'Judge Server",
        pairingPolicy = "operator-approval",
        serverTimeMillis = 0
    )

    private fun json(value: String): JsonObject = Json.parseToJsonElement(value).jsonObject

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
