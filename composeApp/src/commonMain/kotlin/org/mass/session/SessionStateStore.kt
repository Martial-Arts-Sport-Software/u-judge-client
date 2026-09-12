package org.mass.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.mass.enums.Disciplines

/** Immutable server-owned session data used by later command and rating flows. */
data class SessionSnapshot(
    val sessionId: String,
    val discipline: Disciplines,
    val boutLabel: String,
    val blueParticipantLabel: String,
    val redParticipantLabel: String
) {
    init {
        require(sessionId.isNotBlank())
        require(boutLabel.isNotBlank())
        require(blueParticipantLabel.isNotBlank())
        require(redParticipantLabel.isNotBlank())
    }
}

enum class SessionPhase {
    PREPARED,
    RUNNING,
    PAUSED,
    COMPLETED
}

sealed interface SessionState {
    data object NoActiveSession : SessionState
    data class Prepared(val snapshot: SessionSnapshot) : SessionState
    data class Running(val snapshot: SessionSnapshot) : SessionState
    data class Paused(val snapshot: SessionSnapshot) : SessionState
    data class Completed(val snapshot: SessionSnapshot) : SessionState
}

/** Server snapshots are authoritative; navigation never changes this state. */
class SessionStateStore {
    var state by mutableStateOf<SessionState>(SessionState.NoActiveSession)
        private set

    fun update(snapshot: SessionSnapshot, phase: SessionPhase) {
        state = when (phase) {
            SessionPhase.PREPARED -> SessionState.Prepared(snapshot)
            SessionPhase.RUNNING -> SessionState.Running(snapshot)
            SessionPhase.PAUSED -> SessionState.Paused(snapshot)
            SessionPhase.COMPLETED -> SessionState.Completed(snapshot)
        }
    }

    fun clear() {
        state = SessionState.NoActiveSession
    }
}
