package org.mass.connection

import kotlin.test.Test
import kotlin.test.assertEquals

class PairingIdentityRepositoryTest {
    @Test
    fun createsAndRestoresOneStableDeviceId() {
        val storage = FakeStorage()
        val repository = PairingIdentityRepository(storage) { "device-1" }

        assertEquals("device-1", repository.deviceId())
        assertEquals("device-1", PairingIdentityRepository(storage) { "device-2" }.deviceId())
    }

    @Test
    fun storesTrimmedNonblankJudgeSurname() {
        val repository = PairingIdentityRepository(FakeStorage()) { "device-1" }

        repository.saveSurname(" Ivanov ")

        assertEquals("Ivanov", repository.surname())
    }

    private class FakeStorage : PairingIdentityStorage {
        private val values = mutableMapOf<String, String>()

        override fun get(key: String): String? = values[key]

        override fun put(key: String, value: String) {
            values[key] = value
        }
    }
}
