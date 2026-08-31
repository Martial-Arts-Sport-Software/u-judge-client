package org.mass.utils

import com.appstractive.dnssd.DiscoveryEvent
import com.appstractive.dnssd.discoverServices
import com.appstractive.dnssd.key
import org.mass.State.availableServers
import org.mass.State.removeServer
import org.mass.State.startDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object ServerConnectionUtil {
    private var scanJob: Job? = null

    fun scan(scope: CoroutineScope) {
        scanJob?.cancel()
        availableServers.clear()
        startDiscovery()
        scanJob = scope.launch {
            discoverServices("_u-judge._tcp.local.").collect {
                when (it) {
                    is DiscoveryEvent.Discovered -> {
                        availableServers.discovered(it.service)
                        it.resolve()
                    }
                    is DiscoveryEvent.Removed -> {
                        availableServers.removed(it.service.key)
                        removeServer(it.service.key)
                    }
                    is DiscoveryEvent.Resolved -> {
                        availableServers.resolved(it.service)
                    }
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
    }
}
