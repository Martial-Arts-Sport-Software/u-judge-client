package org.mass.screens

import org.mass.State
import org.mass.combat.KerugiAvailability
import org.mass.combat.KerugiCommandOutcome
import org.mass.session.BoutResult
import org.mass.session.Competitor
import org.mass.session.KerugiScore
import org.mass.session.ScoreWarning
import org.mass.session.SessionFixtures
import org.mass.session.SessionPhase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KerugiBoutPresentationTest {
    private var locale = ""

    @BeforeTest
    fun useRussian() {
        locale = State.currentLocale
        State.currentLocale = "ru"
    }

    @AfterTest
    fun restoreLocale() {
        State.currentLocale = locale
    }

    @Test
    fun boutNumberCategoryCourtStateAndSeatComeFromTheSnapshot() {
        val snapshot = SessionFixtures.snapshot(boutNumber = 12, state = SessionPhase.PAUSED)

        assertEquals("Поединок №12", KerugiBoutPresentation.boutTitle(snapshot))
        assertEquals("Поединок №—", KerugiBoutPresentation.boutTitle(null))
        assertEquals(
            "Юноши 12-14 лет, 45 кг · Площадка 1 · Пауза · Место судьи 1",
            KerugiBoutPresentation.details(snapshot)
        )
        assertEquals("На площадке ещё не начат поединок", KerugiBoutPresentation.details(null))
        assertEquals("3 · Гамджом 0", KerugiBoutPresentation.sideScore(snapshot, Competitor.BLUE))
        assertEquals("1 · Гамджом 1", KerugiBoutPresentation.sideScore(snapshot, Competitor.RED))
    }

    @Test
    fun everyStateAndBlockedReasonHasARussianText() {
        SessionPhase.entries.forEach {
            assertEquals(false, KerugiBoutPresentation.state(it).startsWith("kerugi_"))
        }
        KerugiAvailability.entries.filter { it != KerugiAvailability.AVAILABLE }.forEach {
            assertEquals(false, KerugiBoutPresentation.availability(it, null)!!.startsWith("kerugi_"))
        }
        assertNull(KerugiBoutPresentation.availability(KerugiAvailability.AVAILABLE, null))
        assertEquals(
            "Сервер не вернул состояние поединка. Запросим его снова после переподключения.",
            KerugiBoutPresentation.availability(KerugiAvailability.SYNCHRONIZING, "invalid_resync_request")
        )
    }

    @Test
    fun feedbackSaysRecordedOnlyAfterTheAckAndNamesTheRejection() {
        assertEquals("Сервер записал нажатие.", KerugiBoutPresentation.tapFeedback(KerugiCommandOutcome.Accepted("e")))
        assertEquals(
            "Отклонено: поединок уже завершён.",
            KerugiBoutPresentation.tapFeedback(KerugiCommandOutcome.Rejected("e", "session_closed"))
        )
        assertEquals(
            "Сервер отклонил это событие.",
            KerugiBoutPresentation.tapFeedback(KerugiCommandOutcome.Rejected("e", "unknown_code"))
        )
        assertEquals("Ожидают подтверждения сервера: 2", KerugiBoutPresentation.pendingCount(2))
        assertNull(KerugiBoutPresentation.pendingCount(0))
    }

    @Test
    fun warningsDisqualificationAndResultAreShown() {
        val snapshot = SessionFixtures.snapshot(
            score = KerugiScore(5, 1, 0, 10, listOf(ScoreWarning("disqualification_pending", Competitor.RED, 10)), null)
        ).copy(result = BoutResult(Competitor.BLUE, "final_score"))

        assertEquals(
            listOf(
                "Красный: 10 гамджом, оператор должен подтвердить дисквалификацию",
                "Победитель: Синий"
            ),
            KerugiBoutPresentation.notices(snapshot)
        )
        assertEquals(
            listOf("Победитель: Синий (дисквалификация соперника)"),
            KerugiBoutPresentation.notices(
                SessionFixtures.snapshot().copy(result = BoutResult(Competitor.BLUE, "disqualification"))
            )
        )
    }
}
