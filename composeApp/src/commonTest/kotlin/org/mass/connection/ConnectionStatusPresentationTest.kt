package org.mass.connection

import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionStatusPresentationTest {
    @Test
    fun reconnectingHeartbeatFailuresShowReconnectStatusAndTheirTypedReason() {
        val cases = listOf(
            ConnectionFailure.HeartbeatUnavailable to "connection_error_heartbeat_unavailable",
            ConnectionFailure.HeartbeatResponseInvalid to "connection_error_heartbeat_response_invalid",
            ConnectionFailure.HeartbeatRejected("session_expired") to "connection_error_heartbeat_rejected"
        )

        cases.forEach { (failure, expectedReasonKey) ->
            assertEquals(
                ConnectionStatusPresentation(
                    statusKey = "connection_reconnecting",
                    reasonKey = expectedReasonKey
                ),
                connectionStatusPresentation(
                    ConnectionState.Reconnecting(
                        deviceId = "device-1",
                        clockOffsetMillis = 60,
                        failure = failure
                    )
                )
            )
        }
    }
}
