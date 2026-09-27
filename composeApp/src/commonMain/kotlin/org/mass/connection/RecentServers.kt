package org.mass.connection

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** A server address the judge was approved on, offered again on the manual tab. */
data class RecentServer(val host: String, val port: Int, val lastUsedMillis: Long) {
    val address: String get() = "$host:$port"
}

/**
 * The last few addresses that led to an approved pairing, newest first. Only addresses are kept here, never credentials,
 * so plain app preferences are enough.
 */
class RecentServersRepository(
    private val storage: PairingIdentityStorage,
    private val nowMillis: () -> Long,
    private val limit: Int = 5
) {
    fun all(): List<RecentServer> = try {
        Json.parseToJsonElement(storage.get(KEY) ?: "[]").jsonArray.map { entry ->
            val body = entry.jsonObject
            RecentServer(
                host = body.getValue("host").jsonPrimitive.content,
                port = body.getValue("port").jsonPrimitive.content.toInt(),
                lastUsedMillis = body.getValue("lastUsedMillis").jsonPrimitive.long
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun record(host: String, port: Int) {
        val entry = RecentServer(host, port, nowMillis())
        val updated = (listOf(entry) + all().filterNot { it.host == host && it.port == port }).take(limit)
        storage.put(KEY, encode(updated).toString())
    }

    private fun encode(servers: List<RecentServer>): JsonArray = buildJsonArray {
        servers.forEach { server ->
            add(buildJsonObject {
                put("host", server.host)
                put("port", server.port)
                put("lastUsedMillis", server.lastUsedMillis)
            })
        }
    }

    private companion object {
        const val KEY = "recent_servers"
    }
}

/** How long ago something happened, as a localization key and its number (`2 ч назад`, `Вчера`). */
data class RelativeTime(val key: String, val amount: Long? = null)

fun relativeTime(nowMillis: Long, thenMillis: Long): RelativeTime {
    val minutes = (nowMillis - thenMillis).coerceAtLeast(0) / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> RelativeTime("recent_just_now")
        hours < 1 -> RelativeTime("recent_minutes_ago", minutes)
        days < 1 -> RelativeTime("recent_hours_ago", hours)
        days < 2 -> RelativeTime("recent_yesterday")
        else -> RelativeTime("recent_days_ago", days)
    }
}
