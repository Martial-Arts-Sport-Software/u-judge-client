package org.mass.connection

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mass.transport.DurableEventOutbox
import org.mass.transport.OutboxEvent

sealed interface RealtimeCommandResult {
    data class Accepted(val eventId: String) : RealtimeCommandResult
    data class Rejected(val eventId: String, val code: String) : RealtimeCommandResult
    data object InvalidResponse : RealtimeCommandResult
}

/** Sends an already durable command and applies only its matching terminal server response. */
class RealtimeCommandClient(private val outbox: DurableEventOutbox) {
    suspend fun send(event: OutboxEvent, socket: RealtimeSocket, nowMillis: Long): RealtimeCommandResult {
        require(event.clientTimestamp.isNotBlank())
        require(event.sessionId.isNotBlank())
        if (outbox.pendingEvents().none { it.eventId == event.eventId }) {
            outbox.enqueue(event)
        }
        outbox.recordAttempt(event.eventId, nowMillis)
        socket.send(event.toJson())
        return when (val response = decode(socket.receive())) {
            is Response.Acknowledged -> if (response.eventId == event.eventId && outbox.acknowledge(event.eventId)) {
                RealtimeCommandResult.Accepted(event.eventId)
            } else {
                RealtimeCommandResult.InvalidResponse
            }
            is Response.Rejected -> if (response.eventId == event.eventId && outbox.reject(event.eventId, response.code)) {
                RealtimeCommandResult.Rejected(event.eventId, response.code)
            } else {
                RealtimeCommandResult.InvalidResponse
            }
            Response.Invalid -> RealtimeCommandResult.InvalidResponse
        }
    }

    private fun OutboxEvent.toJson(): String = buildJsonObject {
        put("type", "command")
        put("eventId", eventId)
        put("sequence", clientSequence)
        put("clientTimestamp", clientTimestamp)
        put("sessionId", sessionId)
        put("payload", Json.parseToJsonElement(payload))
    }.toString()

    private fun decode(payload: String): Response = try {
        val body = Json.parseToJsonElement(payload).jsonObject
        val eventId = body["eventId"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        when (body["type"]?.jsonPrimitive?.content) {
            "command_ack" -> eventId?.let(Response::Acknowledged) ?: Response.Invalid
            "command_rejected" -> {
                val code = body["code"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                if (eventId != null && code != null) Response.Rejected(eventId, code) else Response.Invalid
            }
            else -> Response.Invalid
        }
    } catch (_: Exception) {
        Response.Invalid
    }

    private sealed interface Response {
        data class Acknowledged(val eventId: String) : Response
        data class Rejected(val eventId: String, val code: String) : Response
        data object Invalid : Response
    }
}
