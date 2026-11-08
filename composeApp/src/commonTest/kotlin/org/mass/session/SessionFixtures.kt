package org.mass.session

/** Session values of the server contract fixtures (ADR-009), shared by the session, combat and contract tests. */
object SessionFixtures {
    const val SESSION_ID = "9c8b7a6d-5e4f-4d3c-8b2a-1f0e9d8c7b60"
    const val COMPETITION_ID = "3f6c1d2e-8a4b-4c7d-9e1f-0a2b3c4d5e60"
    const val PEER_ID = "00000000-0000-4000-8000-000000000001"
    const val COURT_ID = "5b7e2c10-1d3f-4a6b-8c9d-0e1f2a3b4c50"
    const val BRACKET_ID = "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c40"
    const val JUDGE_ID = "2d4f6a8c-0e1b-4c3d-9e5f-7a9b1c3d5e70"
    const val DEVICE_ID = "android-contract"

    val seat = JudgeSeat(number = 1, judgeId = JUDGE_ID, deviceId = DEVICE_ID)

    fun snapshot(
        sessionId: String = SESSION_ID,
        discipline: String = "kerugi",
        state: SessionPhase = SessionPhase.RUNNING,
        seat: JudgeSeat? = this.seat,
        boutNumber: Int = 7,
        score: KerugiScore? = KerugiScore(3, 1, 0, 1, emptyList(), null),
        timer: BoutTimer? = null
    ) = SessionSnapshot(
        sessionId = sessionId,
        disciplineCode = discipline,
        competitionId = COMPETITION_ID,
        peerId = PEER_ID,
        courtId = COURT_ID,
        courtNumber = 1,
        bracketId = BRACKET_ID,
        categoryTitle = "Юноши 12-14 лет, 45 кг",
        boutNumber = boutNumber,
        state = state,
        blue = BoutSide(
            "entry-12",
            "Иванов И. И. (Москва)",
            listOf(Athlete("Иванов Иван Иванович", "Москва"))
        ),
        red = BoutSide(
            "entry-31",
            "Петров П. П. (Казань)",
            listOf(Athlete("Петров Пётр Петрович", "Казань"))
        ),
        settings = BoutSettings(judgeCount = 2, quorum = 2, coincidenceWindowMillis = 1000),
        seat = seat,
        score = score,
        result = null,
        timer = timer
    )

    /** Round 1 of a 12-14 bout (1:30) before «Бой», as in the `session_snapshot` fixture (server ADR-010 §5). */
    val preparedTimer = BoutTimer(BoutPeriod.ROUND, 1, false, 90_000, 90_000, running = false, runningSinceMillis = null)

    /** Round 1 running since 10:15:30Z, as in the `resync_response` fixture. */
    val runningTimer = preparedTimer.copy(running = true, runningSinceMillis = 1_794_132_930_000)

    /** The session of the `resync_response` fixture: running, 12:0, ten Gamjeom for red awaiting confirmation. */
    fun resyncFixtureSession() = snapshot(
        score = KerugiScore(12, 0, 0, 10, listOf(ScoreWarning("disqualification_pending", Competitor.RED, 10)), null),
        timer = runningTimer
    )

    const val PERFORMANCE_ID = "1b2c3d4e-5f60-4718-8a9b-0c1d2e3f4a50"
    const val PERFORMANCE_BRACKET_ID = "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c41"
    const val PERFORMANCE_JUDGE_ID = "6e5d4c3b-2a19-4807-9f6e-5d4c3b2a1900"

    /** The performance of the `technical_session_snapshot` fixture (server ADR-011 §6). */
    fun technical(
        sessionId: String = PERFORMANCE_ID,
        discipline: String = "hosinsool",
        state: SessionPhase = SessionPhase.PREPARED,
        seat: JudgeSeat? = JudgeSeat(number = 1, judgeId = PERFORMANCE_JUDGE_ID, deviceId = DEVICE_ID),
        techniqueCriteria: Int = 4,
        sent: SentVector? = null,
        score: TechnicalScore = TechnicalScore(emptyList(), null)
    ) = TechnicalSnapshot(
        sessionId = sessionId,
        disciplineCode = discipline,
        competitionId = COMPETITION_ID,
        peerId = PEER_ID,
        courtId = COURT_ID,
        courtNumber = 1,
        bracketId = PERFORMANCE_BRACKET_ID,
        categoryTitle = "Приёмы самообороны · 2013-2011 г.р. Юноши",
        performanceNumber = 12,
        state = state,
        entry = BoutSide(
            "entry-5",
            "Иванов И. И. (Москва) / Петров П. П. (Москва)",
            listOf(Athlete("Иванов Иван Иванович", "Москва"), Athlete("Петров Пётр Петрович", "Москва"))
        ),
        judgeCount = 2,
        formula = "mean",
        techniqueCriteria = techniqueCriteria,
        presentationCriteria = 4,
        deductionStepTenths = 3,
        seat = seat,
        sent = sent,
        score = score
    )

    /** A store whose current session is [performance], applied on the current socket. */
    fun technicalStore(performance: TechnicalSnapshot = technical(state = SessionPhase.RUNNING), cursor: Long = 205) =
        SessionStateStore().apply { applyResync(cursor, null, performance) }

    /** A store with [session] applied on the current socket. */
    fun synchronizedStore(session: SessionSnapshot? = snapshot(), cursor: Long = 118) = SessionStateStore().apply {
        applyResync(cursor, session)
    }
}
