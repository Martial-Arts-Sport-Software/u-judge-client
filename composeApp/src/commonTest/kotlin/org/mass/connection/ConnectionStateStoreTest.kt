package org.mass.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionStateStoreTest {
    @Test
    fun compatibleMetadataMakesSelectedServerReadyForPairing() {
        val store = ConnectionStateStore(
            protocolMajor = 1,
            requiredCapabilities = setOf("pairing", "realtime")
        )

        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(
            ConnectionEvent.ValidateMetadata(
                ServerMetadata(
                    protocolMajor = 1,
                    protocolMinor = 0,
                    capabilities = setOf("pairing", "realtime"),
                    peerId = "peer-1",
                    courtId = "court-1",
                    serverName = "Court 1",
                    pairingPolicy = "operatorApproval",
                    serverTimeMillis = 1_000
                )
            )
        )

        assertEquals(
            ConnectionState.MetadataValidated(
                serverKey = "court-1",
                metadata = ServerMetadata(
                    protocolMajor = 1,
                    protocolMinor = 0,
                    capabilities = setOf("pairing", "realtime"),
                    peerId = "peer-1",
                    courtId = "court-1",
                    serverName = "Court 1",
                    pairingPolicy = "operatorApproval",
                    serverTimeMillis = 1_000
                )
            ),
            store.state
        )
        assertFalse(store.isPaired)
    }

    @Test
    fun incompatibleMetadataRejectsSelectedServerWithoutGrantingPairedAccess() {
        val store = ConnectionStateStore(protocolMajor = 1)

        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(
            ConnectionEvent.ValidateMetadata(
                ServerMetadata(
                    protocolMajor = 2,
                    protocolMinor = 0,
                    capabilities = emptySet(),
                    peerId = "peer-1",
                    courtId = "court-1",
                    serverName = "Court 1",
                    pairingPolicy = "operatorApproval",
                    serverTimeMillis = 1_000
                )
            )
        )

        assertEquals(
            ConnectionState.Rejected(
                serverKey = "court-1",
                failure = ConnectionFailure.IncompatibleProtocolVersion(
                    expectedMajor = 1,
                    actualMajor = 2
                )
            ),
            store.state
        )
        assertFalse(store.isPaired)
    }

    @Test
    fun metadataMissingRequiredCapabilitiesRejectsSelectedServer() {
        val store = ConnectionStateStore(requiredCapabilities = setOf("pairing", "realtime"))

        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(
            ConnectionEvent.ValidateMetadata(
                ServerMetadata(
                    protocolMajor = 1,
                    protocolMinor = 0,
                    capabilities = setOf("pairing"),
                    peerId = "peer-1",
                    courtId = "court-1",
                    serverName = "Court 1",
                    pairingPolicy = "operatorApproval",
                    serverTimeMillis = 1_000
                )
            )
        )

        assertEquals(
            ConnectionState.Rejected(
                serverKey = "court-1",
                failure = ConnectionFailure.MissingRequiredCapabilities(setOf("realtime"))
            ),
            store.state
        )
        assertFalse(store.isPaired)
    }

    @Test
    fun selectingServerDoesNotGrantPairedAccess() {
        val store = ConnectionStateStore()

        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))

        assertEquals(ConnectionState.ServerSelected("court-1"), store.state)
        assertFalse(store.isPaired)
    }

    @Test
    fun acceptedPairingGrantsPairedAccess() {
        val store = ConnectionStateStore()

        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(
            ConnectionEvent.ValidateMetadata(
                ServerMetadata(
                    protocolMajor = 1,
                    protocolMinor = 0,
                    capabilities = emptySet(),
                    peerId = "peer-1",
                    courtId = "court-1",
                    serverName = "Court 1",
                    pairingPolicy = "operatorApproval",
                    serverTimeMillis = 1_000
                )
            )
        )
        store.dispatch(ConnectionEvent.RequestPairing)
        store.dispatch(ConnectionEvent.AcceptPairing("device-1"))

        assertEquals(ConnectionState.ConnectedIdle("device-1"), store.state)
        assertTrue(store.isPaired)
    }

    @Test
    fun pairingFailureRejectsOnlyMetadataValidatedServer() {
        val store = ConnectionStateStore()

        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(
            ConnectionEvent.ValidateMetadata(
                ServerMetadata(
                    protocolMajor = 1,
                    protocolMinor = 0,
                    capabilities = emptySet(),
                    peerId = "peer-1",
                    courtId = "court-1",
                    serverName = "Court 1",
                    pairingPolicy = "operatorApproval",
                    serverTimeMillis = 1_000
                )
            )
        )
        store.dispatch(ConnectionEvent.RejectPairing(ConnectionFailure.PairingResponseInvalid))

        assertEquals(
            ConnectionState.Rejected("court-1", ConnectionFailure.PairingResponseInvalid),
            store.state
        )
        assertFalse(store.isPaired)
    }

    @Test
    fun invalidTransitionsLeaveConnectionStateUnchanged() {
        val store = ConnectionStateStore()

        store.dispatch(ConnectionEvent.AcceptPairing("device-1"))

        assertEquals(ConnectionState.Offline, store.state)
        assertFalse(store.isPaired)
    }

    @Test
    fun switchingOfflineClearsAnySelectedServerState() {
        val store = ConnectionStateStore()

        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(ConnectionEvent.UseOffline)

        assertEquals(ConnectionState.Offline, store.state)
        assertFalse(store.isPaired)
    }
}
