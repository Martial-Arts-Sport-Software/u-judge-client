package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface PairingStatus {
    val deviceId: String

    data class Pending(override val deviceId: String) : PairingStatus
    data class Accepted(override val deviceId: String) : PairingStatus
    data class Rejected(override val deviceId: String, val code: String) : PairingStatus
}

sealed interface PairingStatusFetchResult {
    data class Success(val status: PairingStatus) : PairingStatusFetchResult
    data object MalformedResponse : PairingStatusFetchResult
    data object Unavailable : PairingStatusFetchResult
}

class PairingStatusClient(
    private val httpClient: HttpClient,
    private val endpoint: Url
) {
    suspend fun fetch(requestId: String): PairingStatusFetchResult = try {
        val response = httpClient.get(endpoint) {
            url.appendPathSegments("v1", "pairing-status", requestId)
        }
        if (response.status.value !in 200..299) {
            PairingStatusFetchResult.Unavailable
        } else {
            decode(response.bodyAsText())
        }
    } catch (exception: Exception) {
        if (exception is CancellationException) throw exception
        PairingStatusFetchResult.Unavailable
    }

    private fun decode(response: String): PairingStatusFetchResult = try {
        val body = Json.parseToJsonElement(response).jsonObject
        val deviceId = body["deviceId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        if (body["type"]?.jsonPrimitive?.content != "pairing_status" || deviceId == null) {
            return PairingStatusFetchResult.MalformedResponse
        }

        val status = when (body["state"]?.jsonPrimitive?.content) {
            "pending" -> if (body["code"] == null) PairingStatus.Pending(deviceId) else null
            "accepted" -> if (body["code"] == null) PairingStatus.Accepted(deviceId) else null
            "rejected" -> body["code"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?.let { PairingStatus.Rejected(deviceId, it) }
            else -> null
        }
        status?.let(PairingStatusFetchResult::Success) ?: PairingStatusFetchResult.MalformedResponse
    } catch (_: Exception) {
        PairingStatusFetchResult.MalformedResponse
    }
}
