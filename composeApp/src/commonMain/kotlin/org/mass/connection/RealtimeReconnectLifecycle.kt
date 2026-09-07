package org.mass.connection

/** Restores the authenticated realtime channel after its heartbeat reports a transport failure. */
class RealtimeReconnectLifecycle(
    private val reconnectCredentialRepository: ReconnectCredentialRepository,
    private val realtimeClient: RealtimeClient,
    private val heartbeatLifecycle: HeartbeatLifecycle
) {
    suspend fun start(socket: RealtimeSocket, store: ConnectionStateStore) {
        heartbeatLifecycle.start(socket, store) { reconnect(store) }
    }

    suspend fun stop() {
        heartbeatLifecycle.stop()
    }

    private suspend fun reconnect(store: ConnectionStateStore) {
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
            is RealtimeHandshakeResult.Accepted -> start(result.socket, store)
            else -> Unit
        }
    }
}
