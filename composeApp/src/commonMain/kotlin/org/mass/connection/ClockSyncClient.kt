package org.mass.connection

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Instant

sealed interface ClockSyncResult {
    data class Synchronized(val offsetMillis: Long, val roundTripMillis: Long) : ClockSyncResult
    data class Rejected(val code: String) : ClockSyncResult
    data object InvalidResponse : ClockSyncResult
}

/** Estimates the server clock offset from one authenticated realtime four-timestamp exchange. */
class ClockSyncClient(private val nowMillis: () -> Long) {
    suspend fun synchronize(socket: RealtimeSocket): ClockSyncResult {
        val clientSendMillis = nowMillis()
        val clientSendTimestamp = Instant.fromEpochMilliseconds(clientSendMillis).toString()
        socket.send(buildJsonObject {
            put("type", "clock_sync")
            put("clientSendTimestamp", clientSendTimestamp)
        }.toString())
        val response = socket.receive()
        val clientReceiveMillis = nowMillis()

        return decode(response, clientSendTimestamp)?.let { decoded ->
            when (decoded) {
                is Response.Synchronized -> ClockSyncResult.Synchronized(
                    offsetMillis = ((decoded.serverReceiveMillis - clientSendMillis) +
                        (decoded.serverSendMillis - clientReceiveMillis)) / 2,
                    roundTripMillis = (clientReceiveMillis - clientSendMillis) -
                        (decoded.serverSendMillis - decoded.serverReceiveMillis)
                )
                is Response.Rejected -> ClockSyncResult.Rejected(decoded.code)
            }
        } ?: ClockSyncResult.InvalidResponse
    }

    private fun decode(payload: String, expectedClientSendTimestamp: String): Response? = try {
        val body = Json.parseToJsonElement(payload).jsonObject
        when (body["type"]?.jsonPrimitive?.content) {
            "clock_sync_response" -> {
                val clientSendTimestamp = body["clientSendTimestamp"]?.jsonPrimitive?.content
                if (clientSendTimestamp != expectedClientSendTimestamp) {
                    null
                } else {
                    val serverReceiveMillis = body["serverReceiveTimestamp"]?.jsonPrimitive?.content
                        ?.let(Instant::parse)
                        ?.toEpochMilliseconds()
                    val serverSendMillis = body["serverSendTimestamp"]?.jsonPrimitive?.content
                        ?.let(Instant::parse)
                        ?.toEpochMilliseconds()
                    if (serverReceiveMillis != null && serverSendMillis != null) {
                        Response.Synchronized(serverReceiveMillis, serverSendMillis)
                    } else {
                        null
                    }
                }
            }
            "clock_sync_rejected" -> body["code"]?.jsonPrimitive?.content
                ?.takeIf { it.isNotBlank() }
                ?.let(Response::Rejected)
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    private sealed interface Response {
        data class Synchronized(val serverReceiveMillis: Long, val serverSendMillis: Long) : Response
        data class Rejected(val code: String) : Response
    }
}
