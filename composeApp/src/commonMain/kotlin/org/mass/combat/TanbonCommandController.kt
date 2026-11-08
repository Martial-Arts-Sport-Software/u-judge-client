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
import org.mass.session.AuditContext
import org.mass.session.SessionPhase
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

/**
 * Creates durable `tanbon_score_command`s (server ADR-011 §3) for a seated judge of the server's Tanbon bout, only while
 * connected, synchronized and `running`. Every command copies its audit context from the snapshot current at the tap;
 * `CROSS` carries `competitor: null`.
 */
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

    /** Taps of any session still waiting for a server reply. */
    var pendingCount by mutableStateOf(outbox.pendingEvents().size)
        private set

    fun submit(action: TanbonAction): TanbonCommandOutcome {
        val session = activeSession() ?: return TanbonCommandOutcome.Unavailable.also { latestOutcome = it }
        val connected = connection.state as? ConnectionState.ConnectedIdle
            ?: return TanbonCommandOutcome.Unavailable.also { latestOutcome = it }
        val localTimestampMillis = nowMillis()
        val audit = requireNotNull(session.auditContext())
        val event = outbox.enqueueNew(
            eventId = eventId(),
            clientTimestampMillis = localTimestampMillis,
            clientTimestamp = Instant.fromEpochMilliseconds(
                localTimestampMillis + connected.clockOffsetMillis
            ).toString(),
            sessionId = audit.sessionId,
            payload = action.payload(audit)
        )
        pendingCount = outbox.pendingEvents().size
        return TanbonCommandOutcome.Pending(event).also { outcome ->
            latestOutcome = outcome
            dispatcher()?.dispatch()
        }
    }

    /** Updates feedback only for the latest physical Tanbon action, never for an unrelated replay. */
    fun recordTerminalOutcome(result: RealtimeCommandResult) {
        pendingCount = outbox.pendingEvents().size
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
            is RealtimeCommandResult.Retry,
            RealtimeCommandResult.InvalidResponse -> pending
        }
    }

    /** A synchronized, running Tanbon bout in which this device has a seat (server ADR-009 §10.5). */
    private fun activeSession() = session.snapshot?.takeIf {
        session.isSynchronized && it.state == SessionPhase.RUNNING && it.discipline == Disciplines.TANBON && it.seat != null
    }

    private fun TanbonAction.payload(audit: AuditContext) = buildJsonObject {
        put("type", COMMAND)
        put("competitionId", audit.competitionId)
        put("peerId", audit.peerId)
        put("courtId", audit.courtId)
        put("bracketId", audit.bracketId)
        put("sessionId", audit.sessionId)
        put("judgeId", audit.judgeId)
        put("deviceId", audit.deviceId)
        val (competitor, area) = when (this@payload) {
            TanbonAction.BlueHead -> "BLUE" to "HEAD"
            TanbonAction.BlueBody -> "BLUE" to "BODY"
            TanbonAction.RedHead -> "RED" to "HEAD"
            TanbonAction.RedBody -> "RED" to "BODY"
            TanbonAction.Cross -> null to "CROSS"
        }
        put("competitor", competitor)
        put("area", area)
    }.toString()

    @OptIn(ExperimentalUuidApi::class)
    private companion object {
        const val COMMAND = "tanbon_score_command"

        fun newEventId(): String = Uuid.random().toString()
    }
}
