package org.mass.connection

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RealtimeRequestChannelTest {
    @Test
    fun concurrentRequestsKeepEachSendAndResponseTogether() = runTest {
        val socket = SingleExchangeSocket()
        val channel = SerializedRealtimeRequestChannel(socket)

        val responses = awaitAll(
            async { channel.exchange("first") },
            async { channel.exchange("second") }
        )

        assertEquals(listOf("ack:first", "ack:second"), responses)
        assertEquals(listOf("first", "second"), socket.sent)
    }

    private class SingleExchangeSocket : RealtimeSocket {
        val sent = mutableListOf<String>()
        private var inFlight: String? = null

        override suspend fun send(payload: String) {
            check(inFlight == null) { "A second request started before the first response." }
            inFlight = payload
            sent += payload
            delay(1)
        }

        override suspend fun receive(): String {
            delay(1)
            return "ack:${requireNotNull(inFlight).also { inFlight = null }}"
        }

        override suspend fun close() = Unit
    }
}
