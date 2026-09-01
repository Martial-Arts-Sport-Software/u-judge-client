package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface PairingStatusResult {
    data class Pending(val deviceId: String) : PairingStatusResult
    data class Accepted(val deviceId: String) : PairingStatusResult
    data class Rejected(val deviceId: String, val code: String) : PairingStatusResult
    data object NotFound : PairingStatusResult
    data object MalformedResponse : PairingStatusResult
    data object Unavailable : PairingStatusResult
}

class PairingStatusClient(
    private val httpClient: HttpClient,
    private val endpoint: Url
) {
    suspend fun fetch(requestId: String): PairingStatusResult = try {
        val response = httpClient.get(endpoint) {
            url.appendPathSegments("v1", "pairing-status", requestId)
        }
        when {
            response.status == HttpStatusCode.NotFound -> PairingStatusResult.NotFound
            response.status.value !in 200..299 -> PairingStatusResult.Unavailable
            else -> decode(response.bodyAsText())
        }
    } catch (_: Exception) {
        PairingStatusResult.Unavailable
    }

    private fun decode(response: String): PairingStatusResult = try {
        val body = Json.parseToJsonElement(response).jsonObject
        val deviceId = body["deviceId"]?.jsonPrimitive?.content
        if (body["type"]?.jsonPrimitive?.content != "pairing_status" || deviceId.isNullOrBlank()) {
            return PairingStatusResult.MalformedResponse
        }
        when (body["state"]?.jsonPrimitive?.content) {
            "pending" -> PairingStatusResult.Pending(deviceId)
            "accepted" -> PairingStatusResult.Accepted(deviceId)
            "rejected" -> body["code"]?.jsonPrimitive?.content
                ?.takeIf { it.isNotBlank() }
                ?.let { PairingStatusResult.Rejected(deviceId, it) }
                ?: PairingStatusResult.MalformedResponse
            else -> PairingStatusResult.MalformedResponse
        }
    } catch (_: Exception) {
        PairingStatusResult.MalformedResponse
    }
}
