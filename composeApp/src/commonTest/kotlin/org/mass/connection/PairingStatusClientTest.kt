package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

class PairingStatusClientTest {
    @Test
    fun decodesPendingProjectionFromThePublicStatusEndpoint() = runTest {
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

        assertEquals(
            PairingStatusFetchResult.Success(PairingStatus.Pending("android-8")),
            client.fetch("request-1")
        )
        assertEquals("GET", request.method.value)
        assertEquals("/v1/pairing-status/request-1", request.url.encodedPath)
    }

    @Test
    fun decodesAcceptedProjectionWithoutGrantingConnectionAccess() = runTest {
        val client = PairingStatusClient(
            HttpClient(MockEngine {
                respond(
                    """{"type":"pairing_status","state":"accepted","deviceId":"ios-8"}""",
                    HttpStatusCode.OK
                )
            }),
            Url("http://court.local")
        )

        val store = pairingPendingStore()

        assertEquals(
            PairingStatusFetchResult.Success(PairingStatus.Accepted("ios-8")),
            client.fetch("request-1")
        )
        assertEquals(ConnectionState.PairingPending("court-1"), store.state)
        assertFalse(store.isPaired)
    }

    @Test
    fun decodesRejectionCodeOnlyForRejectedProjection() = runTest {
        val client = PairingStatusClient(
            HttpClient(MockEngine {
                respond(
                    """{"type":"pairing_status","state":"rejected","deviceId":"android-10","code":"operator_rejected"}""",
                    HttpStatusCode.OK
                )
            }),
            Url("http://court.local")
        )

        assertEquals(
            PairingStatusFetchResult.Success(
                PairingStatus.Rejected("android-10", "operator_rejected")
            ),
            client.fetch("request-1")
        )
    }

    @Test
    fun rejectsMalformedOrUnavailableStatusProjection() = runTest {
        val malformed = PairingStatusClient(
            HttpClient(MockEngine {
                respond(
                    """{"type":"pairing_status","state":"accepted","deviceId":"ios-8","code":"unexpected"}""",
                    HttpStatusCode.OK
                )
            }),
            Url("http://court.local")
        )
        val unavailable = PairingStatusClient(
            HttpClient(MockEngine { respond("", HttpStatusCode.NotFound) }),
            Url("http://court.local")
        )

        assertEquals(PairingStatusFetchResult.MalformedResponse, malformed.fetch("request-1"))
        assertEquals(PairingStatusFetchResult.Unavailable, unavailable.fetch("request-1"))
    }

    private fun pairingPendingStore(): ConnectionStateStore = ConnectionStateStore().also { store ->
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        store.dispatch(ConnectionEvent.ValidateMetadata(metadata()))
        store.dispatch(ConnectionEvent.RequestPairing)
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
