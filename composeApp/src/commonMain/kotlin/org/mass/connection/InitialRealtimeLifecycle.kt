package org.mass.connection

/** Starts the authenticated realtime lifecycle after pairing approval. */
class InitialRealtimeLifecycle(
    private val reconnectCredentialRepository: ReconnectCredentialRepository,
    private val realtimeClient: RealtimeClient,
    private val reconnectLifecycle: RealtimeReconnectLifecycle,
    private val closeTransport: () -> Unit = {}
) {
    suspend fun start(deviceId: String, store: ConnectionStateStore) {
        reconnectLifecycle.stop()
        val credential = reconnectCredentialRepository.load()
        if (credential == null) {
            store.dispatch(
                ConnectionEvent.RejectRealtime(
                    ConnectionFailure.RealtimeHandshakeRejected("missing_reconnect_credential")
                )
            )
            closeTransport()
            return
        }
        when (
            val result = realtimeClient.connect(
                RealtimeHandshakeRequest(deviceId, credential),
                store
            )
        ) {
            is RealtimeHandshakeResult.Accepted -> reconnectLifecycle.start(result.socket, store)
            else -> closeTransport()
        }
    }

    suspend fun stop() {
        reconnectLifecycle.stop()
        closeTransport()
    }
}
