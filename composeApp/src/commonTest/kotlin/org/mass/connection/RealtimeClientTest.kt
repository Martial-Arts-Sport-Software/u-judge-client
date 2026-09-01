package org.mass.connection

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

class RealtimeClientTest {
    @Test
    fun acceptedHandshakeSendsVersionedCredentialAndGrantsPairedAccess() = runTest {
        val socket = FakeRealtimeSocket("""{"type":"handshake_accepted"}""")
        lateinit var openedEndpoint: Url
        val client = RealtimeClient(
            endpoint = Url("http://court.local"),
            socketOpener = RealtimeSocketOpener { endpoint ->
                openedEndpoint = endpoint
                socket
            }
        )
        val store = pairingPendingStore()

        val result = client.connect(
            RealtimeHandshakeRequest(deviceId = "device-1", reconnectCredential = "credential-1"),
            store
        )

        assertEquals(RealtimeHandshakeResult.Accepted(socket), result)
        assertEquals("ws://court.local/v1/realtime", openedEndpoint.toString())
        assertEquals(
            "{\"type\":\"handshake\",\"protocolVersion\":\"1.0\",\"reconnectCredential\":\"credential-1\"}",
            socket.sentPayloads.single()
        )
        assertEquals(ConnectionState.ConnectedIdle("device-1"), store.state)
    }

    @Test
    fun rejectedHandshakeKeepsPairingPendingServerOutOfOnlineAccess() = runTest {
        val socket = FakeRealtimeSocket("""{"type":"handshake_rejected","code":"invalid_reconnect_credential"}""")
        val client = RealtimeClient(
            endpoint = Url("http://court.local"),
            socketOpener = RealtimeSocketOpener { socket }
        )
        val store = pairingPendingStore()

        val result = client.connect(
            RealtimeHandshakeRequest(deviceId = "device-1", reconnectCredential = "revoked"),
            store
        )

        assertEquals(
            RealtimeHandshakeResult.Rejected("invalid_reconnect_credential"),
            result
        )
        assertEquals(
            ConnectionState.Rejected(
                "court-1",
                ConnectionFailure.RealtimeHandshakeRejected("invalid_reconnect_credential")
            ),
            store.state
        )
        assertFalse(store.isPaired)
        assertEquals(1, socket.closeCount)
    }

    @Test
    fun malformedHandshakeResponseRejectsConnectionWithoutGrantingAccess() = runTest {
        val socket = FakeRealtimeSocket("""{"type":"unknown"}""")
        val client = RealtimeClient(
            endpoint = Url("http://court.local"),
            socketOpener = RealtimeSocketOpener { socket }
        )
        val store = pairingPendingStore()

        assertEquals(
            RealtimeHandshakeResult.InvalidResponse,
            client.connect(RealtimeHandshakeRequest("device-1", "credential-1"), store)
        )
        assertEquals(
            ConnectionState.Rejected("court-1", ConnectionFailure.RealtimeResponseInvalid),
            store.state
        )
        assertFalse(store.isPaired)
    }

    @Test
    fun unavailableSocketRejectsConnectionWithoutGrantingAccess() = runTest {
        val client = RealtimeClient(
            endpoint = Url("http://court.local"),
            socketOpener = RealtimeSocketOpener { error("connection refused") }
        )
        val store = pairingPendingStore()

        assertEquals(
            RealtimeHandshakeResult.Unavailable,
            client.connect(RealtimeHandshakeRequest("device-1", "credential-1"), store)
        )
        assertEquals(
            ConnectionState.Rejected("court-1", ConnectionFailure.RealtimeUnavailable),
            store.state
        )
        assertFalse(store.isPaired)
    }

    private fun pairingPendingStore(): ConnectionStateStore = ConnectionStateStore().also { store ->
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(ConnectionEvent.ValidateMetadata(metadata()))
        store.dispatch(ConnectionEvent.RequestPairing)
    }

    private fun metadata() = ServerMetadata(
        protocolMajor = 1,
        protocolMinor = 0,
        capabilities = emptySet(),
        peerId = "peer-1",
        courtId = "court-1",
        serverName = "Court 1",
        pairingPolicy = "operator-approval",
        serverTimeMillis = 1_000
    )

    private class FakeRealtimeSocket(private val response: String) : RealtimeSocket {
        val sentPayloads = mutableListOf<String>()
        var closeCount = 0

        override suspend fun send(payload: String) {
            sentPayloads += payload
        }

        override suspend fun receive(): String = response

        override suspend fun close() {
            closeCount++
        }
    }
}
