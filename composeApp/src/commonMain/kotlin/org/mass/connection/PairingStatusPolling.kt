package org.mass.connection

import kotlinx.coroutines.delay

class PairingStatusPolling(
    private val statusClient: PairingStatusClient,
    private val pollingIntervalMillis: Long = 1_000
) {
    suspend fun poll(requestId: String, onStatus: (PairingStatusFetchResult) -> Unit) {
        while (true) {
            val result = statusClient.fetch(requestId)
            onStatus(result)
            if (result !is PairingStatusFetchResult.Success || result.status !is PairingStatus.Pending) return
            delay(pollingIntervalMillis)
        }
    }
}
