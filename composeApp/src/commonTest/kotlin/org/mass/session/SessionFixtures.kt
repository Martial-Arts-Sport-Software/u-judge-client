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
        score: KerugiScore? = KerugiScore(3, 1, 0, 1, emptyList(), null)
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
        result = null
    )

    /** The session of the `resync_response` fixture: running, 12:0, ten Gamjeom for red awaiting confirmation. */
    fun resyncFixtureSession() = snapshot(
        score = KerugiScore(12, 0, 0, 10, listOf(ScoreWarning("disqualification_pending", Competitor.RED, 10)), null)
    )

    /** A store with [session] applied on the current socket. */
    fun synchronizedStore(session: SessionSnapshot? = snapshot(), cursor: Long = 118) = SessionStateStore().apply {
        applyResync(cursor, session)
    }
}
