package org.mass.contract

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mass.connection.ClockSyncClient
import org.mass.connection.ClockSyncResult
import org.mass.connection.ConnectionEvent
import org.mass.connection.ConnectionStateStore
import org.mass.connection.HeartbeatClient
import org.mass.connection.HeartbeatResult
import org.mass.connection.MetadataFetchResult
import org.mass.connection.PAIRING_DELIVERY_PROOF_HEADER
import org.mass.connection.PairingClient
import org.mass.connection.PairingRequest
import org.mass.connection.PairingResult
import org.mass.connection.PairingStatusClient
import org.mass.connection.PairingStatusResult
import org.mass.connection.RealtimeClient
import org.mass.connection.RealtimeCommandClient
import org.mass.connection.RealtimeCommandResult
import org.mass.connection.RealtimeHandshakeRequest
import org.mass.connection.RealtimeHandshakeResult
import org.mass.connection.RealtimeSocket
import org.mass.connection.RealtimeSocketOpener
import org.mass.connection.ServerMetadataClient
import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import org.mass.transport.OutboxEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

/**
 * Client side of the wire contract (`CLI-103`): requests are encoded exactly as the server fixtures and every server
 * fixture decodes to the expected result. Fixtures come from u-judge-server at `contract/server-ref`.
 */
class ServerContractTest {
    private val endpoint = Url("https://court.local:8443")

    @Test
    fun metadataFixtureValidates() = runTest {
        val result = ServerMetadataClient(respondingWith("metadata_response"), endpoint).fetch()

        val metadata = assertIs<MetadataFetchResult.Success>(result).metadata
        assertEquals(1, metadata.protocolMajor)
        assertEquals(fixture("metadata_response").getValue("peerId").jsonPrimitive.content, metadata.peerId)
    }

    @Test
    fun pairingRequestMatchesTheFixtureAndThePendingResponseDecodes() = runTest {
        lateinit var sent: HttpRequestData
        val client = HttpClient(MockEngine { request ->
            sent = request
            respond(ServerContractFixtures.message("pairing_pending_response"), HttpStatusCode.Accepted)
        })
        val request = fixture("pairing_request")
        val store = ConnectionStateStore().apply {
            dispatch(ConnectionEvent.StartDiscovery)
            dispatch(ConnectionEvent.SelectServer("court"))
        }

        val result = PairingClient(client, endpoint).request(
            PairingRequest(
                deviceId = request.string("deviceId"),
                surname = request.string("surname"),
                platform = request.string("platform"),
                deliveryProof = request.string("deliveryProof")
            ),
            store
        )

        assertEquals(request, Json.parseToJsonElement((sent.body as TextContent).text).jsonObject)
        assertEquals("/v1/pairing-requests", sent.url.encodedPath)
        assertEquals(
            PairingResult.Pending(fixture("pairing_pending_response").string("requestId"), request.string("deliveryProof")),
            result
        )
    }

    @Test
    fun pairingStatusUsesTheFixturePathAndHeaderAndDecodesEveryState() = runTest {
        val statusRequest = fixture("pairing_status_request")
        assertEquals(statusRequest.string("deliveryProofHeader"), PAIRING_DELIVERY_PROOF_HEADER)

        lateinit var sent: HttpRequestData
        val pending = PairingStatusClient(HttpClient(MockEngine { request ->
            sent = request
            respond(ServerContractFixtures.message("pairing_status_pending"), HttpStatusCode.OK)
        }), endpoint).fetch("request-1", "proof-1")

        assertEquals(statusRequest.string("path").replace("{requestId}", "request-1"), sent.url.encodedPath)
        assertEquals("proof-1", sent.headers[statusRequest.string("deliveryProofHeader")])
        assertEquals(PairingStatusResult.Pending("android-contract"), pending)
        assertEquals(
            PairingStatusResult.Accepted("android-contract", "credential-contract"),
            PairingStatusClient(respondingWith("pairing_status_accepted"), endpoint).fetch("request-1", "proof-1")
        )
        assertEquals(
            PairingStatusResult.Rejected("android-contract", "operator_rejected"),
            PairingStatusClient(respondingWith("pairing_status_rejected"), endpoint).fetch("request-1", "proof-1")
        )
    }

    @Test
    fun realtimeHandshakeClockSyncAndHeartbeatMatchTheFixtures() = runTest {
        val socket = ScriptedSocket("handshake_accepted", "clock_sync_response", "heartbeat_ack")
        val clientSendMillis = Instant.parse(fixture("clock_sync_request").string("clientSendTimestamp")).toEpochMilliseconds()
        val times = mutableListOf(clientSendMillis, clientSendMillis + 150)
        val store = ConnectionStateStore().apply {
            dispatch(ConnectionEvent.ResumePairedServer("android-contract"))
        }

        val handshake = RealtimeClient(endpoint, RealtimeSocketOpener { socket }, clockSyncClient = ClockSyncClient { times.removeFirst() })
            .connect(RealtimeHandshakeRequest("android-contract", fixture("handshake_request").string("reconnectCredential")), store)
        val heartbeat = HeartbeatClient().send(socket)

        assertIs<RealtimeHandshakeResult.Accepted>(handshake)
        assertEquals(HeartbeatResult.Acknowledged, heartbeat)
        assertEquals(
            listOf(fixture("handshake_request"), fixture("clock_sync_request"), fixture("heartbeat_request")),
            socket.sent.map { Json.parseToJsonElement(it).jsonObject }
        )
    }

    @Test
    fun handshakeRejectionAndClockSyncFixturesDecode() = runTest {
        val rejected = RealtimeClient(endpoint, RealtimeSocketOpener { ScriptedSocket("handshake_rejected") })
            .connect(RealtimeHandshakeRequest("android-contract", "credential-contract"), ConnectionStateStore())
        assertEquals(RealtimeHandshakeResult.Rejected("invalid_reconnect_credential"), rejected)

        val clientSendMillis = Instant.parse(fixture("clock_sync_request").string("clientSendTimestamp")).toEpochMilliseconds()
        val times = mutableListOf(clientSendMillis, clientSendMillis + 150)
        val sync = ClockSyncClient { times.removeFirst() }.synchronize(ScriptedSocket("clock_sync_response"))
        assertEquals(ClockSyncResult.Synchronized(offsetMillis = 35, roundTripMillis = 130), sync)
    }

    @Test
    fun commandMatchesTheFixtureAndServerOutcomesSettleTheOutbox() = runTest {
        val command = fixture("command_request")
        val event = OutboxEvent(
            eventId = command.string("eventId"),
            clientSequence = command.getValue("sequence").jsonPrimitive.content.toLong(),
            clientTimestampMillis = 0,
            clientTimestamp = command.string("clientTimestamp"),
            sessionId = command.string("sessionId"),
            payload = command.getValue("payload").toString()
        )

        val acknowledged = ScriptedSocket("command_ack")
        assertEquals(
            RealtimeCommandResult.Accepted("event-contract"),
            RealtimeCommandClient(outbox()).send(event, acknowledged, nowMillis = 0)
        )
        assertEquals(command, Json.parseToJsonElement(acknowledged.sent.single()).jsonObject)

        assertEquals(
            RealtimeCommandResult.Rejected("event-contract", "invalid_reconnect_credential"),
            RealtimeCommandClient(outbox()).send(event, ScriptedSocket("command_rejected"), nowMillis = 0)
        )
    }

    private fun respondingWith(name: String) = HttpClient(MockEngine {
        respond(ServerContractFixtures.message(name), HttpStatusCode.OK)
    })

    private fun fixture(name: String): JsonObject = Json.parseToJsonElement(ServerContractFixtures.message(name)).jsonObject

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

    private fun outbox() = DurableEventOutbox(object : EventOutboxStorage {
        private var value: String? = null
        override fun load() = value
        override fun save(value: String) { this.value = value }
    })

    /** Answers each client message with the next server fixture. */
    private class ScriptedSocket(vararg fixtures: String) : RealtimeSocket {
        val sent = mutableListOf<String>()
        private val replies = fixtures.map(ServerContractFixtures::message).toMutableList()
        override suspend fun send(payload: String) { sent += payload }
        override suspend fun receive(): String = replies.removeFirst()
        override suspend fun close() = Unit
    }
}
