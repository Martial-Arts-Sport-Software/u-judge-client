package org.mass.session

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlin.time.Instant

/** Server pushes that change the session after the socket is subscribed (server ADR-009 §3, §5, §9; ADR-010 §5; ADR-011 §3, §10). */
sealed interface SessionPush {
    val cursor: Long

    /**
     * A new current session: [session] is a Kerugi or Tanbon bout, [technical] a technical performance and
     * [otherSessionId] a session of a discipline this client does not know; at most one is set.
     */
    data class Snapshot(
        override val cursor: Long,
        val session: SessionSnapshot?,
        val technical: TechnicalSnapshot? = null,
        val otherSessionId: String? = null
    ) : SessionPush

    data class StateUpdated(
        val sessionId: String,
        override val cursor: Long,
        val state: SessionPhase,
        val result: BoutResult?,
        /** The timer after the transition (server ADR-010 §5). */
        val timer: BoutTimer? = null
    ) : SessionPush

    /** The clock changed without a state change (server ADR-010 §5). */
    data class TimerUpdated(
        val sessionId: String,
        override val cursor: Long,
        val timer: BoutTimer
    ) : SessionPush

    /** `kerugi_score_updated` or `tanbon_score_updated`: the same score shape. */
    data class ScoreUpdated(
        val sessionId: String,
        override val cursor: Long,
        val score: KerugiScore
    ) : SessionPush

    data class TechnicalScoreUpdated(
        val sessionId: String,
        override val cursor: Long,
        val score: TechnicalScore
    ) : SessionPush
}

sealed interface ResyncReply {
    data class Response(
        val requestId: String,
        val cursor: Long,
        val session: SessionSnapshot?,
        val technical: TechnicalSnapshot? = null,
        val otherSessionId: String? = null
    ) : ResyncReply
    data class Rejected(val requestId: String?, val code: String) : ResyncReply
}

/**
 * Decodes the session messages of server ADR-009. Server messages may gain fields and types, so unknown fields are
 * ignored; a missing or mistyped listed field makes the whole message invalid (null).
 */
object SessionMessages {
    const val RESYNC_JOURNAL_UNAVAILABLE = "journal_unavailable"

    fun resyncRequest(requestId: String, cursor: Long?): String = buildJsonObject {
        put("type", "resync_request")
        put("requestId", requestId)
        put("cursor", cursor)
    }.toString()

    fun decodePush(body: JsonObject): SessionPush? = decoding {
        when (body.string("type")) {
            "session_snapshot" -> body.court("session").let { court ->
                SessionPush.Snapshot(body.integer("cursor"), court.bout(), court.technical(), court.other())
            }
            "session_state_updated" -> SessionPush.StateUpdated(
                sessionId = body.string("sessionId"),
                cursor = body.integer("cursor"),
                state = body.phase("state"),
                result = body.nullable("result")?.let(::result),
                timer = body.optionalTimer()
            )
            "kerugi_timer_updated" -> SessionPush.TimerUpdated(
                sessionId = body.string("sessionId"),
                cursor = body.integer("cursor"),
                timer = timer(body.objectAt("timer"))
            )
            "kerugi_score_updated", "tanbon_score_updated" -> SessionPush.ScoreUpdated(
                sessionId = body.string("sessionId"),
                cursor = body.integer("cursor"),
                score = score(body.objectAt("score"))
            )
            "technical_score_updated" -> SessionPush.TechnicalScoreUpdated(
                sessionId = body.string("sessionId"),
                cursor = body.integer("cursor"),
                score = technicalScore(body.objectAt("score"))
            )
            else -> null
        }
    }

    fun decodeResyncReply(body: JsonObject): ResyncReply? = decoding {
        when (body.string("type")) {
            "resync_response" -> body.court("session").let { court ->
                ResyncReply.Response(
                    requestId = body.string("requestId"),
                    cursor = body.integer("cursor"),
                    session = court.bout(),
                    technical = court.technical(),
                    otherSessionId = court.other()
                )
            }
            "resync_rejected" -> ResyncReply.Rejected(
                requestId = body.nullable("requestId")?.let { (it as JsonPrimitive).text() },
                code = body.string("code")
            )
            else -> null
        }
    }

    /** The session body decoded by its `discipline` (server ADR-011 §6). */
    private fun JsonObject.court(key: String): CourtSession? = nullable(key)?.let { element ->
        val body = element as JsonObject
        val discipline = body.string("discipline")
        when {
            discipline in BOUT_DISCIPLINES -> CourtSession.Bout(snapshot(body))
            TechnicalDiscipline.ofCode(discipline) != null -> CourtSession.Technical(technical(body))
            else -> CourtSession.Unknown(body.string("sessionId"), discipline)
        }
    }

    private fun CourtSession?.bout() = (this as? CourtSession.Bout)?.snapshot

    private fun CourtSession?.technical() = (this as? CourtSession.Technical)?.snapshot

    private fun CourtSession?.other() = (this as? CourtSession.Unknown)?.sessionId

    private fun technical(body: JsonObject): TechnicalSnapshot {
        val settings = body.objectAt("settings")
        val criteria = body.objectAt("criteria")
        return TechnicalSnapshot(
            sessionId = body.string("sessionId"),
            disciplineCode = body.string("discipline"),
            competitionId = body.string("competitionId"),
            peerId = body.string("peerId"),
            courtId = body.string("courtId"),
            courtNumber = body.integer("courtNumber").toInt(),
            bracketId = body.string("bracketId"),
            categoryTitle = body.string("categoryTitle"),
            performanceNumber = body.integer("performanceNumber").toInt(),
            state = body.phase("state"),
            entry = side(body.objectAt("entry")),
            judgeCount = settings.integer("judgeCount").toInt(),
            formula = settings.string("formula"),
            techniqueCriteria = criteria.integer("technique").toInt(),
            presentationCriteria = criteria.integer("presentation").toInt(),
            deductionStepTenths = criteria.integer("deductionStepTenths").toInt(),
            seat = body.nullable("seat")?.let {
                val seat = it as JsonObject
                JudgeSeat(seat.integer("number").toInt(), seat.string("judgeId"), seat.string("deviceId"))
            },
            sent = body.nullable("sent")?.let {
                val sent = it as JsonObject
                SentVector(
                    eventId = sent.string("eventId"),
                    techniqueTenths = sent.integers("techniqueTenths"),
                    presentationTenths = sent.integers("presentationTenths"),
                    deductionTenths = sent.integer("deductionTenths").toInt(),
                    totalTenths = sent.integer("totalTenths").toInt()
                )
            },
            score = technicalScore(body.objectAt("score"))
        )
    }

    private fun technicalScore(body: JsonObject) = TechnicalScore(
        submittedSeats = body.integers("submittedSeats"),
        result = body.nullable("result")?.let {
            val result = it as JsonObject
            TechnicalResult(
                totalTenths = result.integer("totalTenths").toInt(),
                aggregateTenths = result.integer("aggregateTenths").toInt(),
                adjustmentTenths = result.integer("adjustmentTenths").toInt(),
                divisor = result.integer("divisor").toInt(),
                formula = result.string("formula")
            )
        }
    )

    private fun JsonObject.integers(key: String): List<Int> = (getValue(key) as JsonArray).map { element ->
        val value = element as JsonPrimitive
        require(!value.isString)
        value.int
    }

    private val BOUT_DISCIPLINES = setOf("kerugi", "tanbon")

    private fun snapshot(body: JsonObject): SessionSnapshot = SessionSnapshot(
        sessionId = body.string("sessionId"),
        disciplineCode = body.string("discipline"),
        competitionId = body.string("competitionId"),
        peerId = body.string("peerId"),
        courtId = body.string("courtId"),
        courtNumber = body.integer("courtNumber").toInt(),
        bracketId = body.string("bracketId"),
        categoryTitle = body.string("categoryTitle"),
        boutNumber = body.integer("boutNumber").toInt(),
        state = body.phase("state"),
        blue = side(body.objectAt("blue")),
        red = side(body.objectAt("red")),
        settings = body.objectAt("settings").let {
            BoutSettings(
                judgeCount = it.integer("judgeCount").toInt(),
                quorum = it.integer("quorum").toInt(),
                coincidenceWindowMillis = it.integer("coincidenceWindowMillis")
            )
        },
        seat = body.nullable("seat")?.let {
            val seat = it as JsonObject
            JudgeSeat(seat.integer("number").toInt(), seat.string("judgeId"), seat.string("deviceId"))
        },
        // Kerugi and Tanbon share the score shape (server ADR-011 §3).
        score = score(body.objectAt("score")),
        result = body.nullable("result")?.let(::result),
        timer = body.optionalTimer()
    )

    /** `timer` came with server ADR-010; a server without it sends none, and a malformed one invalidates the message. */
    private fun JsonObject.optionalTimer(): BoutTimer? = get("timer")?.let { timer(it as JsonObject) }

    private fun timer(body: JsonObject) = BoutTimer(
        period = requireNotNull(BoutPeriod.fromWire(body.string("period"))),
        round = body.integer("round").toInt(),
        goldenRound = (body.getValue("goldenRound") as JsonPrimitive).boolean,
        durationMillis = body.integer("durationMillis"),
        remainingMillis = body.integer("remainingMillis"),
        running = (body.getValue("running") as JsonPrimitive).boolean,
        runningSinceMillis = body.nullable("runningSince")?.let {
            Instant.parse((it as JsonPrimitive).text()).toEpochMilliseconds()
        }
    )

    private fun side(body: JsonObject) = BoutSide(
        entryId = body.string("entryId"),
        label = body.string("label"),
        athletes = (body.getValue("athletes") as JsonArray).map { element ->
            val athlete = element as JsonObject
            Athlete(athlete.string("fullName"), athlete.nullable("city")?.let { (it as JsonPrimitive).text() })
        }
    )

    private fun score(body: JsonObject): KerugiScore {
        val gamjeom = body.objectAt("gamjeom")
        return KerugiScore(
            blue = body.integer("blue").toInt(),
            red = body.integer("red").toInt(),
            blueGamjeom = gamjeom.integer("blue").toInt(),
            redGamjeom = gamjeom.integer("red").toInt(),
            warnings = (body.getValue("warnings") as JsonArray).map { element ->
                val warning = element as JsonObject
                ScoreWarning(
                    code = warning.string("code"),
                    competitor = warning["competitor"]?.takeUnless { it is JsonNull }
                        ?.let { competitor((it as JsonPrimitive).text()) },
                    gamjeomCount = warning["gamjeomCount"]?.takeUnless { it is JsonNull }
                        ?.let { (it as JsonPrimitive).int }
                )
            },
            disqualified = body.nullable("disqualified")?.let { competitor((it as JsonPrimitive).text()) }
        )
    }

    private fun result(element: JsonElement): BoutResult {
        val body = element as JsonObject
        return BoutResult(
            winner = body["winner"]?.takeUnless { it is JsonNull }?.let { competitor((it as JsonPrimitive).text()) },
            reason = body.string("reason")
        )
    }

    private fun competitor(value: String) = Competitor.valueOf(value)

    private fun JsonObject.phase(key: String) = requireNotNull(SessionPhase.fromWire(string(key)))

    private fun JsonObject.objectAt(key: String) = getValue(key) as JsonObject

    /** A listed nullable field: it must be present, and JSON null decodes to null. */
    private fun JsonObject.nullable(key: String): JsonElement? = getValue(key).takeUnless { it is JsonNull }

    private fun JsonObject.string(key: String): String = (getValue(key) as JsonPrimitive).text()

    /** Cursors and counts are JSON integers, never strings. */
    private fun JsonObject.integer(key: String): Long {
        val value = getValue(key) as JsonPrimitive
        require(!value.isString)
        return value.long
    }

    private fun JsonPrimitive.text(): String {
        require(isString)
        return content
    }

    private inline fun <T> decoding(block: () -> T?): T? = try {
        block()
    } catch (_: Exception) {
        null
    }
}
