package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

actual fun createHttpClient(trust: ServerTrust): HttpClient = HttpClient(OkHttp) {
    install(WebSockets)
    engine {
        val trustManager = PinningTrustManager(trust)
        val sslContext = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), null) }
        config {
            sslSocketFactory(sslContext.socketFactory, trustManager)
            // The SPKI pin identifies the server; its address may change with DHCP.
            hostnameVerifier { _, _ -> true }
        }
    }
}

/** Accepts only the server key held by [trust]; CA chains are irrelevant for a self-signed peer certificate. */
internal class PinningTrustManager(private val trust: ServerTrust) : X509TrustManager {
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        val leaf = chain.firstOrNull() ?: throw CertificateException("empty certificate chain")
        if (!trust.accept(spkiSha256(leaf))) throw CertificateException("server_identity_changed")
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
        throw CertificateException("client certificates are not used")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    companion object {
        fun spkiSha256(certificate: X509Certificate): ByteArray =
            MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded)
    }
}
