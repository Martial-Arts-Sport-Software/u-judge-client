package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class PairingFlowTest {
    @Test
    fun requestsPairingAfterCompatibleMetadataValidation() = runTest {
        val store = selectedStore()
        val client = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                "/v1/metadata" -> respond(metadataJson, HttpStatusCode.OK)
                "/v1/pairing-requests" -> respond(
                    """{"state":"pending","requestId":"request-1"}""",
                    HttpStatusCode.Accepted
                )
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
        })

        val result = PairingFlow(
            ServerMetadataClient(client, Url("http://court.local")),
            PairingClient(client, Url("http://court.local"))
        ).connect(PairingRequest("device-1", "Ivanov", "android"), store)

        assertEquals(PairingResult.Pending("request-1"), result)
        assertEquals(ConnectionState.PairingPending("court-1"), store.state)
    }

    @Test
    fun doesNotRequestPairingWhenMetadataIsUnavailable() = runTest {
        val store = selectedStore()
        val client = HttpClient(MockEngine { request ->
            assertEquals("/v1/metadata", request.url.encodedPath)
            respond("", HttpStatusCode.ServiceUnavailable)
        })

        val result = PairingFlow(
            ServerMetadataClient(client, Url("http://court.local")),
            PairingClient(client, Url("http://court.local"))
        ).connect(PairingRequest("device-1", "Ivanov", "android"), store)

        assertEquals(null, result)
        assertEquals(
            ConnectionState.Rejected("court-1", ConnectionFailure.MetadataUnavailable),
            store.state
        )
    }

    private fun selectedStore() = ConnectionStateStore().also { store ->
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
    }

    private companion object {
        const val metadataJson = """
            {
              "protocolVersion":"1.0",
              "capabilities":{"metadata":true},
              "peerId":"peer-1",
              "courtId":"court-1",
              "serverName":"Court 1",
              "pairingPolicy":"operator-approval",
              "serverTime":"2026-09-01T12:00:00Z"
            }
        """
    }
}
