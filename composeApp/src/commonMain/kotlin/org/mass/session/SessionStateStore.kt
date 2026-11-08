package org.mass.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** What [SessionStateStore.applyPush] did with a server push. */
enum class PushOutcome {
    APPLIED,

    /** The cursor is not greater than the held one: a duplicate or reordered push. */
    STALE,

    /** An update for a session other than the held one: the client must resync. */
    SESSION_MISMATCH,

    /** No snapshot was applied on this socket yet. */
    IGNORED
}

/**
 * Server snapshots are authoritative; navigation never changes this state. The client replaces its whole session state
 * with every `resync_response` and `session_snapshot` and applies an update only when its cursor is newer than the held
 * one (server ADR-009 §3-§5, `CLI-064`).
 */
class SessionStateStore {
    /** The court's current bout, or null when the court has no bout yet. */
    var snapshot by mutableStateOf<SessionSnapshot?>(null)
        private set

    /** Cursor of the last snapshot or update applied; null after an app start. */
    var cursor by mutableStateOf<Long?>(null)
        private set

    /** A snapshot was applied on the current socket; combat controls stay disabled until then. */
    var isSynchronized by mutableStateOf(false)
        private set

    /** Replaces the whole session state with a `resync_response`, even when its cursor is lower. */
    fun applyResync(cursor: Long, session: SessionSnapshot?) {
        this.cursor = cursor
        snapshot = session
        isSynchronized = true
    }

    fun applyPush(message: SessionPush): PushOutcome {
        if (!isSynchronized) return PushOutcome.IGNORED
        val held = cursor
        if (held != null && message.cursor <= held) return PushOutcome.STALE
        val current = snapshot
        when (message) {
            is SessionPush.Snapshot -> snapshot = message.session
            is SessionPush.StateUpdated -> {
                if (current?.sessionId != message.sessionId) return PushOutcome.SESSION_MISMATCH
                snapshot = current.copy(state = message.state, result = message.result, timer = message.timer ?: current.timer)
            }
            is SessionPush.TimerUpdated -> {
                if (current?.sessionId != message.sessionId) return PushOutcome.SESSION_MISMATCH
                snapshot = current.copy(timer = message.timer)
            }
            is SessionPush.ScoreUpdated -> {
                if (current?.sessionId != message.sessionId) return PushOutcome.SESSION_MISMATCH
                snapshot = current.copy(score = message.score)
            }
        }
        cursor = message.cursor
        return PushOutcome.APPLIED
    }

    /** The socket was lost or replaced: the held state stays visible but no longer enables controls. */
    fun detach() {
        isSynchronized = false
    }

    /** Forgets the session together with the pairing. */
    fun clear() {
        snapshot = null
        cursor = null
        isSynchronized = false
    }
}
