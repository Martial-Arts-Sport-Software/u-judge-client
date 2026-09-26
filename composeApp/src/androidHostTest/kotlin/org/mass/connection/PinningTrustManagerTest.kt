package org.mass.connection

import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PinningTrustManagerTest {
    @Test
    fun spkiHashMatchesOpenSsl() {
        // openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256
        assertEquals(
            "416c2c62f6455d6dadc7fb7596751ef12065627db28a6a5268203ca29b44cee0",
            PinningTrustManager.spkiSha256(certificate).toHex()
        )
    }

    @Test
    fun acceptsOnlyThePinnedServerKey() {
        val pinned = PinningTrustManager(ServerTrust(PinningTrustManager.spkiSha256(certificate)))
        pinned.checkServerTrusted(arrayOf(certificate), "ECDHE_ECDSA")

        val other = PinningTrustManager(ServerTrust(ByteArray(32)))
        assertFailsWith<CertificateException> { other.checkServerTrusted(arrayOf(certificate), "ECDHE_ECDSA") }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    private val certificate = CertificateFactory.getInstance("X.509")
        .generateCertificate(ByteArrayInputStream(PEM.toByteArray())) as X509Certificate

    private companion object {
        val PEM = """
-----BEGIN CERTIFICATE-----
MIIBhTCCASugAwIBAgIUFW1lItwmb1zn9mnFZX6D4/h7VO0wCgYIKoZIzj0EAwIw
FzEVMBMGA1UEAwwMVSdKdWRnZSB0ZXN0MCAXDTI2MDkyNjEyMjYwM1oYDzIxMjYw
OTAyMTIyNjAzWjAXMRUwEwYDVQQDDAxVJ0p1ZGdlIHRlc3QwWTATBgcqhkjOPQIB
BggqhkjOPQMBBwNCAASzG0kiLF+xzbV3KcYbw3xRH/hgm57w6C2hm598rWwXlMTk
W4tztH2md8dwvraS8+PvZ/McjYjamNKI6SfBQJ3/o1MwUTAdBgNVHQ4EFgQUA+uI
Egy8YvfXry2GpEFr9IhJ/5QwHwYDVR0jBBgwFoAUA+uIEgy8YvfXry2GpEFr9IhJ
/5QwDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNIADBFAiBF3xKwmUFnQL1r
1ZXlxnFh3g69Pqv6YIpes2uytTw0pAIhAKoRCH/vkqfq4AgtjOwId2c0fkyq+J6n
A8cjzhVku12d
-----END CERTIFICATE-----
        """.trimIndent()
    }
}
