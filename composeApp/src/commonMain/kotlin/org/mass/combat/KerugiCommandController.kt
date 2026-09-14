package org.mass.combat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mass.connection.ConnectionState
import org.mass.connection.ConnectionStateStore
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

enum class KerugiParticipant { BLUE, RED }

enum class KerugiTarget { HEAD, BODY }

sealed interface KerugiCommandOutcome {
    data class Pending(val event: OutboxEvent) : KerugiCommandOutcome
    data class Accepted(val eventId: String) : KerugiCommandOutcome
    data class Rejected(val eventId: String, val code: String) : KerugiCommandOutcome
    data object Unavailable : KerugiCommandOutcome
}

/** Creates durable Kerugi commands only while an authenticated Kerugi session is running. */
class KerugiCommandController(
    private val connection: ConnectionStateStore,
    private val session: SessionStateStore,
    private val outbox: DurableEventOutbox,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val eventId: () -> String = { newEventId() }
) {
    var latestOutcome by mutableStateOf<KerugiCommandOutcome?>(null)
        private set

    val isAvailable: Boolean
        get() = activeSession() != null && connection.state is ConnectionState.ConnectedIdle

    fun submit(participant: KerugiParticipant, target: KerugiTarget): KerugiCommandOutcome {
        val session = activeSession() ?: return KerugiCommandOutcome.Unavailable.also { latestOutcome = it }
        val connected = connection.state as? ConnectionState.ConnectedIdle
            ?: return KerugiCommandOutcome.Unavailable.also { latestOutcome = it }
        val localTimestampMillis = nowMillis()
        val event = outbox.enqueueNew(
            eventId = eventId(),
            clientTimestampMillis = localTimestampMillis,
            clientTimestamp = Instant.fromEpochMilliseconds(
                localTimestampMillis + connected.clockOffsetMillis
            ).toString(),
            sessionId = session.sessionId,
            payload = buildJsonObject {
                put("type", "kerugi_score")
                put("participant", participant.name)
                put("target", target.name)
            }.toString()
        )
        return KerugiCommandOutcome.Pending(event).also { latestOutcome = it }
    }

    /** Updates feedback only for the latest physical Kerugi action, never for an unrelated replay. */
    fun recordTerminalOutcome(result: RealtimeCommandResult) {
        val pending = latestOutcome as? KerugiCommandOutcome.Pending ?: return
        latestOutcome = when (result) {
            is RealtimeCommandResult.Accepted -> {
                if (result.eventId == pending.event.eventId) KerugiCommandOutcome.Accepted(result.eventId) else pending
            }
            is RealtimeCommandResult.Rejected -> {
                if (result.eventId == pending.event.eventId) {
                    KerugiCommandOutcome.Rejected(result.eventId, result.code)
                } else {
                    pending
                }
            }
            RealtimeCommandResult.InvalidResponse -> pending
        }
    }

    private fun activeSession() = (session.state as? SessionState.Running)?.snapshot
        ?.takeIf { it.discipline == Disciplines.KERUGI }

    @OptIn(ExperimentalUuidApi::class)
    private companion object {
        fun newEventId(): String = Uuid.random().toString()
    }
}
