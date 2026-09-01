package org.mass.connection

/** Coordinates the sequential metadata validation and pairing request lifecycle. */
class PairingFlow(
    private val metadataClient: ServerMetadataClient,
    private val pairingClient: PairingClient
) {
    suspend fun connect(request: PairingRequest, store: ConnectionStateStore): PairingResult? {
        metadataClient.fetchInto(store)
        return if (store.state is ConnectionState.MetadataValidated) {
            pairingClient.request(request, store)
        } else {
            null
        }
    }
}
