package org.mass.connection

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RoutingRealtimeChannelTest {
    @Test
    fun repliesAreRoutedByEventIdRequestIdAndTypeWhilePushesGoToTheHandler() = runTest {
        val socket = ChannelSocket()
        val pushes = mutableListOf<JsonObject>()
        val channel = RoutingRealtimeChannel(socket, backgroundScope, { pushes += it })

        val first = async { channel.exchange("""{"type":"kerugi_score_command","eventId":"event-1"}""") }
        val second = async { channel.exchange("""{"type":"kerugi_score_command","eventId":"event-2"}""") }
        val resync = async { channel.exchange("""{"type":"resync_request","requestId":"request-1","cursor":null}""") }
        val heartbeat = async { channel.exchange("""{"type":"heartbeat"}""") }
        runCurrent()
        socket.serve(
            """{"type":"command_ack","eventId":"event-2"}""",
            """{"type":"kerugi_score_updated","sessionId":"s","cursor":5}""",
            """{"type":"heartbeat_ack"}""",
            """{"type":"resync_response","requestId":"request-1","cursor":4,"session":null}""",
            """{"type":"command_rejected","code":"session_closed","eventId":"event-1"}"""
        )
        runCurrent()

        assertEquals("""{"type":"command_rejected","code":"session_closed","eventId":"event-1"}""", first.await())
        assertEquals("""{"type":"command_ack","eventId":"event-2"}""", second.await())
        assertEquals("""{"type":"resync_response","requestId":"request-1","cursor":4,"session":null}""", resync.await())
        assertEquals("""{"type":"heartbeat_ack"}""", heartbeat.await())
        assertEquals(listOf("kerugi_score_updated"), pushes.map { it.getValue("type").jsonPrimitive.content })
    }

    @Test
    fun aRejectionWithoutAnEventIdAnswersTheOldestCommand() = runTest {
        val socket = ChannelSocket()
        val channel = RoutingRealtimeChannel(socket, backgroundScope, {})

        val first = async { channel.exchange("""{"type":"kerugi_score_command","eventId":"event-1"}""") }
        val second = async { channel.exchange("""{"type":"kerugi_score_command","eventId":"event-2"}""") }
        runCurrent()
        socket.serve(
            """{"type":"command_rejected","code":"command_too_large","eventId":null}""",
            """{"type":"command_ack","eventId":"event-2"}"""
        )
        runCurrent()

        assertEquals("""{"type":"command_rejected","code":"command_too_large","eventId":null}""", first.await())
        assertEquals("""{"type":"command_ack","eventId":"event-2"}""", second.await())
    }

    @Test
    fun aLostSocketFailsWaitingRequestsReportsTheLossAndRefusesNewOnes() = runTest {
        val socket = ChannelSocket()
        val closed = mutableListOf<RoutingRealtimeChannel>()
        val channel = RoutingRealtimeChannel(socket, backgroundScope, {}, { closed += it })

        val waiting = async { runCatching { channel.exchange("""{"type":"heartbeat"}""") } }
        runCurrent()
        socket.drop()
        runCurrent()

        assertTrue(waiting.await().exceptionOrNull() is RealtimeSocketClosedException)
        assertEquals(listOf(channel), closed)
        assertFailsWith<RealtimeSocketClosedException> { channel.exchange("""{"type":"heartbeat"}""") }
    }

    @Test
    fun anExplicitCloseIsNotReportedAsALoss() = runTest {
        val socket = ChannelSocket()
        val closed = mutableListOf<RoutingRealtimeChannel>()
        val channel = RoutingRealtimeChannel(socket, backgroundScope, {}, { closed += it })
        runCurrent()

        channel.close()
        runCurrent()

        assertEquals(emptyList(), closed)
        assertEquals(1, socket.closeCount)
    }

    /** A socket whose incoming frames the test serves explicitly. */
    private class ChannelSocket : RealtimeSocket {
        private val incoming = Channel<String>(Channel.UNLIMITED)
        var closeCount = 0

        fun serve(vararg frames: String) = frames.forEach { incoming.trySend(it) }

        fun drop() {
            incoming.close(IllegalStateException("connection dropped"))
        }

        override suspend fun send(payload: String) = Unit

        override suspend fun receive(): String = incoming.receive()

        override suspend fun close() {
            closeCount++
            incoming.close()
        }
    }
}
