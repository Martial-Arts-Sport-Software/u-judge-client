@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecCertificateCopyKey
import platform.Security.SecKeyCopyExternalRepresentation
import platform.Security.SecTrustGetCertificateAtIndex
import platform.Security.SecTrustRef
import platform.posix.memcpy

actual fun createHttpClient(trust: ServerTrust): HttpClient = HttpClient(Darwin) {
    install(WebSockets)
    engine {
        handleChallenge { _, _, challenge, completionHandler ->
            val space = challenge.protectionSpace
            if (space.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
                completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
                return@handleChallenge
            }
            val serverTrust = space.serverTrust
            val spki = serverTrust?.let(::p256SpkiSha256)
            if (serverTrust != null && spki != null && trust.accept(spki)) {
                completionHandler(NSURLSessionAuthChallengeUseCredential, NSURLCredential.credentialForTrust(serverTrust))
            } else {
                completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
            }
        }
    }
}

/**
 * SHA-256 of the leaf certificate's SubjectPublicKeyInfo. iOS exports an EC key as the raw X9.62 point, so the fixed
 * DER header of a P-256 SubjectPublicKeyInfo is prepended; other key types are not trusted.
 */
private fun p256SpkiSha256(serverTrust: SecTrustRef): ByteArray? {
    @Suppress("DEPRECATION")
    val certificate = SecTrustGetCertificateAtIndex(serverTrust, 0) ?: return null
    val key = SecCertificateCopyKey(certificate) ?: return null
    val external = SecKeyCopyExternalRepresentation(key, null) ?: return null
    val point = (CFBridgingRelease(external) as NSData).toByteArray()
    if (point.size != P256_POINT_LENGTH || point[0] != 0x04.toByte()) return null
    return sha256(P256_SPKI_PREFIX + point)
}

private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).also { bytes ->
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
}

private fun sha256(input: ByteArray): ByteArray = ByteArray(CC_SHA256_DIGEST_LENGTH).also { digest ->
    input.usePinned { source ->
        digest.usePinned { target ->
            CC_SHA256(source.addressOf(0), input.size.convert(), target.addressOf(0).reinterpret())
        }
    }
}

private const val P256_POINT_LENGTH = 65

private val P256_SPKI_PREFIX = "3059301306072a8648ce3d020106082a8648ce3d030107034200"
    .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
