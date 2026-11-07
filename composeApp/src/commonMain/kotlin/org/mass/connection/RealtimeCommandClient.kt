package org.mass.connection

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mass.transport.DurableEventOutbox
import org.mass.transport.OutboxEvent

sealed interface RealtimeCommandResult {
    data class Accepted(val eventId: String) : RealtimeCommandResult
    data class Rejected(val eventId: String, val code: String) : RealtimeCommandResult

    /** A transient rejection: the event stays pending and is retried with its original ID after backoff. */
    data class Retry(val eventId: String, val code: String) : RealtimeCommandResult
    data object InvalidResponse : RealtimeCommandResult
}

/** Sends an already durable command and applies only its matching terminal server response. */
class RealtimeCommandClient(
    private val outbox: DurableEventOutbox,
    private val onTerminalResult: (RealtimeCommandResult) -> Unit = {}
) {
    suspend fun send(event: OutboxEvent, socket: RealtimeSocket, nowMillis: Long): RealtimeCommandResult {
        return send(event, SerializedRealtimeRequestChannel(socket), nowMillis)
    }

    suspend fun send(
        event: OutboxEvent,
        channel: RealtimeRequestChannel,
        nowMillis: Long
    ): RealtimeCommandResult {
        require(event.clientTimestamp.isNotBlank())
        require(event.sessionId.isNotBlank())
        if (outbox.pendingEvents().none { it.eventId == event.eventId }) {
            outbox.enqueue(event)
        }
        outbox.recordAttempt(event.eventId, nowMillis)
        val result = when (val response = decode(channel.exchange(event.toJson()))) {
            is Response.Acknowledged -> if (response.eventId == event.eventId && outbox.acknowledge(event.eventId)) {
                RealtimeCommandResult.Accepted(event.eventId)
            } else {
                RealtimeCommandResult.InvalidResponse
            }
            is Response.Rejected -> when {
                response.eventId != event.eventId -> RealtimeCommandResult.InvalidResponse
                response.code in RETRYABLE_CODES -> RealtimeCommandResult.Retry(event.eventId, response.code)
                outbox.reject(event.eventId, response.code) -> RealtimeCommandResult.Rejected(event.eventId, response.code)
                else -> RealtimeCommandResult.InvalidResponse
            }
            Response.Invalid -> RealtimeCommandResult.InvalidResponse
        }
        if (result is RealtimeCommandResult.Accepted || result is RealtimeCommandResult.Rejected) {
            onTerminalResult(result)
        }
        return result
    }

    /**
     * A typed command (`kerugi_score_command`, `judge_attention_command`) keeps its audit context and fields in the
     * payload and gets the envelope of server ADR-009 §6. Any other payload is an older generic `command`, which the
     * server rejects with the terminal `unsupported_command`.
     */
    private fun OutboxEvent.toJson(): String {
        val body = Json.parseToJsonElement(payload).jsonObject
        val type = (body["type"] as? JsonPrimitive)?.content
        return if (type != null && type.endsWith(TYPED_COMMAND_SUFFIX)) {
            JsonObject(buildMap {
                put("type", body.getValue("type"))
                putAll(buildJsonObject {
                    put("eventId", eventId)
                    put("sequence", clientSequence)
                    put("clientTimestamp", clientTimestamp)
                })
                putAll(body - "type")
            }).toString()
        } else {
            buildJsonObject {
                put("type", "command")
                put("eventId", eventId)
                put("sequence", clientSequence)
                put("clientTimestamp", clientTimestamp)
                put("sessionId", sessionId)
                put("payload", body)
            }.toString()
        }
    }

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

    companion object {
        private const val TYPED_COMMAND_SUFFIX = "_command"

        /** Every rejection code is terminal except a failed journal write (server ADR-009 §8). */
        val RETRYABLE_CODES = setOf("journal_unavailable")
    }
}
