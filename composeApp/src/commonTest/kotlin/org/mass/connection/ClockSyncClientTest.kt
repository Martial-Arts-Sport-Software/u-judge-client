package org.mass.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ClockSyncClientTest {
    @Test
    fun estimatesServerOffsetAndRoundTripFromFourTimestamps() = runTest {
        val socket = FakeSocket(
            """{"type":"clock_sync_response","clientSendTimestamp":"1970-01-01T00:00:01Z","serverReceiveTimestamp":"1970-01-01T00:00:01.100Z","serverSendTimestamp":"1970-01-01T00:00:01.120Z"}"""
        )
        val times = mutableListOf(1_000L, 1_100L)

        val result = ClockSyncClient { times.removeFirst() }.synchronize(socket)

        assertEquals(ClockSyncResult.Synchronized(offsetMillis = 60, roundTripMillis = 80), result)
        assertEquals(
            "{\"type\":\"clock_sync\",\"clientSendTimestamp\":\"1970-01-01T00:00:01Z\"}",
            socket.sent.single()
        )
    }

    @Test
    fun returnsTypedServerRejection() = runTest {
        val socket = FakeSocket("""{"type":"clock_sync_rejected","code":"invalid_clock_sync_timestamp"}""")

        val result = ClockSyncClient { 1_000L }.synchronize(socket)

        assertEquals(ClockSyncResult.Rejected("invalid_clock_sync_timestamp"), result)
    }

    @Test
    fun rejectsResponseForADifferentClientTimestamp() = runTest {
        val socket = FakeSocket(
            """{"type":"clock_sync_response","clientSendTimestamp":"1970-01-01T00:00:02Z","serverReceiveTimestamp":"1970-01-01T00:00:01.100Z","serverSendTimestamp":"1970-01-01T00:00:01.120Z"}"""
        )
        val times = mutableListOf(1_000L, 1_100L)

        val result = ClockSyncClient { times.removeFirst() }.synchronize(socket)

        assertEquals(ClockSyncResult.InvalidResponse, result)
    }

    private class FakeSocket(private val response: String) : RealtimeSocket {
        val sent = mutableListOf<String>()

        override suspend fun send(payload: String) {
            sent += payload
        }

        override suspend fun receive(): String = response

        override suspend fun close() = Unit
    }
}
