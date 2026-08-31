package org.mass.connection

interface PairingIdentityStorage {
    fun get(key: String): String?

    fun put(key: String, value: String)
}

class PairingIdentityRepository(
    private val storage: PairingIdentityStorage,
    private val createDeviceId: () -> String
) {
    fun deviceId(): String {
        val storedId = storage.get(DEVICE_ID_KEY)
        if (!storedId.isNullOrBlank()) return storedId

        return createDeviceId().also { storage.put(DEVICE_ID_KEY, it) }
    }

    fun surname(): String = storage.get(SURNAME_KEY).orEmpty()

    fun saveSurname(surname: String) {
        storage.put(SURNAME_KEY, surname.trim())
    }

    private companion object {
        const val DEVICE_ID_KEY = "pairing_device_id"
        const val SURNAME_KEY = "judge_surname"
    }
}
