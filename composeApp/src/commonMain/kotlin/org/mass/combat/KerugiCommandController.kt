package org.mass.combat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.JsonObjectBuilder
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

enum class KerugiParticipant { BLUE, RED }

enum class KerugiTarget { HEAD, BODY }

sealed interface KerugiCommandOutcome {
    data class Pending(val event: OutboxEvent) : KerugiCommandOutcome
    data class Accepted(val eventId: String) : KerugiCommandOutcome
    data class Rejected(val eventId: String, val code: String) : KerugiCommandOutcome
    data object Unavailable : KerugiCommandOutcome
}

/** Why the controls are disabled, in the order of server ADR-009 §10.5; [AVAILABLE] when they are enabled. */
enum class KerugiAvailability {
    AVAILABLE,
    NOT_CONNECTED,
    SYNCHRONIZING,
    NO_BOUT,
    OTHER_DISCIPLINE,
    NOT_SEATED,
    NOT_RUNNING
}

/**
 * Creates durable Kerugi commands for a seated judge of the server bout. Scoring is enabled only while connected, with a
 * snapshot applied on this socket, a Kerugi session in `running` and a seat; the attention signal also in `prepared` and
 * `paused`. Every command copies its audit context from the snapshot current at the tap, and its feedback changes only
 * on the server reply for its own event ID.
 */
class KerugiCommandController(
    private val connection: ConnectionStateStore,
    private val session: SessionStateStore,
    private val outbox: DurableEventOutbox,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val eventId: () -> String = { newEventId() },
    private val dispatcher: () -> RealtimeCommandDispatcher? = { null }
) {
    /** Feedback for the latest scoring tap. */
    var latestOutcome by mutableStateOf<KerugiCommandOutcome?>(null)
        private set

    /** Feedback for the latest attention signal. */
    var latestAttention by mutableStateOf<KerugiCommandOutcome?>(null)
        private set

    /** Taps and signals of any session still waiting for a server reply, including ones restored after a restart. */
    var pendingCount by mutableStateOf(outbox.pendingEvents().size)
        private set

    val availability: KerugiAvailability
        get() = availability(SCORE_PHASES)

    val isAvailable: Boolean
        get() = availability == KerugiAvailability.AVAILABLE

    val isAttentionAvailable: Boolean
        get() = availability(ATTENTION_PHASES) == KerugiAvailability.AVAILABLE

    fun submit(participant: KerugiParticipant, target: KerugiTarget): KerugiCommandOutcome {
        val outcome = enqueue(SCORE_PHASES, SCORE_COMMAND) {
            put("competitor", participant.name)
            put("area", target.name)
        }
        latestOutcome = outcome
        return outcome
    }

    /** The judge's warning/attention signal, separate from scoring (`CLI-037`). */
    fun signalAttention(): KerugiCommandOutcome {
        val outcome = enqueue(ATTENTION_PHASES, ATTENTION_COMMAND) {}
        latestAttention = outcome
        return outcome
    }

    /** Updates feedback only for the matching latest action, never for an unrelated replay. */
    fun recordTerminalOutcome(result: RealtimeCommandResult) {
        pendingCount = outbox.pendingEvents().size
        latestOutcome = latestOutcome.settledBy(result)
        latestAttention = latestAttention.settledBy(result)
    }

    private fun enqueue(
        phases: Set<SessionPhase>,
        type: String,
        fields: JsonObjectBuilder.() -> Unit
    ): KerugiCommandOutcome {
        if (availability(phases) != KerugiAvailability.AVAILABLE) return KerugiCommandOutcome.Unavailable
        val connected = connection.state as ConnectionState.ConnectedIdle
        val audit = requireNotNull(session.snapshot?.auditContext())
        val localTimestampMillis = nowMillis()
        val event = outbox.enqueueNew(
            eventId = eventId(),
            clientTimestampMillis = localTimestampMillis,
            clientTimestamp = Instant.fromEpochMilliseconds(
                localTimestampMillis + connected.clockOffsetMillis
            ).toString(),
            sessionId = audit.sessionId,
            payload = buildJsonObject {
                put("type", type)
                putAudit(audit)
                fields()
            }.toString()
        )
        pendingCount = outbox.pendingEvents().size
        dispatcher()?.dispatch()
        return KerugiCommandOutcome.Pending(event)
    }

    private fun availability(phases: Set<SessionPhase>): KerugiAvailability {
        if (connection.state !is ConnectionState.ConnectedIdle) return KerugiAvailability.NOT_CONNECTED
        if (!session.isSynchronized) return KerugiAvailability.SYNCHRONIZING
        val snapshot = session.snapshot ?: return KerugiAvailability.NO_BOUT
        return when {
            snapshot.discipline != Disciplines.KERUGI -> KerugiAvailability.OTHER_DISCIPLINE
            snapshot.seat == null -> KerugiAvailability.NOT_SEATED
            snapshot.state !in phases -> KerugiAvailability.NOT_RUNNING
            else -> KerugiAvailability.AVAILABLE
        }
    }

    private fun KerugiCommandOutcome?.settledBy(result: RealtimeCommandResult): KerugiCommandOutcome? {
        val pending = this as? KerugiCommandOutcome.Pending ?: return this
        return when (result) {
            is RealtimeCommandResult.Accepted ->
                if (result.eventId == pending.event.eventId) KerugiCommandOutcome.Accepted(result.eventId) else pending
            is RealtimeCommandResult.Rejected ->
                if (result.eventId == pending.event.eventId) {
                    KerugiCommandOutcome.Rejected(result.eventId, result.code)
                } else {
                    pending
                }
            is RealtimeCommandResult.Retry,
            RealtimeCommandResult.InvalidResponse -> pending
        }
    }

    private fun JsonObjectBuilder.putAudit(audit: AuditContext) {
        put("competitionId", audit.competitionId)
        put("peerId", audit.peerId)
        put("courtId", audit.courtId)
        put("bracketId", audit.bracketId)
        put("sessionId", audit.sessionId)
        put("judgeId", audit.judgeId)
        put("deviceId", audit.deviceId)
    }

    private companion object {
        const val SCORE_COMMAND = "kerugi_score_command"
        const val ATTENTION_COMMAND = "judge_attention_command"
        val SCORE_PHASES = setOf(SessionPhase.RUNNING)
        val ATTENTION_PHASES = setOf(SessionPhase.PREPARED, SessionPhase.RUNNING, SessionPhase.PAUSED)

        @OptIn(ExperimentalUuidApi::class)
        fun newEventId(): String = Uuid.random().toString()
    }
}
