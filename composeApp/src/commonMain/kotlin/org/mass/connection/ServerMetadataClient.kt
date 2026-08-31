package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

sealed interface MetadataFetchResult {
    data class Success(val metadata: ServerMetadata) : MetadataFetchResult
    data object MalformedResponse : MetadataFetchResult
    data object Unavailable : MetadataFetchResult
}

class ServerMetadataClient(
    private val httpClient: HttpClient,
    private val endpoint: Url
) {
    suspend fun fetch(): MetadataFetchResult = try {
        decode(httpClient.get(endpoint) {
            url.appendPathSegments("v1", "metadata")
        }.bodyAsText())
    } catch (_: Exception) {
        MetadataFetchResult.Unavailable
    }

    suspend fun fetchInto(store: ConnectionStateStore): MetadataFetchResult {
        val result = fetch()
        if (result is MetadataFetchResult.Success) {
            store.dispatch(ConnectionEvent.ValidateMetadata(result.metadata))
        }
        return result
    }

    private fun decode(response: String): MetadataFetchResult = try {
        val metadata = Json.parseToJsonElement(response).jsonObject
        val version = metadata.getValue("protocolVersion").jsonPrimitive.content.split('.')
        val capabilities = metadata.getValue("capabilities").jsonObject
            .filterValues { it.jsonPrimitive.content == "true" }
            .keys

        MetadataFetchResult.Success(
            ServerMetadata(
                protocolMajor = version[0].toInt(),
                protocolMinor = version[1].toInt(),
                capabilities = capabilities,
                peerId = metadata.getValue("peerId").jsonPrimitive.content,
                courtId = metadata.getValue("courtId").jsonPrimitive.content,
                serverName = metadata.getValue("serverName").jsonPrimitive.content,
                pairingPolicy = metadata.getValue("pairingPolicy").jsonPrimitive.content,
                serverTimeMillis = Instant.parse(metadata.getValue("serverTime").jsonPrimitive.content).toEpochMilliseconds()
            )
        )
    } catch (_: Exception) {
        MetadataFetchResult.MalformedResponse
    }
}
