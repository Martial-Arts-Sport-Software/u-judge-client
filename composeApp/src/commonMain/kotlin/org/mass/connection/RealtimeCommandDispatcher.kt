package org.mass.connection

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mass.transport.DurableEventOutbox
import kotlin.time.Clock

/**
 * Drains the durable outbox through the currently authenticated socket, one command at a time in client sequence: a
 * later tap never overtakes an earlier pending one, and a transient rejection waits for its backoff before the same
 * event ID is sent again.
 */
class RealtimeCommandDispatcher(
    private val scope: CoroutineScope,
    private val outbox: DurableEventOutbox,
    private val commandClient: RealtimeCommandClient,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private var channel: RealtimeRequestChannel? = null
    private var drainJob: Job? = null

    /** Starts draining on a socket whose outbox replay has finished. */
    fun activate(channel: RealtimeRequestChannel) {
        deactivate()
        this.channel = channel
        drain()
    }

    fun deactivate() {
        drainJob?.cancel()
        drainJob = null
        channel = null
    }

    /** Sends a newly persisted event after the earlier pending ones; false while no socket is ready. */
    fun dispatch(): Boolean {
        if (channel == null) return false
        drain()
        return true
    }

    private fun drain() {
        val active = channel ?: return
        if (drainJob?.isActive == true) return
        drainJob = scope.launch {
            while (channel === active) {
                val next = outbox.nextPending() ?: return@launch
                val waitMillis = next.nextAttemptAtMillis - nowMillis()
                if (waitMillis > 0) {
                    delay(waitMillis)
                    continue
                }
                try {
                    commandClient.send(next.event, active, nowMillis())
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    // The socket is gone; the durable event keeps its ID and is replayed after reconnect.
                    return@launch
                }
            }
        }
    }
}
