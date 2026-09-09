package org.mass.connection

import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InitialRealtimeLifecycleTest {
    @Test
    fun acceptedPairingStartsAuthenticatedSocketClockSyncAndHeartbeat() = runTest {
        val socket = RespondingSocket(
            """{"type":"handshake_accepted"}""",
            """{"type":"clock_sync_response","clientSendTimestamp":"1970-01-01T00:00:01Z","serverReceiveTimestamp":"1970-01-01T00:00:01.100Z","serverSendTimestamp":"1970-01-01T00:00:01.120Z"}""",
            """{"type":"heartbeat_ack"}"""
        )
        val clockSyncTimes = mutableListOf(1_000L, 1_100L)
        val lifecycle = InitialRealtimeLifecycle(
            reconnectCredentialRepository = credentialRepository("credential-1"),
            realtimeClient = RealtimeClient(
                endpoint = Url("http://court.local"),
                socketOpener = RealtimeSocketOpener { socket },
                clockSyncClient = ClockSyncClient { clockSyncTimes.removeFirst() }
            ),
            reconnectLifecycle = RealtimeReconnectLifecycle(
                reconnectCredentialRepository = credentialRepository("credential-1"),
                realtimeClient = RealtimeClient(
                    endpoint = Url("http://court.local"),
                    socketOpener = RealtimeSocketOpener { error("reconnect must not start") }
                ),
                heartbeatLifecycle = HeartbeatLifecycle(
                    scope = backgroundScope,
                    heartbeatIntervalMillis = Long.MAX_VALUE,
                    heartbeatTimeoutMillis = 20
                )
            )
        )
        val store = pairingPendingStore()

        lifecycle.start("device-1", store)
        runCurrent()

        assertEquals(ConnectionState.ConnectedIdle("device-1", clockOffsetMillis = 60), store.state)
        assertEquals(
            listOf(
                "{\"type\":\"handshake\",\"protocolVersion\":\"1.0\",\"reconnectCredential\":\"credential-1\"}",
                "{\"type\":\"clock_sync\",\"clientSendTimestamp\":\"1970-01-01T00:00:01Z\"}",
                "{\"type\":\"heartbeat\"}"
            ),
            socket.sentPayloads
        )

        lifecycle.stop()
        assertEquals(1, socket.closeCount)
    }

    @Test
    fun missingCredentialRejectsAcceptedPairingWithoutOpeningAnOnlineSocket() = runTest {
        var openCount = 0
        var transportClosed = false
        val lifecycle = InitialRealtimeLifecycle(
            reconnectCredentialRepository = credentialRepository(null),
            realtimeClient = RealtimeClient(
                endpoint = Url("http://court.local"),
                socketOpener = RealtimeSocketOpener {
                    openCount++
                    error("a missing credential must not open a socket")
                }
            ),
            reconnectLifecycle = RealtimeReconnectLifecycle(
                reconnectCredentialRepository = credentialRepository(null),
                realtimeClient = RealtimeClient(
                    endpoint = Url("http://court.local"),
                    socketOpener = RealtimeSocketOpener { error("reconnect must not start") }
                ),
                heartbeatLifecycle = HeartbeatLifecycle(backgroundScope)
            ),
            closeTransport = { transportClosed = true }
        )
        val store = pairingPendingStore()

        lifecycle.start("device-1", store)

        assertEquals(
            ConnectionState.Rejected(
                "court-1",
                ConnectionFailure.RealtimeHandshakeRejected("missing_reconnect_credential")
            ),
            store.state
        )
        assertFalse(store.isPaired)
        assertEquals(0, openCount)
        assertTrue(transportClosed)
    }

    private fun pairingPendingStore(): ConnectionStateStore = ConnectionStateStore().also { store ->
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(ConnectionEvent.ValidateMetadata(metadata()))
        store.dispatch(ConnectionEvent.RequestPairing)
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
