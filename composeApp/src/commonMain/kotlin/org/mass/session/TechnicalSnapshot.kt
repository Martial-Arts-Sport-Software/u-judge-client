package org.mass.session

import org.mass.enums.Disciplines

/** The seven technical disciplines of server ADR-011 §4 with their wire codes and the client's screens. */
enum class TechnicalDiscipline(val code: String, val discipline: Disciplines) {
    HOSINSOOL("hosinsool", Disciplines.HOSINSOOL),
    PAIR_FREESTYLE("pair_freestyle", Disciplines.FREESTYLE_PAIR),
    GROUP_FREESTYLE("group_freestyle", Disciplines.FREESTYLE_GROUP),
    SWORD("sword", Disciplines.FREESTYLE_SWORD),
    POLE("pole", Disciplines.FREESTYLE_POLE),
    PAIRED_NUNCHAKU("paired_nunchaku", Disciplines.FREESTYLE_NUNCHAKU),
    PAIRED_FANS("paired_fans", Disciplines.FREESTYLE_FANS);

    companion object {
        fun ofCode(code: String): TechnicalDiscipline? = entries.firstOrNull { it.code == code }

        fun of(discipline: Disciplines): TechnicalDiscipline? = entries.firstOrNull { it.discipline == discipline }
    }
}

/** The device's own acknowledged Send in this performance; the client shows it read-only (`CLI-051`). */
data class SentVector(
    val eventId: String,
    val techniqueTenths: List<Int>,
    val presentationTenths: List<Int>,
    val deductionTenths: Int,
    val totalTenths: Int
)

data class TechnicalResult(
    val totalTenths: Int,
    val aggregateTenths: Int,
    val adjustmentTenths: Int,
    val divisor: Int,
    val formula: String
)

/** Seats that sent, and the server result once the performance is completed. */
data class TechnicalScore(val submittedSeats: List<Int>, val result: TechnicalResult?)

/** The court's current technical performance as the server personalised it for this device (server ADR-011 §6). */
data class TechnicalSnapshot(
    val sessionId: String,
    val disciplineCode: String,
    val competitionId: String,
    val peerId: String,
    val courtId: String,
    val courtNumber: Int,
    val bracketId: String,
    val categoryTitle: String,
    val performanceNumber: Int,
    val state: SessionPhase,
    val entry: BoutSide,
    val judgeCount: Int,
    val formula: String,
    val techniqueCriteria: Int,
    val presentationCriteria: Int,
    val deductionStepTenths: Int,
    val seat: JudgeSeat?,
    val sent: SentVector?,
    val score: TechnicalScore
) {
    init {
        require(sessionId.isNotBlank())
    }

    val discipline: TechnicalDiscipline?
        get() = TechnicalDiscipline.ofCode(disciplineCode)

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

/** The current session decoded by its `discipline` (server ADR-011 §6). */
sealed interface CourtSession {
    val sessionId: String

    data class Bout(val snapshot: SessionSnapshot) : CourtSession {
        override val sessionId: String get() = snapshot.sessionId
    }

    data class Technical(val snapshot: TechnicalSnapshot) : CourtSession {
        override val sessionId: String get() = snapshot.sessionId
    }

    /** A discipline this client does not know: its controls stay disabled. */
    data class Unknown(override val sessionId: String, val disciplineCode: String) : CourtSession
}
