package org.mass.connection

import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Opt-in check of the Android transport stack (OkHttp + pinning) against a running u-judge-server:
 * `UJUDGE_SERVER_URL=https://127.0.0.1:8443 ./gradlew :composeApp:testAndroidHostTest --tests '*RealServerSmokeTest'`.
 * Operator approval needs the desktop UI, so this stops at a pending request; the emulator scenario covers the rest.
 */
class RealServerSmokeTest {
    @Test
    fun metadataAndPairingRequestOverPinnedTls() = runBlocking {
        val url = System.getenv("UJUDGE_SERVER_URL")
        assumeTrue("Set UJUDGE_SERVER_URL to a running server", !url.isNullOrBlank())
        val endpoint = Url(requireNotNull(url))
        val trust = ServerTrust()
        val store = ConnectionStateStore().apply {
            dispatch(ConnectionEvent.StartDiscovery)
            dispatch(ConnectionEvent.SelectServer(endpoint.toString()))
        }

        createHttpClient(trust).use { client ->
            val pending = PairingFlow(ServerMetadataClient(client, endpoint), PairingClient(client, endpoint)).connect(
                PairingRequest("android-smoke", "Smoke", "android", "proof-smoke"),
                store
            )
            val requestId = assertIs<PairingResult.Pending>(pending).requestId
            assertEquals(
                PairingStatusResult.Pending("android-smoke"),
                PairingStatusClient(client, endpoint).fetch(requestId, "proof-smoke")
            )
        }

        println("RealServerSmokeTest verification code: ${assertNotNull(trust.verificationCode)}")
    }
}
