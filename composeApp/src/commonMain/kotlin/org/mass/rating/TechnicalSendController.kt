package org.mass.rating

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mass.connection.ConnectionState
import org.mass.connection.ConnectionStateStore
import org.mass.connection.RealtimeCommandDispatcher
import org.mass.connection.RealtimeCommandResult
import org.mass.enums.Disciplines
import org.mass.session.SessionPhase
import org.mass.session.SessionStateStore
import org.mass.session.TechnicalSnapshot
import org.mass.transport.DurableEventOutbox
import org.mass.transport.OutboxEvent
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Why `Send` is disabled, in the order of server ADR-011 §7; [AVAILABLE] when it is enabled (`CLI-048`). */
enum class TechnicalAvailability {
    AVAILABLE,
    NOT_CONNECTED,
    SYNCHRONIZING,
    NO_PERFORMANCE,
    OTHER_DISCIPLINE,
    NOT_SEATED,
    NOT_RUNNING,
    ALREADY_SENT,
    INVALID_SHAPE
}

sealed interface TechnicalSendOutcome {
    val sessionId: String

    /** Durable and waiting for the server; survives a disconnect and an app kill (`CLI-052`). */
    data class Pending(val event: OutboxEvent) : TechnicalSendOutcome {
        override val sessionId: String get() = event.sessionId
    }

    /** Recorded by the server: the rating is read-only (`CLI-051`). */
    data class Accepted(override val sessionId: String, val eventId: String, val totalTenths: Int) : TechnicalSendOutcome

    /** A terminal rejection; the payload stays in the outbox journal (`CLI-053`). */
    data class Rejected(override val sessionId: String, val eventId: String, val code: String) : TechnicalSendOutcome
}

/**
 * The final `Send` of a technical rating (server ADR-011 §7): one durable `technical_score_send_command` with the raw
 * criteria, the judge's deduction and the local total in tenths, created only for a seated judge of the server's running
 * performance of the screen's discipline. Feedback changes only on the server reply for its own event ID.
 */
class TechnicalSendController(
    private val connection: ConnectionStateStore,
    private val session: SessionStateStore,
    private val outbox: DurableEventOutbox,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val eventId: () -> String = { newEventId() },
    private val dispatcher: () -> RealtimeCommandDispatcher? = { null }
) {
    var latestOutcome by mutableStateOf<TechnicalSendOutcome?>(restoredOutcome())
        private set

    fun availability(discipline: Disciplines, vector: TechnicalVector): TechnicalAvailability {
        if (connection.state !is ConnectionState.ConnectedIdle) return TechnicalAvailability.NOT_CONNECTED
        if (!session.isSynchronized) return TechnicalAvailability.SYNCHRONIZING
        val performance = session.technical ?: return TechnicalAvailability.NO_PERFORMANCE
        return when {
            performance.discipline?.discipline != discipline -> TechnicalAvailability.OTHER_DISCIPLINE
            performance.seat == null -> TechnicalAvailability.NOT_SEATED
            performance.state != SessionPhase.RUNNING -> TechnicalAvailability.NOT_RUNNING
            isFinal(performance) -> TechnicalAvailability.ALREADY_SENT
            !vector.fits(performance.techniqueCriteria, performance.presentationCriteria) -> TechnicalAvailability.INVALID_SHAPE
            else -> TechnicalAvailability.AVAILABLE
        }
    }

    /** True once this device's Send of the current performance is pending or acknowledged: the rating is read-only. */
    fun isReadOnly(): Boolean = session.technical?.let(::isFinal) ?: false

    /** The total the server recorded for this device, from the snapshot or the latest ACK. */
    fun sentTotalTenths(): Int? {
        val performance = session.technical ?: return null
        return performance.sent?.totalTenths
            ?: (latestOutcome as? TechnicalSendOutcome.Accepted)?.takeIf { it.sessionId == performance.sessionId }?.totalTenths
    }

    /** Called after the judge confirmed the irreversible Send (`CLI-049`). */
    fun send(discipline: Disciplines, vector: TechnicalVector): TechnicalSendOutcome? {
        if (availability(discipline, vector) != TechnicalAvailability.AVAILABLE) return null
        val connected = connection.state as ConnectionState.ConnectedIdle
        val performance = requireNotNull(session.technical)
        val audit = requireNotNull(performance.auditContext())
        val localTimestampMillis = nowMillis()
        val event = outbox.enqueueNew(
            eventId = eventId(),
            clientTimestampMillis = localTimestampMillis,
            clientTimestamp = Instant.fromEpochMilliseconds(localTimestampMillis + connected.clockOffsetMillis).toString(),
            sessionId = audit.sessionId,
            payload = buildJsonObject {
                put("type", COMMAND)
                put("competitionId", audit.competitionId)
                put("peerId", audit.peerId)
                put("courtId", audit.courtId)
                put("bracketId", audit.bracketId)
                put("sessionId", audit.sessionId)
                put("judgeId", audit.judgeId)
                put("deviceId", audit.deviceId)
                put("discipline", performance.disciplineCode)
                put("techniqueTenths", JsonArray(vector.techniqueTenths.map(::JsonPrimitive)))
                put("presentationTenths", JsonArray(vector.presentationTenths.map(::JsonPrimitive)))
                put("deductionTenths", vector.deductionTenths)
                put("totalTenths", vector.totalTenths)
            }.toString()
        )
        val outcome = TechnicalSendOutcome.Pending(event)
        latestOutcome = outcome
        dispatcher()?.dispatch()
        return outcome
    }

    /** Updates feedback only for the matching Send, never for an unrelated replay. */
    fun recordTerminalOutcome(result: RealtimeCommandResult) {
        val pending = latestOutcome as? TechnicalSendOutcome.Pending ?: return
        latestOutcome = when (result) {
            is RealtimeCommandResult.Accepted -> if (result.eventId == pending.event.eventId) {
                TechnicalSendOutcome.Accepted(pending.sessionId, result.eventId, totalOf(pending.event))
            } else {
                pending
            }
            is RealtimeCommandResult.Rejected -> if (result.eventId == pending.event.eventId) {
                TechnicalSendOutcome.Rejected(pending.sessionId, result.eventId, result.code)
            } else {
                pending
            }
            is RealtimeCommandResult.Retry,
            RealtimeCommandResult.InvalidResponse -> pending
        }
    }

    private fun isFinal(performance: TechnicalSnapshot): Boolean {
        if (performance.sent != null) return true
        if (pendingSends().any { it.sessionId == performance.sessionId }) return true
        return when (val outcome = latestOutcome) {
            is TechnicalSendOutcome.Accepted -> outcome.sessionId == performance.sessionId
            // The server already holds another Send of this seat.
            is TechnicalSendOutcome.Rejected -> outcome.sessionId == performance.sessionId && outcome.code == ALREADY_SENT_CODE
            else -> false
        }
    }

    private fun pendingSends(): List<OutboxEvent> = outbox.pendingEvents().filter(::isSend)

    /** After an app restart a pending Send is still the latest action of its performance. */
    private fun restoredOutcome(): TechnicalSendOutcome? = pendingSends().lastOrNull()?.let(TechnicalSendOutcome::Pending)
        ?: outbox.rejectedEvents().lastOrNull { isSend(it.event) }?.let {
            TechnicalSendOutcome.Rejected(it.event.sessionId, it.event.eventId, it.reason)
        }

    private fun isSend(event: OutboxEvent): Boolean = runCatching {
        Json.parseToJsonElement(event.payload).jsonObject["type"]?.jsonPrimitive?.content == COMMAND
    }.getOrDefault(false)

    private fun totalOf(event: OutboxEvent): Int =
        Json.parseToJsonElement(event.payload).jsonObject.getValue("totalTenths").jsonPrimitive.content.toInt()

    companion object {
        const val COMMAND = "technical_score_send_command"
        const val ALREADY_SENT_CODE = "score_already_sent"

        @OptIn(ExperimentalUuidApi::class)
        private fun newEventId(): String = Uuid.random().toString()
    }
}
