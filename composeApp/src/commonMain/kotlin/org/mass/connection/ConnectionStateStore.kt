package org.mass.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The connection lifecycle; only a successful pairing enables online features. */
sealed interface ConnectionState {
    data object Offline : ConnectionState
    data object Discovering : ConnectionState
    data class ServerSelected(val serverKey: String) : ConnectionState
    data class PairingPending(val serverKey: String) : ConnectionState
    data class ConnectedIdle(val deviceId: String) : ConnectionState
}

sealed interface ConnectionEvent {
    data object UseOffline : ConnectionEvent
    data object StartDiscovery : ConnectionEvent
    data class SelectServer(val serverKey: String) : ConnectionEvent
    data object RequestPairing : ConnectionEvent
    data class AcceptPairing(val deviceId: String) : ConnectionEvent
}

class ConnectionStateStore {
    var state by mutableStateOf<ConnectionState>(ConnectionState.Offline)
        private set

    val isPaired: Boolean
        get() = state is ConnectionState.ConnectedIdle

    fun dispatch(event: ConnectionEvent) {
        state = when (event) {
            ConnectionEvent.UseOffline -> ConnectionState.Offline
            ConnectionEvent.StartDiscovery -> ConnectionState.Discovering
            is ConnectionEvent.SelectServer -> when (state) {
                ConnectionState.Discovering -> ConnectionState.ServerSelected(event.serverKey)
                else -> state
            }
            ConnectionEvent.RequestPairing -> when (val currentState = state) {
                is ConnectionState.ServerSelected -> ConnectionState.PairingPending(currentState.serverKey)
                else -> state
            }
            is ConnectionEvent.AcceptPairing -> when (state) {
                is ConnectionState.PairingPending -> ConnectionState.ConnectedIdle(event.deviceId)
                else -> state
            }
        }
    }
}
