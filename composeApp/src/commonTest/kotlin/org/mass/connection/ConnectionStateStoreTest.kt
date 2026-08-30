package org.mass.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionStateStoreTest {
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
        store.dispatch(ConnectionEvent.RequestPairing)
        store.dispatch(ConnectionEvent.AcceptPairing("device-1"))

        assertEquals(ConnectionState.ConnectedIdle("device-1"), store.state)
        assertTrue(store.isPaired)
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
