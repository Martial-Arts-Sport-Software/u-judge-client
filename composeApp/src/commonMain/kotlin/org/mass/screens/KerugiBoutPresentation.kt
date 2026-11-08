package org.mass.screens

import org.mass.combat.KerugiAvailability
import org.mass.combat.KerugiCommandOutcome
import org.mass.locale.Localization
import org.mass.session.BoutPeriod
import org.mass.session.BoutTimer
import org.mass.session.Competitor
import org.mass.session.SessionPhase
import org.mass.session.SessionSnapshot

/** Localized texts of the Kerugi judge screen; every value comes from the server snapshot, never from the client. */
object KerugiBoutPresentation {
    fun boutTitle(snapshot: SessionSnapshot?): String =
        Localization.getString("kerugi_bout") + " №" + (snapshot?.boutNumber?.toString() ?: "—")

    fun state(phase: SessionPhase): String = Localization.getString("kerugi_state_${phase.wireName}")

    /** Category, court, state and seat in one line, for example «Юноши 12-14 лет, 45 кг · Площадка 1 · Идёт». */
    fun details(snapshot: SessionSnapshot?): String {
        if (snapshot == null) return Localization.getString("kerugi_no_bout")
        return listOfNotNull(
            snapshot.categoryTitle,
            Localization.getString("kerugi_court") + " " + snapshot.courtNumber,
            state(snapshot.state),
            snapshot.seat?.let { Localization.getString("kerugi_seat") + " " + it.number }
        ).joinToString(" · ")
    }

    /** «Раунд 1», «Перерыв», «Отдых перед дополнительным раундом» or «Дополнительный раунд» (server ADR-010 §5). */
    fun period(timer: BoutTimer?): String? = timer?.let {
        when {
            it.period == BoutPeriod.ROUND && it.goldenRound -> Localization.getString("kerugi_period_golden_round")
            it.period == BoutPeriod.ROUND -> Localization.getString("kerugi_period_round").replace("%s", it.round.toString())
            it.goldenRound -> Localization.getString("kerugi_period_golden_break")
            else -> Localization.getString("kerugi_period_break")
        }
    }

    /** The time left as `m:ss` at [serverNowMillis], rounded up so 0:00 shows only when the time is over. */
    fun clock(timer: BoutTimer?, serverNowMillis: Long): String? = timer?.let {
        val seconds = (it.remainingAt(serverNowMillis) + 999) / 1_000
        "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }

    fun sideName(competitor: Competitor): String = Localization.getString(
        if (competitor == Competitor.BLUE) "kerugi_side_blue" else "kerugi_side_red"
    )

    /** Score and Gamjeom of one side as the server reports them. */
    fun sideScore(snapshot: SessionSnapshot?, competitor: Competitor): String {
        val score = snapshot?.score ?: return "—"
        val (points, gamjeom) = if (competitor == Competitor.BLUE) {
            score.blue to score.blueGamjeom
        } else {
            score.red to score.redGamjeom
        }
        return "$points · ${Localization.getString("kerugi_gamjeom")} $gamjeom"
    }

    /** Server warnings, the confirmed disqualification and the result; empty when there is nothing to report. */
    fun notices(snapshot: SessionSnapshot?): List<String> {
        if (snapshot == null) return emptyList()
        val score = snapshot.score
        val warnings = score?.warnings.orEmpty().mapNotNull { warning ->
            when (warning.code) {
                "disqualification_pending" -> warning.competitor?.let {
                    Localization.getString("kerugi_warning_disqualification_pending").replace("%s", sideName(it))
                }
                else -> null
            }
        }
        val disqualified = score?.disqualified?.let {
            Localization.getString("kerugi_disqualified").replace("%s", sideName(it))
        }
        val result = snapshot.result?.let { result ->
            val winner = result.winner?.let { Localization.getString("kerugi_result_winner").replace("%s", sideName(it)) }
                ?: Localization.getString("kerugi_result_no_winner")
            // The final score needs no extra text; a reason the client does not know shows the winner only.
            when (result.reason) {
                "disqualification", "golden_round", "preference" ->
                    winner + " " + Localization.getString("kerugi_result_by_${result.reason}")
                else -> winner
            }
        }
        return warnings + listOfNotNull(disqualified, result)
    }

    fun availability(availability: KerugiAvailability, syncFailureCode: String?): String? = when (availability) {
        KerugiAvailability.AVAILABLE -> null
        KerugiAvailability.SYNCHRONIZING -> if (syncFailureCode != null && syncFailureCode != "journal_unavailable") {
            Localization.getString("kerugi_sync_failed")
        } else {
            Localization.getString("kerugi_availability_synchronizing")
        }
        else -> Localization.getString("kerugi_availability_${availability.name.lowercase()}")
    }

    fun tapFeedback(outcome: KerugiCommandOutcome?): String? = when (outcome) {
        is KerugiCommandOutcome.Pending -> Localization.getString("combat_event_pending")
        is KerugiCommandOutcome.Accepted -> Localization.getString("combat_event_accepted")
        is KerugiCommandOutcome.Rejected -> rejection(outcome.code) ?: Localization.getString("combat_event_rejected")
        KerugiCommandOutcome.Unavailable -> Localization.getString("combat_event_unavailable")
        KerugiCommandOutcome.StaleSession -> Localization.getString("kerugi_availability_stale_session")
        null -> null
    }

    fun attentionFeedback(outcome: KerugiCommandOutcome?): String? = when (outcome) {
        is KerugiCommandOutcome.Pending -> Localization.getString("kerugi_attention_pending")
        is KerugiCommandOutcome.Accepted -> Localization.getString("kerugi_attention_accepted")
        is KerugiCommandOutcome.Rejected -> Localization.getString("kerugi_attention_rejected") +
            (rejection(outcome.code)?.let { " $it" } ?: "")
        KerugiCommandOutcome.Unavailable -> Localization.getString("kerugi_attention_unavailable")
        KerugiCommandOutcome.StaleSession -> Localization.getString("kerugi_availability_stale_session")
        null -> null
    }

    fun readyFeedback(outcome: KerugiCommandOutcome?): String? = when (outcome) {
        is KerugiCommandOutcome.Pending -> Localization.getString("kerugi_ready_pending")
        is KerugiCommandOutcome.Accepted -> Localization.getString("kerugi_ready_accepted")
        is KerugiCommandOutcome.Rejected -> Localization.getString("kerugi_ready_rejected") +
            (rejection(outcome.code)?.let { " $it" } ?: "")
        KerugiCommandOutcome.Unavailable -> Localization.getString("kerugi_ready_unavailable")
        KerugiCommandOutcome.StaleSession -> Localization.getString("kerugi_availability_stale_session")
        null -> null
    }

    fun pendingCount(count: Int): String? =
        count.takeIf { it > 0 }?.let { Localization.getString("combat_pending_count").replace("%s", it.toString()) }

    private fun rejection(code: String): String? {
        val key = "combat_event_rejected_$code"
        return Localization.getString(key).takeUnless { it == key }
    }
}
