package org.mass.contract

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mass.State
import org.mass.combat.TanbonAction
import org.mass.combat.TanbonCommandController
import org.mass.combat.TanbonCommandOutcome
import org.mass.connection.ConnectionEvent
import org.mass.connection.ConnectionStateStore
import org.mass.connection.RealtimeCommandClient
import org.mass.connection.RealtimeCommandResult
import org.mass.connection.RealtimeRequestChannel
import org.mass.enums.Disciplines
import org.mass.rating.TechnicalSendController
import org.mass.rating.TechnicalSendOutcome
import org.mass.rating.TechnicalVector
import org.mass.session.KerugiScore
import org.mass.session.PushOutcome
import org.mass.session.ResyncReply
import org.mass.session.SessionFixtures
import org.mass.session.SessionMessages
import org.mass.session.SessionPhase
import org.mass.session.SessionPush
import org.mass.session.SessionStateStore
import org.mass.session.TechnicalResult
import org.mass.session.TechnicalScore
import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Client side of the Tanbon and technical performance contract (server ADR-011): every server fixture at
 * `contract/server-ref` decodes and the client encodes `tanbon_score_command` and `technical_score_send_command` exactly
 * as the request fixtures.
 */
class DisciplinesContractTest {
    @Test
    fun theMetadataFixtureOffersEveryCapabilityTheClientRequires() {
        val capabilities = fixture("metadata_response").getValue("capabilities").jsonObject.keys
        assertTrue(capabilities.containsAll(State.REQUIRED_SERVER_CAPABILITIES), "$capabilities")
        assertEquals(setOf("kerugi_bout", "tanbon_bout", "technical_performance"), State.REQUIRED_SERVER_CAPABILITIES)
    }

    @Test
    fun theTanbonSnapshotAndScorePushDecodeWithTheBoutShape() {
        val push = assertIs<SessionPush.Snapshot>(SessionMessages.decodePush(fixture("tanbon_session_snapshot")))
        val session = requireNotNull(push.session)
        assertEquals(Disciplines.TANBON, session.discipline)
        assertEquals(KerugiScore(0, 0, 0, 0, emptyList(), null), session.score)
        assertNull(push.technical)

        assertEquals(
            SessionPush.ScoreUpdated(SessionFixtures.SESSION_ID, 124, KerugiScore(2, 0, 0, 0, emptyList(), null)),
            SessionMessages.decodePush(fixture("tanbon_score_updated"))
        )
    }

    @Test
    fun theCrossButtonIsEncodedExactlyAsTheTanbonFixture() = runTest {
        val command = fixture("tanbon_score_command")
        val outbox = outbox()
        val tanbon = SessionFixtures.synchronizedStore(SessionFixtures.snapshot(discipline = "tanbon"))
        val controller = TanbonCommandController(
            connected(), tanbon, outbox,
            nowMillis = { Instant.parse(command.text("clientTimestamp")).toEpochMilliseconds() },
            eventId = { command.text("eventId") }
        )
        val event = assertIs<TanbonCommandOutcome.Pending>(controller.submit(TanbonAction.Cross)).event
        val channel = CapturingChannel("""{"type":"command_ack","eventId":"${command.text("eventId")}"}""")

        assertEquals(RealtimeCommandResult.Accepted(command.text("eventId")), RealtimeCommandClient(outbox).send(event, channel, 0))
        assertEquals(command, json(channel.sent.single()))
    }

    @Test
    fun theTechnicalSnapshotDecodesEveryField() {
        val push = assertIs<SessionPush.Snapshot>(SessionMessages.decodePush(fixture("technical_session_snapshot")))
        assertNull(push.session)
        assertEquals(205, push.cursor)
        assertEquals(SessionFixtures.technical(), push.technical)
        assertEquals(Disciplines.HOSINSOOL, push.technical?.discipline?.discipline)

        val resync = JsonObject(
            fixture("resync_response") + ("session" to fixture("technical_session_snapshot").getValue("session"))
        )
        val reply = assertIs<ResyncReply.Response>(SessionMessages.decodeResyncReply(resync))
        assertEquals(SessionFixtures.technical(), reply.technical)
        assertNull(reply.session)
    }

    @Test
    fun theTechnicalScorePushAndStateUpdateApplyToTheHeldPerformance() {
        val update = assertIs<SessionPush.TechnicalScoreUpdated>(SessionMessages.decodePush(fixture("technical_score_updated")))
        assertEquals(TechnicalScore(listOf(1, 2), TechnicalResult(62, 62, 0, 2, "mean")), update.score)

        val store = SessionFixtures.technicalStore()
        assertEquals(PushOutcome.APPLIED, store.applyPush(update))
        assertEquals(update.score, store.technical?.score)
        assertEquals(
            PushOutcome.APPLIED,
            store.applyPush(SessionPush.StateUpdated(SessionFixtures.PERFORMANCE_ID, 212, SessionPhase.COMPLETED, null))
        )
        assertEquals(SessionPhase.COMPLETED, store.technical?.state)
        assertEquals(
            PushOutcome.SESSION_MISMATCH,
            store.applyPush(SessionPush.ScoreUpdated(SessionFixtures.PERFORMANCE_ID, 213, KerugiScore(1, 0, 0, 0, emptyList(), null)))
        )
    }

    @Test
    fun anUnknownDisciplineIsHeldWithoutControlsAndWithoutAResyncLoop() {
        val unknown = JsonObject(
            fixture("technical_session_snapshot") + ("session" to JsonObject(
                fixture("technical_session_snapshot").getValue("session").jsonObject + ("discipline" to JsonPrimitive("archery"))
            ))
        )
        val push = assertIs<SessionPush.Snapshot>(SessionMessages.decodePush(unknown))
        assertNull(push.session)
        assertNull(push.technical)
        assertEquals(SessionFixtures.PERFORMANCE_ID, push.otherSessionId)

        val store = SessionStateStore().apply { applyResync(1, null) }
        store.applyPush(push)
        assertEquals(
            PushOutcome.APPLIED,
            store.applyPush(SessionPush.StateUpdated(SessionFixtures.PERFORMANCE_ID, 300, SessionPhase.RUNNING, null))
        )
    }

    @Test
    fun theSendIsEncodedExactlyAsTheFixtureAndTheRejectionFixtureIsTerminal() = runTest {
        val command = fixture("technical_score_send_command")
        val outbox = outbox()
        // The fixture carries sequence 7: seven earlier commands of this device.
        repeat(7) {
            outbox.enqueueNew("earlier-$it", 0, "2026-11-08T12:00:00Z", "older", "{}")
            outbox.acknowledge("earlier-$it")
        }
        val controller = TechnicalSendController(
            connected(), SessionFixtures.technicalStore(), outbox,
            nowMillis = { Instant.parse(command.text("clientTimestamp")).toEpochMilliseconds() },
            eventId = { command.text("eventId") }
        )
        val vector = TechnicalVector(listOf(8, 7, 9, 6), listOf(7, 8, 8, 9), 3)
        val event = assertIs<TechnicalSendOutcome.Pending>(controller.send(Disciplines.HOSINSOOL, vector)).event
        val channel = CapturingChannel("""{"type":"command_ack","eventId":"${command.text("eventId")}"}""")

        assertEquals(RealtimeCommandClient(outbox).send(event, channel, 0), RealtimeCommandResult.Accepted(command.text("eventId")))
        assertEquals(command, json(channel.sent.single()))

        val rejected = fixture("technical_command_rejected")
        val second = outbox.enqueueNew(rejected.text("eventId"), 0, "2026-11-08T12:40:02Z", SessionFixtures.PERFORMANCE_ID, "{\"type\":\"technical_score_send_command\"}")
        assertEquals(
            RealtimeCommandResult.Rejected(rejected.text("eventId"), "total_mismatch"),
            RealtimeCommandClient(outbox).send(second, CapturingChannel(ServerContractFixtures.message("technical_command_rejected")), 0)
        )
        assertEquals("total_mismatch", outbox.rejectedEvents().single().reason)
    }

    private fun connected() = ConnectionStateStore().apply {
        dispatch(ConnectionEvent.ResumePairedServer(SessionFixtures.DEVICE_ID))
        dispatch(ConnectionEvent.AcceptPairing(SessionFixtures.DEVICE_ID, clockOffsetMillis = 0))
    }

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
