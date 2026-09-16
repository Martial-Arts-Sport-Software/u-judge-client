package org.mass.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.mass.transport.OutboxEvent
import kotlin.time.Clock

/** Dispatches durable commands through the currently authenticated socket, if one is available. */
class RealtimeCommandDispatcher(
    private val scope: CoroutineScope,
    private val commandClient: RealtimeCommandClient,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private var channel: RealtimeRequestChannel? = null

    fun activate(channel: RealtimeRequestChannel) {
        this.channel = channel
    }

    fun deactivate() {
        channel = null
    }

    fun dispatch(event: OutboxEvent, onResult: (RealtimeCommandResult) -> Unit): Boolean {
        val activeChannel = channel ?: return false
        scope.launch {
            try {
                onResult(commandClient.send(event, activeChannel, nowMillis()))
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                // The durable event remains pending and reconnect replay will retry its original ID.
            }
        }
        return true
    }
}
