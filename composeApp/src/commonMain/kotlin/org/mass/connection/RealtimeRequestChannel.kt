package org.mass.connection

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes complete request/response exchanges on one realtime socket. */
interface RealtimeRequestChannel {
    suspend fun exchange(payload: String): String

    suspend fun close()
}

class SerializedRealtimeRequestChannel(
    private val socket: RealtimeSocket
) : RealtimeRequestChannel {
    private val exchangeMutex = Mutex()

    override suspend fun exchange(payload: String): String = exchangeMutex.withLock {
        socket.send(payload)
        socket.receive()
    }

    override suspend fun close() = socket.close()
}
