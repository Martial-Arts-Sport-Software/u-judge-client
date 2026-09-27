package org.mass.connection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Restores the authenticated realtime channel after its heartbeat reports a transport failure or the app restarts.
 * Transport failures are retried with bounded exponential backoff; a revoked credential or a changed server identity ends
 * the session, and any other handshake rejection stops retrying with its typed reason.
 */
class RealtimeReconnectLifecycle(
    private val reconnectCredentialRepository: ReconnectCredentialRepository,
    private val realtimeClient: RealtimeClient,
    private val heartbeatLifecycle: HeartbeatLifecycle,
    private val outboxReplay: RealtimeOutboxReplay = RealtimeOutboxReplay { true },
    private val onChannelReady: (RealtimeRequestChannel) -> Unit = {},
    private val onChannelClosed: () -> Unit = {},
    private val isServerIdentityChanged: () -> Boolean = { false },
    private val retryDelayMillis: (attempt: Int) -> Long = ::defaultRetryDelayMillis
) {
    private var stopped = false

    /** Reconnects a session restored from secure storage after the app restarted. */
    suspend fun resume(store: ConnectionStateStore) {
        stopped = false
        reconnect(store)
    }

    suspend fun start(socket: RealtimeSocket, store: ConnectionStateStore) {
        stopped = false
        val replayFailure = try {
            if (outboxReplay.replay(socket)) null else ConnectionFailure.RealtimeResponseInvalid
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            ConnectionFailure.RealtimeUnavailable
        }
        if (replayFailure != null) {
            socket.close()
            store.dispatch(ConnectionEvent.HeartbeatFailed(replayFailure))
            reconnect(store)
            return
        }
        val channel = SerializedRealtimeRequestChannel(socket)
        onChannelReady(channel)
        heartbeatLifecycle.start(channel, store) {
            onChannelClosed()
            reconnect(store)
        }
    }

    suspend fun stop() {
        stopped = true
        heartbeatLifecycle.stop()
        onChannelClosed()
    }

    private suspend fun reconnect(store: ConnectionStateStore) {
        var attempt = 0
        while (!stopped) {
            val state = store.state as? ConnectionState.Reconnecting ?: return
            val credential = reconnectCredentialRepository.load()
            if (credential == null) {
                store.dispatch(
                    ConnectionEvent.ReconnectFailed(
                        ConnectionFailure.RealtimeHandshakeRejected("missing_reconnect_credential")
                    )
                )
                return
            }
            when (
                val result = realtimeClient.connect(
                    RealtimeHandshakeRequest(state.deviceId, credential),
                    store
                )
            ) {
                is RealtimeHandshakeResult.Accepted -> {
                    start(result.socket, store)
                    return
                }
                is RealtimeHandshakeResult.Rejected -> {
                    if (result.code == REVOKED_CREDENTIAL_CODE) {
                        reconnectCredentialRepository.forget()
                        store.dispatch(ConnectionEvent.LoseAccess(ConnectionFailure.DeviceRevoked))
                    }
                    return
                }
                else -> {
                    if (isServerIdentityChanged()) {
                        store.dispatch(ConnectionEvent.LoseAccess(ConnectionFailure.ServerIdentityChanged))
                        return
                    }
                    delay(retryDelayMillis(attempt++))
                }
            }
        }
    }

    companion object {
        /** Server handshake code for an unknown or revoked reconnect credential. */
        const val REVOKED_CREDENTIAL_CODE = "invalid_reconnect_credential"

        fun defaultRetryDelayMillis(attempt: Int): Long = minOf(1_000L shl minOf(attempt, 5), 30_000L)
    }
}
