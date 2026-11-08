package org.mass.session

import org.mass.enums.Disciplines

/** Bout states of server `SES-001`; only the operator changes them on the desktop (server ADR-009 §2). */
enum class SessionPhase(val wireName: String) {
    PREPARED("prepared"),
    RUNNING("running"),
    PAUSED("paused"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        fun fromWire(value: String): SessionPhase? = entries.firstOrNull { it.wireName == value }
    }
}

/** Blue is the upper slot of the bracket bout and red the lower one. */
enum class Competitor { BLUE, RED }

data class Athlete(val fullName: String, val city: String?)

/** One side of the bout; [label] is the server-formatted display text, shown as is (`CLI-024`). */
data class BoutSide(val entryId: String, val label: String, val athletes: List<Athlete>)

data class BoutSettings(val judgeCount: Int, val quorum: Int, val coincidenceWindowMillis: Long)

/** The receiving device's seat; it belongs to the paired device and survives a reconnect. */
data class JudgeSeat(val number: Int, val judgeId: String, val deviceId: String)

data class ScoreWarning(val code: String, val competitor: Competitor?, val gamjeomCount: Int?)

/** The authoritative score projection; the client displays it and never computes quorum or totals (`CLI-034`). */
data class KerugiScore(
    val blue: Int,
    val red: Int,
    val blueGamjeom: Int,
    val redGamjeom: Int,
    val warnings: List<ScoreWarning>,
    val disqualified: Competitor?
)

data class BoutResult(val winner: Competitor?, val reason: String)

/** A round or the break before the next round (server ADR-010 §2). */
enum class BoutPeriod(val wireName: String) {
    ROUND("round"),
    BREAK("break");

    companion object {
        fun fromWire(value: String): BoutPeriod? = entries.firstOrNull { it.wireName == value }
    }
}

/**
 * The bout clock of server ADR-010 §5. [round] is the round being fought, or for a break the round after it; while the
 * clock runs, [remainingMillis] is the time left at [runningSinceMillis] (server time), so the client counts down itself.
 */
data class BoutTimer(
    val period: BoutPeriod,
    val round: Int,
    val goldenRound: Boolean,
    val durationMillis: Long,
    val remainingMillis: Long,
    val running: Boolean,
    val runningSinceMillis: Long?
) {
    /** The time left at [serverNowMillis], never below zero and never above the time left when the clock started. */
    fun remainingAt(serverNowMillis: Long): Long {
        val since = runningSinceMillis?.takeIf { running } ?: return remainingMillis
        return (remainingMillis - (serverNowMillis - since)).coerceIn(0, remainingMillis)
    }
}

/** Audit IDs copied into every command from the snapshot current at the tap (server ADR-009 §6). */
data class AuditContext(
    val competitionId: String,
    val peerId: String,
    val courtId: String,
    val bracketId: String,
    val sessionId: String,
    val judgeId: String,
    val deviceId: String
)

/** The court's current bout as the server personalised it for this device (server ADR-009 §3). */
data class SessionSnapshot(
    val sessionId: String,
    val disciplineCode: String,
    val competitionId: String,
    val peerId: String,
    val courtId: String,
    val courtNumber: Int,
    val bracketId: String,
    val categoryTitle: String,
    val boutNumber: Int,
    val state: SessionPhase,
    val blue: BoutSide,
    val red: BoutSide,
    val settings: BoutSettings,
    val seat: JudgeSeat?,
    val score: KerugiScore?,
    val result: BoutResult?,
    /** Null only for a server without the clock of ADR-010. */
    val timer: BoutTimer? = null
) {
    init {
        require(sessionId.isNotBlank())
    }

    val discipline: Disciplines?
        get() = when (disciplineCode) {
            "kerugi" -> Disciplines.KERUGI
            "tanbon" -> Disciplines.TANBON
            else -> null
        }

    /** Null when this device has no seat in the bout: it then receives the bout read-only. */
    fun auditContext(): AuditContext? = seat?.let {
        AuditContext(
            competitionId = competitionId,
            peerId = peerId,
            courtId = courtId,
            bracketId = bracketId,
            sessionId = sessionId,
            judgeId = it.judgeId,
            deviceId = it.deviceId
        )
    }
}
