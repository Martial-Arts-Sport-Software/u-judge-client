package org.mass.discovery

import androidx.compose.runtime.mutableStateMapOf

enum class DiscoveryStatus {
    Resolving,
    Available
}

data class DiscoveredServer<T>(
    val server: T,
    val status: DiscoveryStatus
)

class ServerDiscoveryStore<T>(private val keyOf: (T) -> String) {
    private val serversByKey = mutableStateMapOf<String, DiscoveredServer<T>>()

    val servers: List<DiscoveredServer<T>>
        get() = serversByKey.values.toList()

    fun discovered(server: T) {
        serversByKey[keyOf(server)] = DiscoveredServer(server, DiscoveryStatus.Resolving)
    }

    fun resolved(server: T) {
        serversByKey[keyOf(server)] = DiscoveredServer(server, DiscoveryStatus.Available)
    }

    fun removed(key: String) {
        serversByKey.remove(key)
    }

    fun clear() {
        serversByKey.clear()
    }
}
