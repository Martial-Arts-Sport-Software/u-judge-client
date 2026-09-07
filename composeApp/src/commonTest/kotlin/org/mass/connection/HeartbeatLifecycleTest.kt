package org.mass.connection

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HeartbeatLifecycleTest {
    @Test
    fun inactiveConnectionDoesNotStartHeartbeatTraffic() = runTest {
        val socket = RespondingSocket("""{"type":"heartbeat_ack"}""")
        val lifecycle = HeartbeatLifecycle(
            scope = backgroundScope,
            heartbeatIntervalMillis = 100,
            heartbeatTimeoutMillis = 20
        )

        lifecycle.start(socket, ConnectionStateStore())
        runCurrent()

        assertEquals(emptyList(), socket.sentPayloads)
        assertEquals(0, socket.closeCount)
    }

    @Test
    fun activeConnectionSendsHeartbeatsAtTheConfiguredInterval() = runTest {
        val socket = RespondingSocket("""{"type":"heartbeat_ack"}""")
        val lifecycle = HeartbeatLifecycle(
            scope = backgroundScope,
            heartbeatIntervalMillis = 100,
            heartbeatTimeoutMillis = 20
        )

        lifecycle.start(socket, connectedStore())
        runCurrent()
        advanceTimeBy(200)
        runCurrent()

        assertEquals(
            listOf("""{"type":"heartbeat"}""", """{"type":"heartbeat"}""", """{"type":"heartbeat"}"""),
            socket.sentPayloads
        )
    }

    @Test
    fun rejectedHeartbeatEndsTheActiveConnectionAndClosesItsSocket() = runTest {
        val socket = RespondingSocket("""{"type":"heartbeat_rejected","code":"session_expired"}""")
        val store = connectedStore()
        val lifecycle = HeartbeatLifecycle(
            scope = backgroundScope,
            heartbeatIntervalMillis = 100,
            heartbeatTimeoutMillis = 20
        )

        lifecycle.start(socket, store)
        runCurrent()

        assertEquals(
            ConnectionState.Reconnecting(
                deviceId = "device-1",
                clockOffsetMillis = 60,
                failure = ConnectionFailure.HeartbeatRejected("session_expired")
            ),
            store.state
        )
        assertEquals(1, socket.closeCount)
    }

    @Test
    fun missingHeartbeatAcknowledgementEndsTheActiveConnectionAfterTimeout() = runTest {
        val socket = UnresponsiveSocket()
        val store = connectedStore()
        val lifecycle = HeartbeatLifecycle(
            scope = backgroundScope,
            heartbeatIntervalMillis = 100,
            heartbeatTimeoutMillis = 20
        )

        lifecycle.start(socket, store)
        runCurrent()
        advanceTimeBy(20)
        runCurrent()

        assertEquals(
            ConnectionState.Reconnecting(
                deviceId = "device-1",
                clockOffsetMillis = 60,
                failure = ConnectionFailure.HeartbeatUnavailable
            ),
            store.state
        )
        assertEquals(1, socket.closeCount)
    }

    @Test
    fun stoppingLifecycleCancelsPendingHeartbeatAndClosesSocketWithoutFailure() = runTest {
        val socket = UnresponsiveSocket()
        val store = connectedStore()
        val lifecycle = HeartbeatLifecycle(
            scope = backgroundScope,
            heartbeatIntervalMillis = 100,
            heartbeatTimeoutMillis = 20
        )

        lifecycle.start(socket, store)
        runCurrent()
        lifecycle.stop()

        assertEquals(ConnectionState.ConnectedIdle("device-1", clockOffsetMillis = 60), store.state)
        assertEquals(1, socket.closeCount)
    }

    private fun connectedStore(): ConnectionStateStore = ConnectionStateStore().also { store ->
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(ConnectionEvent.ValidateMetadata(metadata()))
        store.dispatch(ConnectionEvent.RequestPairing)
        store.dispatch(ConnectionEvent.AcceptPairing("device-1", clockOffsetMillis = 60))
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

    private class RespondingSocket(private val response: String) : RealtimeSocket {
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

    private class UnresponsiveSocket : RealtimeSocket {
        var closeCount = 0

        override suspend fun send(payload: String) = Unit

        override suspend fun receive(): String {
            awaitCancellation()
        }

        override suspend fun close() {
            closeCount++
        }
    }
}
