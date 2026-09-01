package org.mass.transport

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Platform storage for the complete outbox journal, retained across process recreation. */
interface EventOutboxStorage {
    fun load(): String?

    fun save(value: String)
}

/** Immutable event identity and payload created before the event reaches transport. */
data class OutboxEvent(
    val eventId: String,
    val clientSequence: Long,
    val clientTimestampMillis: Long,
    val clientTimestamp: String = "",
    val sessionId: String = "",
    val payload: String
)

data class PendingOutboxEvent(
    val event: OutboxEvent,
    val attempt: Int,
    val nextAttemptAtMillis: Long
)

data class RejectedOutboxEvent(
    val event: OutboxEvent,
    val reason: String
)

/**
 * Persists client-generated events until a terminal server outcome. Retries are strictly ordered
 * by client sequence so a later event cannot overtake an earlier pending event.
 */
class DurableEventOutbox(
    private val storage: EventOutboxStorage,
    private val retryBaseMillis: Long = 1_000,
    private val retryMaximumMillis: Long = 30_000
) {
    private val records = decode(storage.load()).toMutableList()

    init {
        require(retryBaseMillis > 0)
        require(retryMaximumMillis >= retryBaseMillis)
    }

    fun enqueue(event: OutboxEvent) {
        require(event.eventId.isNotBlank())
        require(event.clientSequence >= 0)
        require(records.none { it.event.eventId == event.eventId })
        records += Record.Pending(event, attempt = 0, nextAttemptAtMillis = event.clientTimestampMillis)
        persist()
    }

    fun pendingEvents(): List<OutboxEvent> = records.filterIsInstance<Record.Pending>()
        .sortedBy { it.event.clientSequence }
        .map { it.event }

    fun rejectedEvents(): List<RejectedOutboxEvent> = records.filterIsInstance<Record.Rejected>()
        .sortedBy { it.event.clientSequence }
        .map { RejectedOutboxEvent(it.event, it.reason) }

    fun nextEventDue(nowMillis: Long): OutboxEvent? {
        val earliest = records.filterIsInstance<Record.Pending>().minByOrNull { it.event.clientSequence }
        return earliest?.event?.takeIf { earliest.nextAttemptAtMillis <= nowMillis }
    }

    fun recordAttempt(eventId: String, nowMillis: Long): PendingOutboxEvent? {
        val index = records.indexOfFirst { it is Record.Pending && it.event.eventId == eventId }
        val current = records.getOrNull(index) as? Record.Pending ?: return null
        val updated = current.copy(
            attempt = current.attempt + 1,
            nextAttemptAtMillis = nowMillis + retryDelay(current.attempt)
        )
        records[index] = updated
        persist()
        return PendingOutboxEvent(updated.event, updated.attempt, updated.nextAttemptAtMillis)
    }

    fun acknowledge(eventId: String): Boolean {
        val index = records.indexOfFirst { it is Record.Pending && it.event.eventId == eventId }
        if (index == -1) return false
        records.removeAt(index)
        persist()
        return true
    }

    fun reject(eventId: String, reason: String): Boolean {
        val index = records.indexOfFirst { it is Record.Pending && it.event.eventId == eventId }
        val pending = records.getOrNull(index) as? Record.Pending ?: return false
        records[index] = Record.Rejected(pending.event, reason)
        persist()
        return true
    }

    private fun retryDelay(attempt: Int): Long {
        var delay = retryBaseMillis
        repeat(attempt) {
            delay = (delay * 2).coerceAtMost(retryMaximumMillis)
        }
        return delay
    }

    private fun persist() {
        storage.save(buildJsonArray {
            records.forEach { record ->
                add(buildJsonObject {
                    put("eventId", record.event.eventId)
                    put("clientSequence", record.event.clientSequence)
                    put("clientTimestampMillis", record.event.clientTimestampMillis)
                    put("clientTimestamp", record.event.clientTimestamp)
                    put("sessionId", record.event.sessionId)
                    put("payload", record.event.payload)
                    when (record) {
                        is Record.Pending -> {
                            put("status", "pending")
                            put("attempt", record.attempt)
                            put("nextAttemptAtMillis", record.nextAttemptAtMillis)
                        }
                        is Record.Rejected -> {
                            put("status", "rejected")
                            put("reason", record.reason)
                        }
                    }
                })
            }
        }.toString())
    }

    private fun decode(serialized: String?): List<Record> = try {
        Json.parseToJsonElement(serialized.orEmpty()).jsonArray.mapNotNull { element ->
            val record = element.jsonObject
            val event = OutboxEvent(
                eventId = record["eventId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                clientSequence = record["clientSequence"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@mapNotNull null,
                clientTimestampMillis = record["clientTimestampMillis"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@mapNotNull null,
                clientTimestamp = record["clientTimestamp"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                sessionId = record["sessionId"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                payload = record["payload"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            )
            when (record["status"]?.jsonPrimitive?.contentOrNull) {
                "pending" -> Record.Pending(
                    event = event,
                    attempt = record["attempt"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return@mapNotNull null,
                    nextAttemptAtMillis = record["nextAttemptAtMillis"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: return@mapNotNull null
                )
                "rejected" -> Record.Rejected(
                    event = event,
                    reason = record["reason"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                )
                else -> null
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private sealed interface Record {
        val event: OutboxEvent

        data class Pending(
            override val event: OutboxEvent,
            val attempt: Int,
            val nextAttemptAtMillis: Long
        ) : Record

        data class Rejected(
            override val event: OutboxEvent,
            val reason: String
        ) : Record
    }
}
