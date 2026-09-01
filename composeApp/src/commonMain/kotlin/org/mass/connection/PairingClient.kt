package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class PairingRequest(
    val deviceId: String,
    val surname: String,
    val platform: String
)

sealed interface PairingResult {
    data class Pending(val requestId: String) : PairingResult
    data object Rejected : PairingResult
    data object Unavailable : PairingResult
}

class PairingClient(
    private val httpClient: HttpClient,
    private val endpoint: Url
) {
    suspend fun request(request: PairingRequest, store: ConnectionStateStore): PairingResult = try {
        val response = httpClient.post(endpoint) {
            url.appendPathSegments("v1", "pairing-requests")
            setBody(TextContent(request.toJson(), ContentType.Application.Json))
        }
        val result = if (response.status.value in 200..299) {
            decodePending(response.bodyAsText())
        } else {
            null
        }
        if (result == null) {
            store.dispatch(ConnectionEvent.RejectPairing(ConnectionFailure.PairingResponseInvalid))
            PairingResult.Rejected
        } else {
            store.dispatch(ConnectionEvent.RequestPairing)
            result
        }
    } catch (_: Exception) {
        store.dispatch(ConnectionEvent.RejectPairing(ConnectionFailure.PairingUnavailable))
        PairingResult.Unavailable
    }

    private fun decodePending(response: String): PairingResult.Pending? = try {
        val body = Json.parseToJsonElement(response).jsonObject
        val requestId = body["requestId"]?.jsonPrimitive?.content
        if (body["state"]?.jsonPrimitive?.content == "pending" && !requestId.isNullOrBlank()) {
            PairingResult.Pending(requestId)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    private fun PairingRequest.toJson(): String = buildJsonObject {
        put("deviceId", deviceId)
        put("surname", surname)
        put("platform", platform)
    }.toString()
}
