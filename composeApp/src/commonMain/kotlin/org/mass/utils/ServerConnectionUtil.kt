package org.mass.utils

import com.appstractive.dnssd.DiscoveryEvent
import com.appstractive.dnssd.discoverServices
import com.appstractive.dnssd.key
import org.mass.State.availableServers

object ServerConnectionUtil {
    suspend fun scan() {
        println("GOIDA STARTED!")
        discoverServices("_u-judge._tcp.local.").collect {
            when (it) {
                is DiscoveryEvent.Discovered -> {
                    println("GOIDA FOUND!")
                    availableServers[it.service.key] = it.service
                    it.resolve()
                }
                is DiscoveryEvent.Removed -> {
                    availableServers.remove(it.service.key)
                }
                is DiscoveryEvent.Resolved -> {
                    availableServers[it.service.key] = it.service
                }
            }
        }
    }
}