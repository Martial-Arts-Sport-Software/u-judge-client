package org.mass.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReconnectCredentialRepositoryTest {
    @Test
    fun restoresCredentialSavedByAnotherRepositoryInstance() {
        val storage = FakeStorage()
        ReconnectCredentialRepository(storage).save("credential-1")

        assertEquals("credential-1", ReconnectCredentialRepository(storage).load())
    }

    @Test
    fun clearsPreviouslySavedCredential() {
        val storage = FakeStorage()
        val repository = ReconnectCredentialRepository(storage)
        repository.save("credential-1")

        repository.clear()

        assertNull(repository.load())
    }

    private class FakeStorage : ReconnectCredentialStorage {
        private var credential: String? = null

        override fun load(): String? = credential

        override fun save(credential: String) {
            this.credential = credential
        }

        override fun clear() {
            credential = null
        }
    }
}
