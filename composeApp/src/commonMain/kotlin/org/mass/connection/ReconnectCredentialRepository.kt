package org.mass.connection

/** Platform boundary for the server-issued reconnect secret. */
interface ReconnectCredentialStorage {
    fun load(): String?

    fun save(credential: String)

    fun clear()
}

class ReconnectCredentialRepository(private val storage: ReconnectCredentialStorage) {
    fun load(): String? = storage.load()

    fun save(credential: String) {
        storage.save(credential)
    }

    fun clear() {
        storage.clear()
    }
}
