package org.mass.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest

class ServerMetadataClientTest {
    @Test
    fun buildsMetadataEndpointFromResolvedAddressAndPort() {
        assertEquals(
            Url("http://192.168.1.10:8080"),
            metadataEndpoint("192.168.1.10", 8080)
        )
    }

    @Test
    fun fetchesAndDecodesAcceptedServerMetadata() = runTest {
        val client = clientReturning("""
            {
              "protocolVersion":"1.0",
              "capabilities":{"metadata":true,"pairing":true},
              "peerId":"peer-47",
              "courtId":"court-2",
              "serverName":"Court Two",
              "pairingPolicy":"operator-approval",
              "serverTime":"2026-08-31T12:00:00Z"
            }
        """.trimIndent())

        assertEquals(
            MetadataFetchResult.Success(
                ServerMetadata(
                    protocolMajor = 1,
                    protocolMinor = 0,
                    capabilities = setOf("metadata", "pairing"),
                    peerId = "peer-47",
                    courtId = "court-2",
                    serverName = "Court Two",
                    pairingPolicy = "operator-approval",
                    serverTimeMillis = 1_788_177_600_000
                )
            ),
            client.fetch()
        )
    }

    @Test
    fun rejectsMalformedMetadataWithoutReturningServerDetails() = runTest {
        val client = clientReturning("""{"protocolVersion":"unsupported"}""")

        assertIs<MetadataFetchResult.MalformedResponse>(client.fetch())
    }

    @Test
    fun returnsUnavailableWhenTheMetadataRequestFails() = runTest {
        val client = ServerMetadataClient(
            HttpClient(MockEngine { error("connection refused") }),
            Url("http://court.local")
        )

        assertIs<MetadataFetchResult.Unavailable>(client.fetch())
    }

    @Test
    fun appliesCompatibleMetadataToTheSelectedServer() = runTest {
        val store = ConnectionStateStore()
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))

        clientReturning("""
            {
              "protocolVersion":"1.0",
              "capabilities":{"metadata":true},
              "peerId":"peer-47",
              "courtId":"court-2",
              "serverName":"Court Two",
              "pairingPolicy":"operator-approval",
              "serverTime":"2026-08-31T12:00:00Z"
            }
        """.trimIndent()).fetchInto(store)

        assertIs<ConnectionState.MetadataValidated>(store.state)
    }

    @Test
    fun appliesUnavailableMetadataFailureToTheSelectedServer() = runTest {
        val store = ConnectionStateStore()
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("court-1"))
        val client = ServerMetadataClient(
            HttpClient(MockEngine { error("connection refused") }),
            Url("http://court.local")
        )

        client.fetchInto(store)

        assertEquals(
            ConnectionState.Rejected("court-1", ConnectionFailure.MetadataUnavailable),
            store.state
        )
    }

    private fun clientReturning(response: String): ServerMetadataClient = ServerMetadataClient(
        HttpClient(MockEngine { respond(response, HttpStatusCode.OK) }),
        Url("http://court.local")
    )
}
