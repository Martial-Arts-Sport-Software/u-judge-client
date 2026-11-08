package org.mass.session

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mass.connection.RealtimeRequestChannel
import org.mass.contract.ServerContractFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionSyncTest {
    @Test
    fun attachSendsTheHeldCursorAndAppliesTheResponse() = runTest {
        val store = SessionStateStore()
        val channel = ResyncChannel { id -> listOf(response(id)) }
        val sync = SessionSync(backgroundScope, store, requestIds())

        sync.attach(channel)
        assertFalse(store.isSynchronized)
        runCurrent()

        assertEquals(listOf<Long?>(null), channel.cursors)
        assertTrue(store.isSynchronized)
        assertEquals(SessionFixtures.resyncFixtureSession(), store.snapshot)
        assertEquals(124, store.cursor)

        // A reconnect detaches the old socket, so controls wait for the resync on the new one.
        val second = ResyncChannel { id -> listOf(response(id)) }
        sync.attach(second)
        assertFalse(store.isSynchronized)
        runCurrent()

        assertEquals(listOf<Long?>(124), second.cursors)
        assertTrue(store.isSynchronized)
    }

    @Test
    fun journalUnavailableIsRetriedWithBackoffWhileControlsStayDisabled() = runTest {
        val store = SessionStateStore()
        val replies = ArrayDeque(listOf("journal_unavailable", "journal_unavailable", "ok"))
        val channel = ResyncChannel { id ->
            val next = replies.removeFirst()
            listOf(if (next == "ok") response(id) else rejected(id, next))
        }
        val sync = SessionSync(backgroundScope, store, requestIds(), retryDelayMillis = { 1_000L * (it + 1) })

        sync.attach(channel)
        runCurrent()
        assertFalse(store.isSynchronized)
        assertEquals("journal_unavailable", sync.failureCode)
        advanceTimeBy(1_001)
        assertFalse(store.isSynchronized)
        advanceTimeBy(2_001)

        assertTrue(store.isSynchronized)
        assertEquals(3, channel.cursors.size)
    }

    @Test
    fun anotherRejectionStopsWithoutEnablingControls() = runTest {
        val store = SessionStateStore()
        val sync = SessionSync(backgroundScope, store, requestIds())

        sync.attach(ResyncChannel { id -> listOf(rejected(id, "invalid_resync_request")) })
        advanceTimeBy(60_000)

        assertFalse(store.isSynchronized)
        assertEquals("invalid_resync_request", sync.failureCode)
    }

    @Test
    fun aResponseToAnotherRequestIsNotApplied() = runTest {
        val store = SessionStateStore()
        val sync = SessionSync(backgroundScope, store, requestIds())

        sync.attach(ResyncChannel { listOf(response("someone-else")) })
        runCurrent()

        assertFalse(store.isSynchronized)
        assertEquals(SessionSync.INVALID_RESPONSE, sync.failureCode)
    }

    @Test
    fun aPushForAnotherSessionTriggersAResyncWithTheHeldCursor() = runTest {
        val store = SessionStateStore()
        val channel = ResyncChannel { id -> listOf(response(id)) }
        val sync = SessionSync(backgroundScope, store, requestIds())
        sync.attach(channel)
        runCurrent()

        sync.onPush(json("""{"type":"session_state_updated","sessionId":"other","cursor":130,"state":"paused","result":null}"""))
        runCurrent()

        assertEquals(listOf(null, 124L), channel.cursors)
        assertEquals(SessionPhase.RUNNING, store.snapshot?.state)
    }

    @Test
    fun stalePushesAreDroppedAndNewerOnesApplied() = runTest {
        val store = SessionStateStore()
        val sync = SessionSync(backgroundScope, store, requestIds())
        sync.attach(ResyncChannel { id -> listOf(response(id)) })
        runCurrent()

        sync.onPush(json(ServerContractFixtures.message("session_state_updated")).with("cursor", 124))
        assertEquals(SessionPhase.RUNNING, store.snapshot?.state)
        sync.onPush(json(ServerContractFixtures.message("kerugi_score_updated")).with("cursor", 126))
        sync.onPush(json(ServerContractFixtures.message("session_state_updated")).with("cursor", 125))
        assertEquals(SessionPhase.RUNNING, store.snapshot?.state)
        sync.onPush(json(ServerContractFixtures.message("session_state_updated")).with("cursor", 127))

        assertEquals(12, store.snapshot?.score?.blue)
        assertEquals(SessionPhase.COMPLETED, store.snapshot?.state)
        assertEquals(127, store.cursor)
    }

    @Test
    fun detachDisablesControlsAndALateResponseIsIgnored() = runTest {
        val store = SessionFixtures.synchronizedStore()
        val sync = SessionSync(backgroundScope, store, requestIds())
        val channel = ResyncChannel { id -> listOf(response(id)) }
        sync.attach(channel)
        assertFalse(store.isSynchronized)

        sync.detach(channel)
        runCurrent()

        assertFalse(store.isSynchronized)
        sync.onPush(json(ServerContractFixtures.message("kerugi_score_updated")))
        assertEquals(3, store.snapshot?.score?.blue)
        assertNull(sync.failureCode)
    }

    private fun requestIds(): () -> String {
        var next = 0
        return { "request-${++next}" }
    }

    private fun response(requestId: String) =
        json(ServerContractFixtures.message("resync_response")).with("requestId", requestId).toString()

    private fun rejected(requestId: String, code: String) =
        """{"type":"resync_rejected","requestId":"$requestId","code":"$code"}"""

    private fun json(value: String) = Json.parseToJsonElement(value).jsonObject

    private fun JsonObject.with(key: String, value: Any) = JsonObject(
        this + (key to if (value is Number) JsonPrimitive(value) else JsonPrimitive(value.toString()))
    )

    /** Answers each `resync_request` with [reply] for its request ID and records the sent cursors. */
    private class ResyncChannel(private val reply: (String) -> List<String>) : RealtimeRequestChannel {
        val cursors = mutableListOf<Long?>()

        override suspend fun exchange(payload: String): String {
            val body = Json.parseToJsonElement(payload).jsonObject
            cursors += body.getValue("cursor").jsonPrimitive.content.toLongOrNull()
            return reply(body.getValue("requestId").jsonPrimitive.content).single()
        }

        override suspend fun close() = Unit
    }
}
