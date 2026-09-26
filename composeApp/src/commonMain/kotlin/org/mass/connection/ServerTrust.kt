package org.mass.connection

import kotlin.concurrent.Volatile

/**
 * Trust in one server's TLS key (server ADR-006). Without a stored pin the first presented key is captured (trust on first
 * use) and every later connection of the same flow must present it again; with a stored pin only that key is accepted.
 * Hostname and CA validation are replaced by the pin, so a DHCP address change does not break trust.
 */
class ServerTrust(expectedPin: ByteArray? = null) {
    // Written from TLS handshake threads, read from the UI and coroutines.
    @Volatile
    private var pin: ByteArray? = expectedPin?.copyOf()

    /** Set when a connection presented a key other than the pinned one; the credential is never sent to it. */
    @Volatile
    var identityChanged: Boolean = false
        private set

    /** SHA-256 of the pinned SubjectPublicKeyInfo, once known. */
    val pinnedSpki: ByteArray?
        get() = pin?.copyOf()

    /** Six digits the judge compares with the operator's desktop before approval. */
    val verificationCode: String?
        get() = pin?.let(::verificationCode)

    /** Accepts [spkiSha256] when it matches the pin, or captures it as the pin on first use. */
    fun accept(spkiSha256: ByteArray): Boolean {
        val current = pin
        if (current == null) {
            pin = spkiSha256.copyOf()
            return true
        }
        return current.contentEquals(spkiSha256).also { matches -> if (!matches) identityChanged = true }
    }
}

/** First three bytes of the SPKI hash as an unsigned big-endian integer modulo 1 000 000, zero-padded (ADR-006). */
fun verificationCode(spkiSha256: ByteArray): String {
    val value = ((spkiSha256[0].toInt() and 0xff) shl 16) or
        ((spkiSha256[1].toInt() and 0xff) shl 8) or
        (spkiSha256[2].toInt() and 0xff)
    return (value % 1_000_000).toString().padStart(6, '0')
}

/** `123 456`, the form shown on both screens. */
fun formatVerificationCode(code: String): String = "${code.take(3)} ${code.drop(3)}"
