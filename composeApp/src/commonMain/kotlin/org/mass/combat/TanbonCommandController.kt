package org.mass.combat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mass.connection.ConnectionState
import org.mass.connection.ConnectionStateStore
import org.mass.connection.RealtimeCommandDispatcher
import org.mass.connection.RealtimeCommandResult
import org.mass.enums.Disciplines
import org.mass.session.SessionState
import org.mass.session.SessionStateStore
import org.mass.transport.DurableEventOutbox
import org.mass.transport.OutboxEvent
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface TanbonAction {
    data object BlueHead : TanbonAction
    data object BlueBody : TanbonAction
    data object RedHead : TanbonAction
    data object RedBody : TanbonAction
    data object Cross : TanbonAction
}

sealed interface TanbonCommandOutcome {
    data class Pending(val event: OutboxEvent) : TanbonCommandOutcome
    data class Accepted(val eventId: String) : TanbonCommandOutcome
    data class Rejected(val eventId: String, val code: String) : TanbonCommandOutcome
    data object Unavailable : TanbonCommandOutcome
}

/** Creates durable Tanbon commands only while an authenticated Tanbon session is running. */
class TanbonCommandController(
    private val connection: ConnectionStateStore,
    private val session: SessionStateStore,
    private val outbox: DurableEventOutbox,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val eventId: () -> String = { newEventId() },
    private val dispatcher: () -> RealtimeCommandDispatcher? = { null }
) {
    var latestOutcome by mutableStateOf<TanbonCommandOutcome?>(null)
        private set

    val isAvailable: Boolean
        get() = activeSession() != null && connection.state is ConnectionState.ConnectedIdle

    fun submit(action: TanbonAction): TanbonCommandOutcome {
        val session = activeSession() ?: return TanbonCommandOutcome.Unavailable.also { latestOutcome = it }
        val connected = connection.state as? ConnectionState.ConnectedIdle
            ?: return TanbonCommandOutcome.Unavailable.also { latestOutcome = it }
        val localTimestampMillis = nowMillis()
        val event = outbox.enqueueNew(
            eventId = eventId(),
            clientTimestampMillis = localTimestampMillis,
            clientTimestamp = Instant.fromEpochMilliseconds(
                localTimestampMillis + connected.clockOffsetMillis
            ).toString(),
            sessionId = session.sessionId,
            payload = action.payload()
        )
        return TanbonCommandOutcome.Pending(event).also { outcome ->
            latestOutcome = outcome
            dispatcher()?.dispatch(event, ::recordTerminalOutcome)
        }
    }

    /** Updates feedback only for the latest physical Tanbon action, never for an unrelated replay. */
    fun recordTerminalOutcome(result: RealtimeCommandResult) {
        val pending = latestOutcome as? TanbonCommandOutcome.Pending ?: return
        latestOutcome = when (result) {
            is RealtimeCommandResult.Accepted -> {
                if (result.eventId == pending.event.eventId) TanbonCommandOutcome.Accepted(result.eventId) else pending
            }
            is RealtimeCommandResult.Rejected -> {
                if (result.eventId == pending.event.eventId) {
                    TanbonCommandOutcome.Rejected(result.eventId, result.code)
                } else {
                    pending
                }
            }
            RealtimeCommandResult.InvalidResponse -> pending
        }
    }

    private fun activeSession() = (session.state as? SessionState.Running)?.snapshot
        ?.takeIf { it.discipline == Disciplines.TANBON }

    private fun TanbonAction.payload() = buildJsonObject {
        put("type", "tanbon_score")
        when (this@payload) {
            TanbonAction.BlueHead -> {
                put("participant", "BLUE")
                put("target", "HEAD")
            }
            TanbonAction.BlueBody -> {
                put("participant", "BLUE")
                put("target", "BODY")
            }
            TanbonAction.RedHead -> {
                put("participant", "RED")
                put("target", "HEAD")
            }
            TanbonAction.RedBody -> {
                put("participant", "RED")
                put("target", "BODY")
            }
            TanbonAction.Cross -> {
                put("participant", "NEUTRAL")
                put("target", "CROSS")
            }
        }
    }.toString()

    @OptIn(ExperimentalUuidApi::class)
    private companion object {
        fun newEventId(): String = Uuid.random().toString()
    }
}
