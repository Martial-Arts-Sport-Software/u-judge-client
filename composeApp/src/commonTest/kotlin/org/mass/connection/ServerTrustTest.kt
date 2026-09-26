package org.mass.connection

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerTrustTest {
    @Test
    fun verificationCodeMatchesTheServerVectors() {
        // Same vectors as PeerCertificateTest in u-judge-server.
        assertEquals("066051", verificationCode(byteArrayOf(0x01, 0x02, 0x03) + ByteArray(29)))
        assertEquals("777215", verificationCode(ByteArray(32) { 0xff.toByte() }))
        assertEquals("000000", verificationCode(ByteArray(32)))
        assertEquals("066 051", formatVerificationCode("066051"))
    }

    @Test
    fun firstContactCapturesTheKeyAndLaterConnectionsMustPresentIt() {
        val trust = ServerTrust()
        assertNull(trust.verificationCode)

        assertTrue(trust.accept(pin(1)))
        assertTrue(trust.accept(pin(1)))
        assertFalse(trust.accept(pin(2)))

        assertContentEquals(pin(1), trust.pinnedSpki)
        assertTrue(trust.identityChanged)
    }

    @Test
    fun storedPinRejectsAnyOtherKey() {
        val trust = ServerTrust(expectedPin = pin(3))

        assertFalse(trust.accept(pin(4)))
        assertTrue(trust.identityChanged)
        assertTrue(ServerTrust(expectedPin = pin(3)).accept(pin(3)))
    }

    private fun pin(seed: Int) = ByteArray(32) { seed.toByte() }
}
