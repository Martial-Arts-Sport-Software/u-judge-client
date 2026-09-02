package org.mass.connection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The connection lifecycle; only a successful pairing enables online features. */
sealed interface ConnectionState {
    data object Offline : ConnectionState
    data object Discovering : ConnectionState
    data class ServerSelected(val serverKey: String) : ConnectionState
    data class MetadataValidated(
        val serverKey: String,
        val metadata: ServerMetadata
    ) : ConnectionState
    data class PairingPending(val serverKey: String) : ConnectionState
    data class ConnectedIdle(
        val deviceId: String,
        val clockOffsetMillis: Long = 0
    ) : ConnectionState
    data class Rejected(
        val serverKey: String,
        val failure: ConnectionFailure
    ) : ConnectionState
}

/** Metadata returned by the server before a device can request pairing. */
data class ServerMetadata(
    val protocolMajor: Int,
    val protocolMinor: Int,
    val capabilities: Set<String>,
    val peerId: String,
    val courtId: String,
    val serverName: String,
    val pairingPolicy: String,
    val serverTimeMillis: Long
)

/** Failures that keep a selected server from reaching the pairing flow. */
sealed interface ConnectionFailure {
    val localizationKey: String

    data class IncompatibleProtocolVersion(
        val expectedMajor: Int,
        val actualMajor: Int
    ) : ConnectionFailure {
        override val localizationKey = "connection_error_protocol_version"
    }

    data class MissingRequiredCapabilities(
        val missingCapabilities: Set<String>
    ) : ConnectionFailure {
        override val localizationKey = "connection_error_missing_capabilities"
    }

    data object MetadataUnavailable : ConnectionFailure {
        override val localizationKey = "connection_error_metadata_unavailable"
    }

    data object PairingResponseInvalid : ConnectionFailure {
        override val localizationKey = "connection_error_pairing_response_invalid"
    }

    data object PairingUnavailable : ConnectionFailure {
        override val localizationKey = "connection_error_pairing_unavailable"
    }

    data class RealtimeHandshakeRejected(val code: String) : ConnectionFailure {
        override val localizationKey = "connection_error_realtime_handshake_rejected"
    }

    data object RealtimeResponseInvalid : ConnectionFailure {
        override val localizationKey = "connection_error_realtime_response_invalid"
    }

    data object RealtimeUnavailable : ConnectionFailure {
        override val localizationKey = "connection_error_realtime_unavailable"
    }

    data class ClockSyncRejected(val code: String) : ConnectionFailure {
        override val localizationKey = "connection_error_clock_sync_rejected"
    }

    data object ClockSyncResponseInvalid : ConnectionFailure {
        override val localizationKey = "connection_error_clock_sync_response_invalid"
    }
}

sealed interface ConnectionEvent {
    data object UseOffline : ConnectionEvent
    data object StartDiscovery : ConnectionEvent
    data class SelectServer(val serverKey: String) : ConnectionEvent
    data class ValidateMetadata(val metadata: ServerMetadata) : ConnectionEvent
    data class RejectMetadata(val failure: ConnectionFailure) : ConnectionEvent
    data object RequestPairing : ConnectionEvent
    data class RejectPairing(val failure: ConnectionFailure) : ConnectionEvent
    data class RejectRealtime(val failure: ConnectionFailure) : ConnectionEvent
    data class AcceptPairing(
        val deviceId: String,
        val clockOffsetMillis: Long = 0
    ) : ConnectionEvent
}

class ConnectionStateStore(
    private val protocolMajor: Int = 1,
    private val requiredCapabilities: Set<String> = emptySet()
) {
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
            is ConnectionEvent.ValidateMetadata -> when (val currentState = state) {
                is ConnectionState.ServerSelected -> validateMetadata(currentState.serverKey, event.metadata)
                else -> state
            }
            is ConnectionEvent.RejectMetadata -> when (val currentState = state) {
                is ConnectionState.ServerSelected -> ConnectionState.Rejected(currentState.serverKey, event.failure)
                else -> state
            }
            ConnectionEvent.RequestPairing -> when (val currentState = state) {
                is ConnectionState.MetadataValidated -> ConnectionState.PairingPending(currentState.serverKey)
                else -> state
            }
            is ConnectionEvent.RejectPairing -> when (val currentState = state) {
                is ConnectionState.MetadataValidated -> ConnectionState.Rejected(currentState.serverKey, event.failure)
                else -> state
            }
            is ConnectionEvent.RejectRealtime -> when (val currentState = state) {
                is ConnectionState.PairingPending -> ConnectionState.Rejected(currentState.serverKey, event.failure)
                else -> state
            }
            is ConnectionEvent.AcceptPairing -> when (state) {
                is ConnectionState.PairingPending -> ConnectionState.ConnectedIdle(
                    deviceId = event.deviceId,
                    clockOffsetMillis = event.clockOffsetMillis
                )
                else -> state
            }
        }
    }

    private fun validateMetadata(serverKey: String, metadata: ServerMetadata): ConnectionState = when {
        metadata.protocolMajor != protocolMajor -> ConnectionState.Rejected(
            serverKey = serverKey,
            failure = ConnectionFailure.IncompatibleProtocolVersion(
                expectedMajor = protocolMajor,
                actualMajor = metadata.protocolMajor
            )
        )
        else -> {
            val missingCapabilities = requiredCapabilities - metadata.capabilities
            if (missingCapabilities.isEmpty()) {
                ConnectionState.MetadataValidated(serverKey, metadata)
            } else {
                ConnectionState.Rejected(
                    serverKey = serverKey,
                    failure = ConnectionFailure.MissingRequiredCapabilities(missingCapabilities)
                )
            }
        }
    }
}
