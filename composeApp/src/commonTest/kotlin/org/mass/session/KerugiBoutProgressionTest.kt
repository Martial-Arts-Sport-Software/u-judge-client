package org.mass.session

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mass.State
import org.mass.combat.KerugiCommandController
import org.mass.combat.KerugiCommandOutcome
import org.mass.combat.KerugiParticipant
import org.mass.combat.KerugiTarget
import org.mass.connection.ConnectionEvent
import org.mass.connection.ConnectionStateStore
import org.mass.screens.KerugiBoutPresentation
import org.mass.transport.DurableEventOutbox
import org.mass.transport.EventOutboxStorage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * The judge's side of server ADR-010 (client I2b): the bout clock from snapshots and pushes, the move to the next bout of
 * the bracket on the same socket, no command for a stale or foreign bout (`CLI-072`) and the ready signal (`CLI-026`).
 */
class KerugiBoutProgressionTest {
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
    fun theClockCountsDownOnServerTimeAndPushesReplaceIt() {
        val store = SessionFixtures.synchronizedStore(SessionFixtures.snapshot(timer = SessionFixtures.runningTimer))
        val started = SessionFixtures.runningTimer.runningSinceMillis!!

        assertEquals(60_500, store.snapshot!!.timer!!.remainingAt(started + 29_500))
        assertEquals(0, store.snapshot!!.timer!!.remainingAt(started + 200_000))
        assertEquals("Раунд 1", KerugiBoutPresentation.period(store.snapshot!!.timer))
        assertEquals("1:01", KerugiBoutPresentation.clock(store.snapshot!!.timer, started + 29_500))

        // Round 1 ends into the break: the state push carries the timer.
        val breakTimer = BoutTimer(BoutPeriod.BREAK, 2, false, 60_000, 60_000, true, started + 90_000)
        assertEquals(
            PushOutcome.APPLIED,
            store.applyPush(SessionPush.StateUpdated(SessionFixtures.SESSION_ID, 120, SessionPhase.PAUSED, null, breakTimer))
        )
        assertEquals(SessionPhase.PAUSED, store.snapshot!!.state)
        assertEquals("Перерыв", KerugiBoutPresentation.period(store.snapshot!!.timer))

        // The break is stopped: only the timer changes.
        val stopped = breakTimer.copy(remainingMillis = 0, running = false, runningSinceMillis = null)
        assertEquals(PushOutcome.APPLIED, store.applyPush(SessionPush.TimerUpdated(SessionFixtures.SESSION_ID, 121, stopped)))
        assertEquals("0:00", KerugiBoutPresentation.clock(store.snapshot!!.timer, started + 500_000))
        assertEquals(PushOutcome.STALE, store.applyPush(SessionPush.TimerUpdated(SessionFixtures.SESSION_ID, 121, breakTimer)))
        assertEquals(
            PushOutcome.SESSION_MISMATCH,
            store.applyPush(SessionPush.TimerUpdated("another-session", 122, breakTimer))
        )
        assertEquals(stopped, store.snapshot!!.timer)

        // A state push without a timer (an ADR-009 server) keeps the held one.
        store.applyPush(SessionPush.StateUpdated(SessionFixtures.SESSION_ID, 123, SessionPhase.RUNNING, null))
        assertEquals(stopped, store.snapshot!!.timer)

        assertEquals("Отдых перед дополнительным раундом", KerugiBoutPresentation.period(breakTimer.copy(round = 3, goldenRound = true)))
        assertEquals(
            "Дополнительный раунд",
            KerugiBoutPresentation.period(breakTimer.copy(period = BoutPeriod.ROUND, round = 3, goldenRound = true))
        )
    }

    @Test
    fun aResultIsFollowedByTheNextBoutOnTheSameSocketAndTheNewSeatIsUsed() {
        val store = SessionFixtures.synchronizedStore()
        val outbox = DurableEventOutbox(MemoryStorage())
        val ids = ArrayDeque(listOf("tap-1", "tap-2", "tap-3"))
        val controller = controller(store, outbox) { ids.removeFirst() }
        assertIs<KerugiCommandOutcome.Pending>(controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD))

        store.applyPush(
            SessionPush.StateUpdated(SessionFixtures.SESSION_ID, 125, SessionPhase.COMPLETED, BoutResult(Competitor.BLUE, "golden_round"))
        )
        assertEquals(
            listOf("Победитель: Синий (дополнительный раунд)"),
            KerugiBoutPresentation.notices(store.snapshot)
        )
        assertIs<KerugiCommandOutcome.Unavailable>(controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD))

        // «Начать» of the next bout pushes a new snapshot: this device now sits on seat 2 of bout 8.
        val nextSeat = JudgeSeat(2, "judge-next", SessionFixtures.DEVICE_ID)
        val next = SessionFixtures.snapshot(
            sessionId = "next-session",
            state = SessionPhase.PREPARED,
            seat = nextSeat,
            boutNumber = 8,
            timer = SessionFixtures.preparedTimer
        )
        assertEquals(PushOutcome.APPLIED, store.applyPush(SessionPush.Snapshot(130, next)))
        assertEquals("Поединок №8", KerugiBoutPresentation.boutTitle(store.snapshot))
        // The feedback of the finished bout is not shown for the new one.
        assertNull(controller.currentOutcome)

        val ready = assertIs<KerugiCommandOutcome.Pending>(controller.signalReady()).event
        assertEquals("next-session", ready.sessionId)
        assertEquals("judge_ready_command", ready.field("type"))
        assertEquals("judge-next", ready.field("judgeId"))
        store.applyPush(SessionPush.StateUpdated("next-session", 131, SessionPhase.RUNNING, null))
        assertIs<KerugiCommandOutcome.Unavailable>(controller.signalReady())
        val tap = assertIs<KerugiCommandOutcome.Pending>(controller.submit(KerugiParticipant.RED, KerugiTarget.BODY)).event
        assertEquals("next-session", tap.sessionId)

        // The tap of the finished bout stays in the outbox with its own context; the server rejects it.
        assertEquals(listOf(SessionFixtures.SESSION_ID, "next-session", "next-session"), outbox.pendingEvents().map { it.sessionId })
    }

    @Test
    fun aTapOnAStaleOrForeignBoutCreatesNothing() {
        val store = SessionFixtures.synchronizedStore()
        val outbox = DurableEventOutbox(MemoryStorage())
        val controller = controller(store, outbox) { "event-1" }
        val shown = store.snapshot!!.sessionId

        store.applyPush(SessionPush.Snapshot(130, SessionFixtures.snapshot(sessionId = "next-session")))
        assertEquals(
            KerugiCommandOutcome.StaleSession,
            controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD, shownSessionId = shown)
        )
        assertEquals(KerugiCommandOutcome.StaleSession, controller.signalAttention(shownSessionId = shown))
        assertEquals(
            "Поединок сменился: нажатие не отправлено. Проверьте новый поединок.",
            KerugiBoutPresentation.tapFeedback(controller.latestOutcome)
        )

        // A bout in which this device has no seat is shown read-only.
        store.applyPush(SessionPush.Snapshot(131, SessionFixtures.snapshot(sessionId = "foreign-session", seat = null)))
        assertEquals(KerugiCommandOutcome.Unavailable, controller.submit(KerugiParticipant.BLUE, KerugiTarget.HEAD))
        assertEquals(emptyList(), outbox.pendingEvents())
    }

    private fun controller(store: SessionStateStore, outbox: DurableEventOutbox, eventId: () -> String) =
        KerugiCommandController(
            connection = ConnectionStateStore().apply {
                dispatch(ConnectionEvent.ResumePairedServer(SessionFixtures.DEVICE_ID))
                dispatch(ConnectionEvent.AcceptPairing(SessionFixtures.DEVICE_ID, clockOffsetMillis = 0))
            },
            session = store,
            outbox = outbox,
            nowMillis = { 1_000 },
            eventId = eventId
        )

    private fun org.mass.transport.OutboxEvent.field(name: String) =
        Json.parseToJsonElement(payload).jsonObject.getValue(name).jsonPrimitive.content

    private class MemoryStorage : EventOutboxStorage {
        private var value: String? = null

        override fun load(): String? = value

        override fun save(value: String) {
            this.value = value
        }
    }
}
