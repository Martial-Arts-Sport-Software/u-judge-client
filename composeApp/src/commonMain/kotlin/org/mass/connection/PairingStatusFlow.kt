package org.mass.connection

class PairingStatusFlow(
    private val polling: PairingStatusPolling,
    private val reconnectCredentialRepository: ReconnectCredentialRepository
) {
    suspend fun awaitStatus(pending: PairingResult.Pending, store: ConnectionStateStore): PairingStatusResult {
        val result = polling.awaitTerminalStatus(pending.requestId)
        when (result) {
            is PairingStatusResult.Accepted -> try {
                reconnectCredentialRepository.save(result.reconnectCredential)
                reconnectCredentialRepository.clearPairingDeliveryProof()
            } catch (_: Exception) {
                return PairingStatusResult.Unavailable
            }
            is PairingStatusResult.Rejected -> store.dispatch(
                ConnectionEvent.RejectRealtime(ConnectionFailure.PairingStatusRejected(result.code))
            )
            PairingStatusResult.NotFound,
            PairingStatusResult.MalformedResponse -> store.dispatch(
                ConnectionEvent.RejectRealtime(ConnectionFailure.PairingStatusResponseInvalid)
            )
            PairingStatusResult.Unavailable -> store.dispatch(
                ConnectionEvent.RejectRealtime(ConnectionFailure.PairingStatusUnavailable)
            )
            is PairingStatusResult.Pending -> error("Polling must return a terminal pairing status")
        }
        return result
    }
}
