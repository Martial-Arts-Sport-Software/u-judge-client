package org.mass.connection

import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mass.combat.KerugiAvailability
import org.mass.combat.KerugiCommandController
import org.mass.combat.KerugiCommandOutcome
import org.mass.combat.KerugiParticipant
import org.mass.combat.KerugiTarget
import org.mass.contract.ServerContractFixtures
import org.mass.session.SessionFixtures
import org.mass.session.SessionStateStore
import org.mass.session.SessionSync
import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The paired session wiring of `PairedServerSession` against a scripted ADR-009 server: app kill with pending taps,
 * reconnect order handshake → clock_sync → outbox replay → resync, controls only after the resync, seat retention.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KerugiBoutReconnectTest {
    @Test
    fun pendingTapsSurviveAnAppKillAndAreReplayedBeforeTheResync() = runTest {
        val storage = SharedStorage()
        val beforeKill = DurableEventOutbox(storage)
        beforeKill.enqueueNew("tap-1", 0, "2026-11-08T10:15:30.120Z", SessionFixtures.SESSION_ID, scorePayload("BLUE"))
        beforeKill.enqueueNew("tap-2", 0, "2026-11-08T10:15:30.400Z", SessionFixtures.SESSION_ID, scorePayload("RED"))
        val server = FakeServer()
        val app = PairedApp(this, DurableEventOutbox(storage), server)

        app.resume()
        runCurrent()

        val socket = server.sockets.single()
        assertEquals(
            listOf("handshake", "clock_sync", "kerugi_score_command", "kerugi_score_command", "resync_request", "heartbeat"),
            socket.types()
        )
        assertEquals(listOf("tap-1", "tap-2"), socket.received.filter { it.type() == "kerugi_score_command" }.map {
            it.getValue("eventId").jsonPrimitive.content
        })
        assertEquals(JsonPrimitive(null as Long?), socket.received.single { it.type() == "resync_request" }["cursor"])
        assertEquals(emptyList(), app.outbox.pendingEvents())
        assertEquals(KerugiAvailability.AVAILABLE, app.controller.availability)
        assertEquals(SessionFixtures.seat, app.session.snapshot?.seat)
        app.stop()
    }

    @Test
    fun controlsStayDisabledUntilTheResyncOnTheNewSocketAndTheSeatIsKept() = runTest {
        val server = FakeServer()
        val app = PairedApp(this, DurableEventOutbox(SharedStorage()), server)
        app.resume()
        runCurrent()
        assertEquals(KerugiAvailability.AVAILABLE, app.controller.availability)

        server.sockets.single().drop()
        runCurrent()
        // Controls are disabled from the moment the socket is lost, before the heartbeat notices it.
        assertEquals(KerugiAvailability.SYNCHRONIZING, app.controller.availability)

        // The heartbeat notices the lost socket and the lifecycle reconnects with a second one.
        server.holdResync = true
        advanceTimeBy(HEARTBEAT_INTERVAL_MILLIS + 1)
        val reconnected = server.sockets.last()
        assertEquals(2, server.sockets.size)
        assertEquals(KerugiAvailability.SYNCHRONIZING, app.controller.availability)
        assertEquals(KerugiCommandOutcome.Unavailable, app.controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD))

        server.releaseResync()
        runCurrent()

        assertEquals(JsonPrimitive(124), reconnected.received.single { it.type() == "resync_request" }["cursor"])
        assertEquals(KerugiAvailability.AVAILABLE, app.controller.availability)
        val tap = assertIs<KerugiCommandOutcome.Pending>(app.controller.submit(KerugiParticipant.RED, KerugiTarget.BODY))
        runCurrent()
        assertEquals(SessionFixtures.JUDGE_ID, reconnected.lastCommand().getValue("judgeId").jsonPrimitive.content)
        assertEquals(KerugiCommandOutcome.Accepted(tap.event.eventId), app.controller.latestOutcome)
        app.stop()
    }

    @Test
    fun pushesUpdateTheBoutAndAPausedStateDisablesScoring() = runTest {
        val server = FakeServer()
        val app = PairedApp(this, DurableEventOutbox(SharedStorage()), server)
        app.resume()
        runCurrent()

        val socket = server.sockets.single()
        socket.push(ServerContractFixtures.message("kerugi_score_updated").replace("\"cursor\":124", "\"cursor\":126"))
        socket.push(
            """{"type":"session_state_updated","sessionId":"${SessionFixtures.SESSION_ID}","cursor":127,"state":"paused","result":null}"""
        )
        runCurrent()

        assertEquals(12, app.session.snapshot?.score?.blue)
        assertEquals(KerugiAvailability.NOT_RUNNING, app.controller.availability)
        app.stop()
    }

    private fun scorePayload(competitor: String) = """{"type":"kerugi_score_command",""" +
        """"competitionId":"${SessionFixtures.COMPETITION_ID}","peerId":"${SessionFixtures.PEER_ID}",""" +
        """"courtId":"${SessionFixtures.COURT_ID}","bracketId":"${SessionFixtures.BRACKET_ID}",""" +
        """"sessionId":"${SessionFixtures.SESSION_ID}","judgeId":"${SessionFixtures.JUDGE_ID}",""" +
        """"deviceId":"${SessionFixtures.DEVICE_ID}","competitor":"$competitor","area":"HEAD"}"""

    /** The client objects as `PairedServerSession.build` wires them. */
    private class PairedApp(test: TestScope, val outbox: DurableEventOutbox, server: FakeServer) {
        private val scope = test.backgroundScope
        val connection = ConnectionStateStore()
        val session = SessionStateStore()
        private val sessionSync = SessionSync(scope, session)
        lateinit var controller: KerugiCommandController
        private val commandClient = RealtimeCommandClient(outbox) { controller.recordTerminalOutcome(it) }
        private val dispatcher = RealtimeCommandDispatcher(scope, outbox, commandClient) { test.currentTime }
        private val lifecycle = RealtimeReconnectLifecycle(
            reconnectCredentialRepository = ReconnectCredentialRepository(object : ReconnectCredentialStorage {
                override fun load() = "credential-contract"
                override fun save(credential: String) = Unit
                override fun clear() = Unit
            }),
            realtimeClient = RealtimeClient(Url("https://court.local:8443"), RealtimeSocketOpener { server.open() }),
            heartbeatLifecycle = HeartbeatLifecycle(scope, heartbeatIntervalMillis = HEARTBEAT_INTERVAL_MILLIS),
            outboxReplay = DueRealtimeOutboxReplay(outbox, commandClient) { test.currentTime },
            channelFactory = { socket ->
                RoutingRealtimeChannel(socket, scope, sessionSync::onPush, onClosed = { sessionSync.detach(it) })
            },
            onChannelReady = { channel ->
                sessionSync.attach(channel)
                dispatcher.activate(channel)
            },
            onChannelClosed = {
                dispatcher.deactivate()
                sessionSync.detach()
            },
            retryDelayMillis = { 1_000 }
        )

        init {
            controller = KerugiCommandController(connection, session, outbox, { test.currentTime }) { dispatcher }
        }

        suspend fun resume() {
            connection.dispatch(ConnectionEvent.ResumePairedServer(SessionFixtures.DEVICE_ID))
            lifecycle.resume(connection)
        }

        suspend fun stop() = lifecycle.stop()
    }

    /** Answers the client as server ADR-009 describes; every socket keeps the device's seat. */
    private class FakeServer {
        val sockets = mutableListOf<ServerSocket>()
        var holdResync = false
        private val heldResyncs = mutableListOf<Pair<ServerSocket, String>>()

        fun open(): RealtimeSocket = ServerSocket(this).also { sockets += it }

        fun releaseResync() {
            holdResync = false
            heldResyncs.forEach { (socket, requestId) -> socket.push(resyncResponse(requestId)) }
            heldResyncs.clear()
        }

        fun reply(socket: ServerSocket, body: JsonObject): List<String> = when (body.type()) {
            "handshake" -> listOf("""{"type":"handshake_accepted"}""")
            "clock_sync" -> {
                val sent = body.getValue("clientSendTimestamp").jsonPrimitive.content
                listOf(
                    """{"type":"clock_sync_response","clientSendTimestamp":"$sent",""" +
                        """"serverReceiveTimestamp":"$sent","serverSendTimestamp":"$sent"}"""
                )
            }
            "heartbeat" -> listOf("""{"type":"heartbeat_ack"}""")
            "kerugi_score_command", "judge_attention_command" ->
                listOf("""{"type":"command_ack","eventId":"${body.getValue("eventId").jsonPrimitive.content}"}""")
            "resync_request" -> {
                val requestId = body.getValue("requestId").jsonPrimitive.content
                if (holdResync) {
                    heldResyncs += socket to requestId
                    emptyList()
                } else {
                    listOf(resyncResponse(requestId))
                }
            }
            else -> listOf("""{"type":"command_rejected","code":"unsupported_command","eventId":null}""")
        }

        private fun resyncResponse(requestId: String) = JsonObject(
            Json.parseToJsonElement(ServerContractFixtures.message("resync_response")).jsonObject +
                ("requestId" to JsonPrimitive(requestId))
        ).toString()
    }

    private class ServerSocket(private val server: FakeServer) : RealtimeSocket {
        val received = mutableListOf<JsonObject>()
        private val incoming = Channel<String>(Channel.UNLIMITED)
        private var dropped = false

        fun types() = received.map { it.type() }

        fun lastCommand() = received.last { it.type() == "kerugi_score_command" }

        fun push(frame: String) {
            incoming.trySend(frame)
        }

        fun drop() {
            dropped = true
            incoming.close(IllegalStateException("connection dropped"))
        }

        override suspend fun send(payload: String) {
            check(!dropped) { "connection dropped" }
            val body = Json.parseToJsonElement(payload).jsonObject
            received += body
            server.reply(this, body).forEach(::push)
        }

        override suspend fun receive(): String = incoming.receive()

        override suspend fun close() {
            incoming.close()
        }
    }

    private class SharedStorage : EventOutboxStorage {
        private var value: String? = null
        override fun load(): String? = value
        override fun save(value: String) {
            this.value = value
        }
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MILLIS = 15_000L

        fun JsonObject.type() = getValue("type").jsonPrimitive.content
    }
}
