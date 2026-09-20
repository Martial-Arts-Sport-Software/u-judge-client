package org.mass.connection

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PairingStatusFlowTest {
    @Test
    fun acceptedHttpStatusKeepsTheClientPendingForAuthenticatedRealtimeHandshake() = runTest {
        val store = pairingPendingStore()
        val flow = PairingStatusFlow(
            PairingStatusPolling(fetch = { PairingStatusResult.Accepted("device-1", "credential-1") }),
            repository()
        )

        assertEquals(PairingStatusResult.Accepted("device-1", "credential-1"), flow.awaitStatus(PairingResult.Pending("request-1", "proof-1"), store))
        assertEquals(ConnectionState.PairingPending("court-1"), store.state)
    }

    @Test
    fun rejectedHttpStatusStoresTheOperatorRejection() = runTest {
        val store = pairingPendingStore()
        val flow = PairingStatusFlow(
            PairingStatusPolling(fetch = { PairingStatusResult.Rejected("device-1", "operator_rejected") }),
            repository()
        )

        flow.awaitStatus(PairingResult.Pending("request-1", "proof-1"), store)

        assertEquals(
            ConnectionState.Rejected(
                "court-1",
                ConnectionFailure.PairingStatusRejected("operator_rejected")
            ),
            store.state
        )
    }

    @Test
    fun unavailableHttpStatusStoresAPairingStatusFailure() = runTest {
        val store = pairingPendingStore()
        val flow = PairingStatusFlow(
            PairingStatusPolling(fetch = { PairingStatusResult.Unavailable }),
            repository()
        )

        flow.awaitStatus(PairingResult.Pending("request-1", "proof-1"), store)

        assertEquals(
            ConnectionState.Rejected("court-1", ConnectionFailure.PairingStatusUnavailable),
            store.state
        )
    }

    private fun pairingPendingStore() = ConnectionStateStore().also { store ->
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(ConnectionEvent.ValidateMetadata(validMetadata()))
        store.dispatch(ConnectionEvent.RequestPairing)
    }

    private fun repository() = ReconnectCredentialRepository(object : ReconnectCredentialStorage {
        private var credential: String? = null
        override fun load(): String? = credential
        override fun save(credential: String) { this.credential = credential }
        override fun clear() { credential = null }
    })

    private fun validMetadata() = ServerMetadata(
        protocolMajor = 1,
        protocolMinor = 0,
        capabilities = emptySet(),
        peerId = "peer-1",
        courtId = "court-1",
        serverName = "Court 1",
        pairingPolicy = "operator_approval",
        serverTimeMillis = 0
    )
}
