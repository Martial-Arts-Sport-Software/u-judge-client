package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class PairingStatusClientTest {
    @Test
    fun readsPendingPairingStatus() = runTest {
        lateinit var request: HttpRequestData
        val client = PairingStatusClient(
            HttpClient(MockEngine {
                request = it
                respond(
                    """{"type":"pairing_status","state":"pending","deviceId":"android-8"}""",
                    HttpStatusCode.OK
                )
            }),
            Url("http://court.local")
        )

        assertEquals(PairingStatusResult.Pending("android-8"), client.fetch("request-1"))
        assertEquals("GET", request.method.value)
        assertEquals("/v1/pairing-status/request-1", request.url.encodedPath)
    }

    @Test
    fun readsAcceptedPairingStatus() = runTest {
        val client = clientReturning(
            """{"type":"pairing_status","state":"accepted","deviceId":"ios-8"}"""
        )

        assertEquals(PairingStatusResult.Accepted("ios-8"), client.fetch("request-2"))
    }

    @Test
    fun readsRejectedPairingStatusAndCode() = runTest {
        val client = clientReturning(
            """{"type":"pairing_status","state":"rejected","deviceId":"android-10","code":"operator_rejected"}"""
        )

        assertEquals(
            PairingStatusResult.Rejected("android-10", "operator_rejected"),
            client.fetch("request-3")
        )
    }

    @Test
    fun returnsNotFoundForAnUnknownPairingRequest() = runTest {
        val client = PairingStatusClient(
            HttpClient(MockEngine { respond("", HttpStatusCode.NotFound) }),
            Url("http://court.local")
        )

        assertEquals(PairingStatusResult.NotFound, client.fetch("unknown-request"))
    }

    @Test
    fun rejectsMalformedPairingStatus() = runTest {
        val client = clientReturning(
            """{"type":"pairing_status","state":"rejected","deviceId":"android-10"}"""
        )

        assertEquals(PairingStatusResult.MalformedResponse, client.fetch("request-3"))
    }

    @Test
    fun returnsUnavailableWhenThePairingStatusRequestFails() = runTest {
        val client = PairingStatusClient(
            HttpClient(MockEngine { error("connection refused") }),
            Url("http://court.local")
        )

        assertEquals(PairingStatusResult.Unavailable, client.fetch("request-1"))
    }

    private fun clientReturning(response: String): PairingStatusClient = PairingStatusClient(
        HttpClient(MockEngine { respond(response, HttpStatusCode.OK) }),
        Url("http://court.local")
    )
}
