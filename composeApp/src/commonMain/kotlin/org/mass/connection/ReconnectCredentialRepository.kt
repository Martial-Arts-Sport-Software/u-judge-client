package org.mass.connection

/** Platform boundary for the server-issued reconnect secret. */
interface ReconnectCredentialStorage {
    fun load(): String?

    fun save(credential: String)

    fun clear()

    fun loadPairingDeliveryProof(): String? = null

    fun savePairingDeliveryProof(proof: String) = Unit

    fun clearPairingDeliveryProof() = Unit
}

class ReconnectCredentialRepository(private val storage: ReconnectCredentialStorage) {
    fun load(): String? = storage.load()

    fun save(credential: String) {
        storage.save(credential)
    }

    fun clear() {
        storage.clear()
    }

    fun loadPairingDeliveryProof(): String? = storage.loadPairingDeliveryProof()

    fun savePairingDeliveryProof(proof: String) {
        storage.savePairingDeliveryProof(proof)
    }

    fun clearPairingDeliveryProof() {
        storage.clearPairingDeliveryProof()
    }
}
