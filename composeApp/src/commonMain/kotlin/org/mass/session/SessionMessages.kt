package org.mass.session

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** Server pushes that change the session after the socket is subscribed (server ADR-009 §3, §5, §9). */
sealed interface SessionPush {
    val cursor: Long

    data class Snapshot(override val cursor: Long, val session: SessionSnapshot?) : SessionPush

    data class StateUpdated(
        val sessionId: String,
        override val cursor: Long,
        val state: SessionPhase,
        val result: BoutResult?
    ) : SessionPush

    data class ScoreUpdated(
        val sessionId: String,
        override val cursor: Long,
        val score: KerugiScore
    ) : SessionPush
}

sealed interface ResyncReply {
    data class Response(val requestId: String, val cursor: Long, val session: SessionSnapshot?) : ResyncReply
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
            "session_snapshot" -> SessionPush.Snapshot(body.integer("cursor"), body.session("session"))
            "session_state_updated" -> SessionPush.StateUpdated(
                sessionId = body.string("sessionId"),
                cursor = body.integer("cursor"),
                state = body.phase("state"),
                result = body.nullable("result")?.let(::result)
            )
            "kerugi_score_updated" -> SessionPush.ScoreUpdated(
                sessionId = body.string("sessionId"),
                cursor = body.integer("cursor"),
                score = score(body.objectAt("score"))
            )
            else -> null
        }
    }

    fun decodeResyncReply(body: JsonObject): ResyncReply? = decoding {
        when (body.string("type")) {
            "resync_response" -> ResyncReply.Response(
                requestId = body.string("requestId"),
                cursor = body.integer("cursor"),
                session = body.session("session")
            )
            "resync_rejected" -> ResyncReply.Rejected(
                requestId = body.nullable("requestId")?.let { (it as JsonPrimitive).text() },
                code = body.string("code")
            )
            else -> null
        }
    }

    private fun JsonObject.session(key: String): SessionSnapshot? = nullable(key)?.let { snapshot(it as JsonObject) }

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
        // The score object is discipline-specific; only the Kerugi shape is known in I2a.
        score = if (body.string("discipline") == "kerugi") score(body.objectAt("score")) else null,
        result = body.nullable("result")?.let(::result)
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
