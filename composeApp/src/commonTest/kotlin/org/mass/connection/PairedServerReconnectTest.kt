package org.mass.connection

import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Restore after an app restart, retry while the server is down, and the terminal outcomes of a paired session. */
@OptIn(ExperimentalCoroutinesApi::class)
class PairedServerReconnectTest {
    @Test
    fun restoredSessionRetriesWithBackoffUntilTheServerIsBack() = runTest {
        var attempts = 0
        val serverBack = RespondingSocket(
            """{"type":"handshake_accepted"}""",
            """{"type":"clock_sync_response","clientSendTimestamp":"1970-01-01T00:00:01Z","serverReceiveTimestamp":"1970-01-01T00:00:01Z","serverSendTimestamp":"1970-01-01T00:00:01Z"}""",
            """{"type":"heartbeat_ack"}"""
        )
        val storage = MemoryStorage(credential = "credential-1")
        val lifecycle = lifecycle(storage, RealtimeSocketOpener {
            attempts++
            if (attempts < 3) error("server is restarting") else serverBack
        })
        val store = ConnectionStateStore()
        store.dispatch(ConnectionEvent.ResumePairedServer("device-1"))

        backgroundScope.launch { lifecycle.resume(store) }
        runCurrent()
        assertEquals(1, attempts)
        advanceTimeBy(1_001)
        assertEquals(2, attempts)
        advanceTimeBy(2_001)

        assertEquals(3, attempts)
        assertEquals(ConnectionState.ConnectedIdle("device-1", clockOffsetMillis = 0), store.state)
        assertEquals(
            """{"type":"handshake","protocolVersion":"1.0","reconnectCredential":"credential-1"}""",
            serverBack.sentPayloads.first()
        )
        lifecycle.stop()
    }

    @Test
    fun revokedCredentialEndsTheSessionAndForgetsThePairing() = runTest {
        val storage = MemoryStorage(credential = "credential-1", pairedServer = PAIRED.toJson())
        val lifecycle = lifecycle(storage, RealtimeSocketOpener {
            RespondingSocket("""{"type":"handshake_rejected","code":"invalid_reconnect_credential"}""")
        })
        val store = ConnectionStateStore()
        store.dispatch(ConnectionEvent.ResumePairedServer("device-1"))

        lifecycle.resume(store)

        assertEquals(ConnectionState.Rejected(PAIRED_SERVER_KEY, ConnectionFailure.DeviceRevoked), store.state)
        assertNull(storage.credential)
        assertNull(storage.pairedServer)
    }

    @Test
    fun changedServerIdentityEndsTheSessionWithoutForgettingIt() = runTest {
        val storage = MemoryStorage(credential = "credential-1", pairedServer = PAIRED.toJson())
        val trust = ServerTrust(expectedPin = PAIRED.spkiSha256)
        val lifecycle = lifecycle(storage, RealtimeSocketOpener {
            trust.accept(ByteArray(32) { 9 })
            error("TLS handshake refused by the pin")
        }, isServerIdentityChanged = { trust.identityChanged })
        val store = ConnectionStateStore()
        store.dispatch(ConnectionEvent.ResumePairedServer("device-1"))

        lifecycle.resume(store)

        assertEquals(ConnectionState.Rejected(PAIRED_SERVER_KEY, ConnectionFailure.ServerIdentityChanged), store.state)
        assertEquals("credential-1", storage.credential)
        assertEquals(PAIRED, PairedServer.fromJson(requireNotNull(storage.pairedServer)))
    }

    @Test
    fun stopEndsTheRetryLoop() = runTest {
        var attempts = 0
        val lifecycle = lifecycle(MemoryStorage(credential = "credential-1"), RealtimeSocketOpener {
            attempts++
            error("server is down")
        })
        val store = ConnectionStateStore()
        store.dispatch(ConnectionEvent.ResumePairedServer("device-1"))
        backgroundScope.launch { lifecycle.resume(store) }
        runCurrent()

        lifecycle.stop()
        advanceTimeBy(120_000)

        assertEquals(1, attempts)
    }

    @Test
    fun retryDelayGrowsAndIsBounded() {
        assertEquals(
            listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L),
            (0..6).map(RealtimeReconnectLifecycle::defaultRetryDelayMillis)
        )
    }

    @Test
    fun pairedServerRoundTripsAndForgetClearsEverything() {
        val storage = MemoryStorage(credential = "credential-1")
        val repository = ReconnectCredentialRepository(storage)
        repository.savePairedServer(PAIRED)
        repository.savePairingDeliveryProof("proof-1")

        assertEquals(PAIRED, repository.loadPairedServer())
        repository.forget()

        assertNull(repository.load())
        assertNull(repository.loadPairedServer())
        assertNull(repository.loadPairingDeliveryProof())
    }

    @Test
    fun resumeAndLoseAccessTransitions() {
        val store = ConnectionStateStore()
        store.dispatch(ConnectionEvent.ResumePairedServer("device-1"))
        assertEquals(ConnectionState.Reconnecting("device-1", 0, ConnectionFailure.RealtimeUnavailable), store.state)

        store.dispatch(ConnectionEvent.LoseAccess(ConnectionFailure.DeviceRevoked))
        assertEquals(ConnectionState.Rejected(PAIRED_SERVER_KEY, ConnectionFailure.DeviceRevoked), store.state)

        val offline = ConnectionStateStore()
        offline.dispatch(ConnectionEvent.LoseAccess(ConnectionFailure.DeviceRevoked))
        assertEquals(ConnectionState.Offline, offline.state)
    }

    private fun kotlinx.coroutines.test.TestScope.lifecycle(
        storage: MemoryStorage,
        opener: RealtimeSocketOpener,
        isServerIdentityChanged: () -> Boolean = { false }
    ) = RealtimeReconnectLifecycle(
        reconnectCredentialRepository = ReconnectCredentialRepository(storage),
        realtimeClient = RealtimeClient(
            endpoint = Url("https://court.local:8443"),
            socketOpener = opener,
            clockSyncClient = ClockSyncClient { 1_000L }
        ),
        heartbeatLifecycle = HeartbeatLifecycle(backgroundScope, heartbeatIntervalMillis = Long.MAX_VALUE),
        isServerIdentityChanged = isServerIdentityChanged
    )

    private class MemoryStorage(var credential: String? = null, var pairedServer: String? = null) : ReconnectCredentialStorage {
        private var proof: String? = null
        override fun load() = credential
        override fun save(credential: String) { this.credential = credential }
        override fun clear() { credential = null }
        override fun loadPairingDeliveryProof() = proof
        override fun savePairingDeliveryProof(proof: String) { this.proof = proof }
        override fun clearPairingDeliveryProof() { proof = null }
        override fun loadPairedServer() = pairedServer
        override fun savePairedServer(server: String) { pairedServer = server }
        override fun clearPairedServer() { pairedServer = null }
    }

    private class RespondingSocket(vararg responses: String) : RealtimeSocket {
        val sentPayloads = mutableListOf<String>()
        private val responses = responses.toMutableList()
        override suspend fun send(payload: String) { sentPayloads += payload }
        override suspend fun receive(): String = responses.removeFirst()
        override suspend fun close() = Unit
    }

    private companion object {
        val PAIRED = PairedServer("https://10.0.2.2:8443", "device-1", ByteArray(32) { it.toByte() })
    }
}
