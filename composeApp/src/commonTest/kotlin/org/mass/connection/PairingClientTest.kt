package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

class PairingClientTest {
    @Test
    fun sendsJudgeAndDeviceIdentityAndMovesValidatedServerToPending() = runTest {
        lateinit var request: HttpRequestData
        val client = PairingClient(
            HttpClient(MockEngine {
                request = it
                respond("""{"state":"pending","requestId":"request-1"}""", HttpStatusCode.Accepted)
            }),
            Url("http://court.local")
        )
        val store = validatedStore()

        assertEquals(
            PairingResult.Pending("request-1"),
            client.request(PairingRequest("device-7", "Ivanov", "ios"), store)
        )
        assertEquals("POST", request.method.value)
        assertEquals("/v1/pairing-requests", request.url.encodedPath)
        assertEquals(
            "{\"deviceId\":\"device-7\",\"surname\":\"Ivanov\",\"platform\":\"ios\"}",
            (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
        )
        assertEquals(ConnectionState.PairingPending("court-1"), store.state)
        assertFalse(store.isPaired)
    }

    @Test
    fun malformedPairingResponseRejectsValidatedServerWithoutGrantingAccess() = runTest {
        val client = PairingClient(
            HttpClient(MockEngine {
                respond("""{"state":"pending"}""", HttpStatusCode.Accepted)
            }),
            Url("http://court.local")
        )
        val store = validatedStore()

        assertEquals(PairingResult.Rejected, client.request(PairingRequest("device-7", "Ivanov", "ios"), store))
        assertEquals(
            ConnectionState.Rejected("court-1", ConnectionFailure.PairingResponseInvalid),
            store.state
        )
        assertFalse(store.isPaired)
    }

    @Test
    fun unavailablePairingRequestRejectsValidatedServerWithoutGrantingAccess() = runTest {
        val client = PairingClient(
            HttpClient(MockEngine { error("connection refused") }),
            Url("http://court.local")
        )
        val store = validatedStore()

        assertEquals(PairingResult.Unavailable, client.request(PairingRequest("device-7", "Ivanov", "ios"), store))
        assertEquals(
            ConnectionState.Rejected("court-1", ConnectionFailure.PairingUnavailable),
            store.state
        )
        assertFalse(store.isPaired)
    }

    @Test
    fun rejectedPairingRequestReportsPairingUnavailable() = runTest {
        val client = PairingClient(
            HttpClient(MockEngine {
                respond("""{"code":"invalid_pairing_request"}""", HttpStatusCode.BadRequest)
            }),
            Url("http://court.local")
        )
        val store = validatedStore()

        assertEquals(PairingResult.Unavailable, client.request(PairingRequest("device-7", "Ivanov", "ios"), store))
        assertEquals(
            ConnectionState.Rejected("court-1", ConnectionFailure.PairingUnavailable),
            store.state
        )
        assertFalse(store.isPaired)
    }

    private fun validatedStore(): ConnectionStateStore = ConnectionStateStore().also { store ->
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(ConnectionEvent.ValidateMetadata(metadata()))
    }

    private fun metadata() = ServerMetadata(
        protocolMajor = 1,
        protocolMinor = 0,
        capabilities = emptySet(),
        peerId = "peer-1",
        courtId = "court-1",
        serverName = "Court 1",
        pairingPolicy = "operator-approval",
        serverTimeMillis = 1_000
    )
}
