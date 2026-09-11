package org.mass.connection

import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeReconnectLifecycleTest {
    @Test
    fun heartbeatFailureReconnectsWithStoredCredentialAndSynchronizesClock() = runTest {
        val failedSocket = RespondingSocket("""{"type":"heartbeat_rejected","code":"session_expired"}""")
        val reconnectedSocket = RespondingSocket(
            """{"type":"handshake_accepted"}""",
            """{"type":"clock_sync_response","clientSendTimestamp":"1970-01-01T00:00:01Z","serverReceiveTimestamp":"1970-01-01T00:00:01.100Z","serverSendTimestamp":"1970-01-01T00:00:01.120Z"}""",
            """{"type":"heartbeat_ack"}"""
        )
        val clockSyncTimes = mutableListOf(1_000L, 1_100L)
        val replayedSockets = mutableListOf<RealtimeSocket>()
        val reconnectClient = RealtimeClient(
            endpoint = Url("http://court.local"),
            socketOpener = RealtimeSocketOpener { reconnectedSocket },
            clockSyncClient = ClockSyncClient { clockSyncTimes.removeFirst() }
        )
        val lifecycle = RealtimeReconnectLifecycle(
            reconnectCredentialRepository = credentialRepository("credential-1"),
            realtimeClient = reconnectClient,
            heartbeatLifecycle = HeartbeatLifecycle(
                scope = backgroundScope,
                heartbeatIntervalMillis = Long.MAX_VALUE,
                heartbeatTimeoutMillis = 20
            ),
            outboxReplay = RealtimeOutboxReplay { socket ->
                replayedSockets += socket
                true
            }
        )
        val store = connectedStore()

        lifecycle.start(failedSocket, store)
        runCurrent()

        assertEquals(
            ConnectionState.ConnectedIdle(deviceId = "device-1", clockOffsetMillis = 60),
            store.state
        )
        assertEquals(1, failedSocket.closeCount)
        assertEquals(listOf<RealtimeSocket>(failedSocket, reconnectedSocket), replayedSockets)
        assertEquals(
            listOf(
                "{\"type\":\"handshake\",\"protocolVersion\":\"1.0\",\"reconnectCredential\":\"credential-1\"}",
                "{\"type\":\"clock_sync\",\"clientSendTimestamp\":\"1970-01-01T00:00:01Z\"}",
                "{\"type\":\"heartbeat\"}"
            ),
            reconnectedSocket.sentPayloads
        )
        lifecycle.stop()
        assertEquals(1, reconnectedSocket.closeCount)
    }

    @Test
    fun missingReconnectCredentialKeepsTypedReconnectFailureWithoutOpeningSocket() = runTest {
        val failedSocket = RespondingSocket("""{"type":"heartbeat_rejected","code":"session_expired"}""")
        var openCount = 0
        val lifecycle = RealtimeReconnectLifecycle(
            reconnectCredentialRepository = credentialRepository(null),
            realtimeClient = RealtimeClient(
                endpoint = Url("http://court.local"),
                socketOpener = RealtimeSocketOpener {
                    openCount++
                    error("a missing credential must not open a socket")
                }
            ),
            heartbeatLifecycle = HeartbeatLifecycle(
                scope = backgroundScope,
                heartbeatIntervalMillis = Long.MAX_VALUE,
                heartbeatTimeoutMillis = 20
            )
        )
        val store = connectedStore()

        lifecycle.start(failedSocket, store)
        runCurrent()

        assertEquals(
            ConnectionState.Reconnecting(
                deviceId = "device-1",
                clockOffsetMillis = 20,
                failure = ConnectionFailure.RealtimeHandshakeRejected("missing_reconnect_credential")
            ),
            store.state
        )
        assertFalse(store.isPaired)
        assertEquals(0, openCount)
    }

    @Test
    fun invalidReplayClosesTheSocketAndRetriesTheAuthenticatedConnection() = runTest {
        val initialSocket = RespondingSocket()
        val retriedSocket = RespondingSocket("""{"type":"handshake_rejected","code":"credential_revoked"}""")
        val lifecycle = RealtimeReconnectLifecycle(
            reconnectCredentialRepository = credentialRepository("credential-1"),
            realtimeClient = RealtimeClient(
                endpoint = Url("http://court.local"),
                socketOpener = RealtimeSocketOpener { retriedSocket }
            ),
            heartbeatLifecycle = HeartbeatLifecycle(backgroundScope),
            outboxReplay = RealtimeOutboxReplay { false }
        )
        val store = connectedStore()

        lifecycle.start(initialSocket, store)

        assertEquals(1, initialSocket.closeCount)
        assertEquals(
            ConnectionState.Reconnecting(
                deviceId = "device-1",
                clockOffsetMillis = 20,
                failure = ConnectionFailure.RealtimeHandshakeRejected("credential_revoked")
            ),
            store.state
        )
    }

    @Test
    fun rejectedReconnectKeepsTypedReconnectFailureWithoutGrantingPairedAccess() = runTest {
        val failedSocket = RespondingSocket("""{"type":"heartbeat_rejected","code":"session_expired"}""")
        val rejectedSocket = RespondingSocket("""{"type":"handshake_rejected","code":"credential_revoked"}""")
        val lifecycle = RealtimeReconnectLifecycle(
            reconnectCredentialRepository = credentialRepository("credential-1"),
            realtimeClient = RealtimeClient(
                endpoint = Url("http://court.local"),
                socketOpener = RealtimeSocketOpener { rejectedSocket }
            ),
            heartbeatLifecycle = HeartbeatLifecycle(
                scope = backgroundScope,
                heartbeatIntervalMillis = Long.MAX_VALUE,
                heartbeatTimeoutMillis = 20
            )
        )
        val store = connectedStore()

        lifecycle.start(failedSocket, store)
        runCurrent()

        assertEquals(
            ConnectionState.Reconnecting(
                deviceId = "device-1",
                clockOffsetMillis = 20,
                failure = ConnectionFailure.RealtimeHandshakeRejected("credential_revoked")
            ),
            store.state
        )
        assertFalse(store.isPaired)
        assertEquals(1, rejectedSocket.closeCount)
    }

    private fun connectedStore(): ConnectionStateStore = ConnectionStateStore().also { store ->
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(ConnectionEvent.ValidateMetadata(metadata()))
        store.dispatch(ConnectionEvent.RequestPairing)
        store.dispatch(ConnectionEvent.AcceptPairing("device-1", clockOffsetMillis = 20))
    }

    private fun credentialRepository(credential: String?): ReconnectCredentialRepository =
        ReconnectCredentialRepository(object : ReconnectCredentialStorage {
            override fun load(): String? = credential

            override fun save(credential: String) = Unit

            override fun clear() = Unit
        })

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

    private class RespondingSocket(vararg responses: String) : RealtimeSocket {
        val sentPayloads = mutableListOf<String>()
        var closeCount = 0
        private val responses = responses.toMutableList()

        override suspend fun send(payload: String) {
            sentPayloads += payload
        }

        override suspend fun receive(): String = responses.removeFirst()

        override suspend fun close() {
            closeCount++
        }
    }
}
