package org.mass.connection

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

sealed interface HeartbeatResult {
    data object Acknowledged : HeartbeatResult
    data class Rejected(val code: String) : HeartbeatResult
    data object InvalidResponse : HeartbeatResult
}

/** Sends one authenticated heartbeat and validates its terminal server response. */
class HeartbeatClient {
    suspend fun send(socket: RealtimeSocket): HeartbeatResult {
        return send(SerializedRealtimeRequestChannel(socket))
    }

    suspend fun send(channel: RealtimeRequestChannel): HeartbeatResult {
        return decode(channel.exchange(buildJsonObject { put("type", "heartbeat") }.toString()))
    }

    private fun decode(payload: String): HeartbeatResult = try {
        val body = Json.parseToJsonElement(payload).jsonObject
        when (body["type"]?.jsonPrimitive?.content) {
            "heartbeat_ack" -> if (body.size == 1) HeartbeatResult.Acknowledged else HeartbeatResult.InvalidResponse
            "heartbeat_rejected" -> body["code"]?.jsonPrimitive?.content
                ?.takeIf { it.isNotBlank() }
                ?.let(HeartbeatResult::Rejected)
                ?: HeartbeatResult.InvalidResponse
            else -> HeartbeatResult.InvalidResponse
        }
    } catch (_: Exception) {
        HeartbeatResult.InvalidResponse
    }
}
