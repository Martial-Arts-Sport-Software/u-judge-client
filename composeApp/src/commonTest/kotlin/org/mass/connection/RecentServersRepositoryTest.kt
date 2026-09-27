package org.mass.connection

import kotlin.test.Test
import kotlin.test.assertEquals

class RecentServersRepositoryTest {
    private val values = mutableMapOf<String, String>()
    private val storage = object : PairingIdentityStorage {
        override fun get(key: String) = values[key]
        override fun put(key: String, value: String) { values[key] = value }
    }
    private var now = 1_000L

    @Test
    fun keepsTheNewestAddressesFirstWithoutDuplicates() {
        val repository = RecentServersRepository(storage, { now }, limit = 2)
        repository.record("10.0.2.2", 8443)
        now = 2_000
        repository.record("192.168.10.5", 8443)
        now = 3_000
        repository.record("10.0.2.2", 8443)
        now = 4_000
        repository.record("172.16.4.20", 8443)

        assertEquals(
            listOf(RecentServer("172.16.4.20", 8443, 4_000), RecentServer("10.0.2.2", 8443, 3_000)),
            RecentServersRepository(storage, { now }).all()
        )
    }

    @Test
    fun corruptedStorageReadsAsEmpty() {
        values["recent_servers"] = "not json"
        assertEquals(emptyList(), RecentServersRepository(storage, { now }).all())
    }

    @Test
    fun relativeTimeUsesMinutesHoursYesterdayAndDays() {
        val hour = 3_600_000L
        assertEquals(RelativeTime("recent_just_now"), relativeTime(10_000, 0))
        assertEquals(RelativeTime("recent_minutes_ago", 5), relativeTime(5 * 60_000, 0))
        assertEquals(RelativeTime("recent_hours_ago", 2), relativeTime(2 * hour, 0))
        assertEquals(RelativeTime("recent_yesterday"), relativeTime(30 * hour, 0))
        assertEquals(RelativeTime("recent_days_ago", 3), relativeTime(3 * 24 * hour, 0))
    }
}
