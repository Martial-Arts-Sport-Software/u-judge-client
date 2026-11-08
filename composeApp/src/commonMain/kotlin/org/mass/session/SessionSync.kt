package org.mass.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.mass.connection.RealtimeReconnectLifecycle
import org.mass.connection.RealtimeRequestChannel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Keeps [store] in step with the server on one authenticated socket (server ADR-009 §4, §10): after the outbox replay it
 * sends `resync_request` with the held cursor, applies the `resync_response`, retries `journal_unavailable` with backoff
 * and resyncs again when a push names another session. Pushes are applied by cursor.
 */
class SessionSync(
    private val scope: CoroutineScope,
    private val store: SessionStateStore,
    private val requestId: () -> String = ::newRequestId,
    private val retryDelayMillis: (attempt: Int) -> Long = { RealtimeReconnectLifecycle.defaultRetryDelayMillis(it) }
) {
    private var channel: RealtimeRequestChannel? = null
    private var resyncJob: Job? = null

    /** The resync could not be completed on the current socket; controls stay disabled. */
    var failureCode by mutableStateOf<String?>(null)
        private set

    /** Starts synchronising a socket whose outbox replay has finished. */
    fun attach(channel: RealtimeRequestChannel) {
        detach()
        this.channel = channel
        resync()
    }

    /** The socket was lost: controls are disabled until a snapshot is applied on the next socket. */
    fun detach() {
        resyncJob?.cancel()
        resyncJob = null
        channel = null
        store.detach()
    }

    /** Detaches only when [closed] is still the current socket. */
    fun detach(closed: RealtimeRequestChannel) {
        if (channel === closed) detach()
    }

    fun onPush(body: JsonObject) {
        if (channel == null) return
        val push = SessionMessages.decodePush(body) ?: return
        if (store.applyPush(push) == PushOutcome.SESSION_MISMATCH) resync()
    }

    private fun resync() {
        val current = channel ?: return
        if (resyncJob?.isActive == true) return
        failureCode = null
        resyncJob = scope.launch { resync(current) }
    }

    private suspend fun resync(current: RealtimeRequestChannel) {
        var attempt = 0
        while (channel === current) {
            val id = requestId()
            val reply = try {
                current.exchange(SessionMessages.resyncRequest(id, store.cursor))
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // The socket is gone; the reconnect lifecycle resyncs on the next one.
                return
            }
            val decoded = try {
                SessionMessages.decodeResyncReply(Json.parseToJsonElement(reply).jsonObject)
            } catch (_: Exception) {
                null
            }
            when {
                channel !== current -> return
                decoded is ResyncReply.Response && decoded.requestId == id -> {
                    store.applyResync(decoded.cursor, decoded.session)
                    return
                }
                decoded is ResyncReply.Rejected && decoded.code == SessionMessages.RESYNC_JOURNAL_UNAVAILABLE -> {
                    failureCode = decoded.code
                    delay(retryDelayMillis(attempt++))
                }
                else -> {
                    failureCode = (decoded as? ResyncReply.Rejected)?.code ?: INVALID_RESPONSE
                    return
                }
            }
        }
    }

    companion object {
        const val INVALID_RESPONSE = "invalid_resync_response"

        @OptIn(ExperimentalUuidApi::class)
        private fun newRequestId(): String = Uuid.random().toString()
    }
}
