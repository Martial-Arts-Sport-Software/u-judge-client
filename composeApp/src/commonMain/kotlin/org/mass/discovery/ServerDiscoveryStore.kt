package org.mass.discovery

import androidx.compose.runtime.mutableStateMapOf

class ServerDiscoveryStore<T>(private val keyOf: (T) -> String) {
    private val serversByKey = mutableStateMapOf<String, T>()

    val servers: List<T>
        get() = serversByKey.values.toList()

    fun discovered(server: T) {
        serversByKey[keyOf(server)] = server
    }

    fun resolved(server: T) {
        serversByKey[keyOf(server)] = server
    }

    fun removed(key: String) {
        serversByKey.remove(key)
    }

    fun clear() {
        serversByKey.clear()
    }
}
