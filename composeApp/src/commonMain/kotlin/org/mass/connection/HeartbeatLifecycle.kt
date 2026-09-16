package org.mass.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Owns the one heartbeat job permitted for an authenticated realtime socket. */
class HeartbeatLifecycle(
    private val scope: CoroutineScope,
    private val heartbeatClient: HeartbeatClient = HeartbeatClient(),
    private val heartbeatIntervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    private val heartbeatTimeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS
) {
    private var heartbeatJob: Job? = null
    private var activeChannel: RealtimeRequestChannel? = null

    suspend fun start(
        socket: RealtimeSocket,
        store: ConnectionStateStore,
        onFailure: suspend () -> Unit = {}
    ) = start(SerializedRealtimeRequestChannel(socket), store, onFailure)

    suspend fun start(
        channel: RealtimeRequestChannel,
        store: ConnectionStateStore,
        onFailure: suspend () -> Unit = {}
    ) {
        if (store.state !is ConnectionState.ConnectedIdle) return
        stop()
        activeChannel = channel
        heartbeatJob = scope.launch {
            while (true) {
                val result = try {
                    withTimeout(heartbeatTimeoutMillis) { heartbeatClient.send(channel) }
                } catch (_: TimeoutCancellationException) {
                    end(channel, store, ConnectionFailure.HeartbeatUnavailable, onFailure)
                    return@launch
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    end(channel, store, ConnectionFailure.HeartbeatUnavailable, onFailure)
                    return@launch
                }
                when (result) {
                    HeartbeatResult.Acknowledged -> delay(heartbeatIntervalMillis)
                    is HeartbeatResult.Rejected -> {
                        end(channel, store, ConnectionFailure.HeartbeatRejected(result.code), onFailure)
                        return@launch
                    }
                    HeartbeatResult.InvalidResponse -> {
                        end(channel, store, ConnectionFailure.HeartbeatResponseInvalid, onFailure)
                        return@launch
                    }
                }
            }
        }
    }

    suspend fun stop() {
        val channel = activeChannel ?: return
        heartbeatJob?.cancelAndJoin()
        heartbeatJob = null
        activeChannel = null
        channel.close()
    }

    private suspend fun end(
        channel: RealtimeRequestChannel,
        store: ConnectionStateStore,
        failure: ConnectionFailure,
        onFailure: suspend () -> Unit
    ) {
        store.dispatch(ConnectionEvent.HeartbeatFailed(failure))
        if (activeChannel === channel) {
            activeChannel = null
        }
        channel.close()
        onFailure()
    }

    private companion object {
        const val DEFAULT_INTERVAL_MILLIS = 15_000L
        const val DEFAULT_TIMEOUT_MILLIS = 5_000L
    }
}
