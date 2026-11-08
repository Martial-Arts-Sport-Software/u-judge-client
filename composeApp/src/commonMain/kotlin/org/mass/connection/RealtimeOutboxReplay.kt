package org.mass.connection

import org.mass.transport.DurableEventOutbox
import kotlin.time.Clock

/** Replays due durable commands in sequence before a socket starts normal realtime traffic. */
fun interface RealtimeOutboxReplay {
    suspend fun replay(socket: RealtimeSocket): Boolean
}

class DueRealtimeOutboxReplay(
    private val outbox: DurableEventOutbox,
    private val commandClient: RealtimeCommandClient = RealtimeCommandClient(outbox),
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : RealtimeOutboxReplay {
    override suspend fun replay(socket: RealtimeSocket): Boolean {
        while (true) {
            val event = outbox.nextEventDue(nowMillis()) ?: return true
            when (commandClient.send(event, socket, nowMillis())) {
                is RealtimeCommandResult.Accepted,
                is RealtimeCommandResult.Rejected -> Unit
                // The journal is down: later events wait behind this one, which the dispatcher retries after backoff.
                is RealtimeCommandResult.Retry -> return true
                RealtimeCommandResult.InvalidResponse -> return false
            }
        }
    }
}
