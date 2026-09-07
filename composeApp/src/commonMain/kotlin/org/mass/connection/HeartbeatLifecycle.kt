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
    private var activeSocket: RealtimeSocket? = null

    suspend fun start(
        socket: RealtimeSocket,
        store: ConnectionStateStore,
        onFailure: suspend () -> Unit = {}
    ) {
        if (store.state !is ConnectionState.ConnectedIdle) return
        stop()
        activeSocket = socket
        heartbeatJob = scope.launch {
            while (true) {
                val result = try {
                    withTimeout(heartbeatTimeoutMillis) { heartbeatClient.send(socket) }
                } catch (_: TimeoutCancellationException) {
                    end(socket, store, ConnectionFailure.HeartbeatUnavailable, onFailure)
                    return@launch
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    end(socket, store, ConnectionFailure.HeartbeatUnavailable, onFailure)
                    return@launch
                }
                when (result) {
                    HeartbeatResult.Acknowledged -> delay(heartbeatIntervalMillis)
                    is HeartbeatResult.Rejected -> {
                        end(socket, store, ConnectionFailure.HeartbeatRejected(result.code), onFailure)
                        return@launch
                    }
                    HeartbeatResult.InvalidResponse -> {
                        end(socket, store, ConnectionFailure.HeartbeatResponseInvalid, onFailure)
                        return@launch
                    }
                }
            }
        }
    }

    suspend fun stop() {
        val socket = activeSocket ?: return
        heartbeatJob?.cancelAndJoin()
        heartbeatJob = null
        activeSocket = null
        socket.close()
    }

    private suspend fun end(
        socket: RealtimeSocket,
        store: ConnectionStateStore,
        failure: ConnectionFailure,
        onFailure: suspend () -> Unit
    ) {
        store.dispatch(ConnectionEvent.HeartbeatFailed(failure))
        if (activeSocket === socket) {
            activeSocket = null
        }
        socket.close()
        onFailure()
    }

    private companion object {
        const val DEFAULT_INTERVAL_MILLIS = 15_000L
        const val DEFAULT_TIMEOUT_MILLIS = 5_000L
    }
}
