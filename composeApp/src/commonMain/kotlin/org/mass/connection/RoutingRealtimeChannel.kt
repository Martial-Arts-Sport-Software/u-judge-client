package org.mass.connection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Once a socket is subscribed the server pushes at any time, so one reader takes every frame and routes it (server
 * ADR-009 §1.5): command replies by `eventId`, resync replies by `requestId`, heartbeat and clock replies by type, and
 * everything else to [onPush].
 *
 * The channel is confined to one thread: the paired session scope runs on the main dispatcher.
 */
class RoutingRealtimeChannel(
    private val socket: RealtimeSocket,
    scope: CoroutineScope,
    private val onPush: (JsonObject) -> Unit,
    private val onClosed: (RoutingRealtimeChannel) -> Unit = {}
) : RealtimeRequestChannel {
    private val waiting = LinkedHashMap<String, CompletableDeferred<String>>()
    private var failure: Exception? = null
    private var closed = false
    private val reader = scope.launch { read() }

    override suspend fun exchange(payload: String): String {
        val key = requireNotNull(requestKey(payload)) { "Realtime request without a reply key" }
        failure?.let { throw RealtimeSocketClosedException(it) }
        check(key !in waiting) { "A reply for $key is already awaited" }
        val reply = CompletableDeferred<String>()
        waiting[key] = reply
        try {
            socket.send(payload)
            return reply.await()
        } finally {
            if (waiting[key] === reply) waiting.remove(key)
        }
    }

    override suspend fun close() {
        if (closed) return
        closed = true
        reader.cancel()
        fail(IllegalStateException("Realtime channel closed"))
        socket.close()
    }

    private suspend fun read() {
        try {
            while (true) {
                route(socket.receive())
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            fail(exception)
            if (!closed) onClosed(this)
        }
    }

    private fun route(frame: String) {
        val body = try {
            Json.parseToJsonElement(frame).jsonObject
        } catch (_: Exception) {
            return
        }
        val key = replyKey(body)
        if (key == null) {
            onPush(body)
            return
        }
        // A reply whose ID could not be echoed answers the oldest request of its kind: replies keep request order.
        val target = waiting.remove(key)
            ?: key.takeIf { it.endsWith(UNKNOWN_ID) }
                ?.removeSuffix(UNKNOWN_ID)
                ?.let { prefix -> waiting.keys.firstOrNull { it.startsWith(prefix) } }
                ?.let(waiting::remove)
        target?.complete(frame)
    }

    private fun fail(cause: Exception) {
        if (failure == null) failure = cause
        val pending = waiting.values.toList()
        waiting.clear()
        pending.forEach { it.completeExceptionally(RealtimeSocketClosedException(cause)) }
    }

    private companion object {
        const val UNKNOWN_ID = "?"

        fun requestKey(payload: String): String? = try {
            val body = Json.parseToJsonElement(payload).jsonObject
            when (body.text("type")) {
                "heartbeat" -> "heartbeat"
                "clock_sync" -> "clock_sync"
                "resync_request" -> body.text("requestId")?.let { "request:$it" }
                null -> null
                else -> body.text("eventId")?.let { "event:$it" }
            }
        } catch (_: Exception) {
            null
        }

        fun replyKey(body: JsonObject): String? = when (body.text("type")) {
            "heartbeat_ack", "heartbeat_rejected" -> "heartbeat"
            "clock_sync_response", "clock_sync_rejected" -> "clock_sync"
            "command_ack", "command_rejected" -> "event:" + (body.text("eventId") ?: UNKNOWN_ID)
            "resync_response", "resync_rejected" -> "request:" + (body.text("requestId") ?: UNKNOWN_ID)
            else -> null
        }

        fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.takeIf { it.isNotBlank() }
    }
}
