package org.mass.discovery

import kotlin.test.Test
import kotlin.test.assertEquals

class ServerDiscoveryStoreTest {
    @Test
    fun discoveredServerIsReplacedByItsResolvedDetails() {
        val store = ServerDiscoveryStore<Server> { it.key }

        store.discovered(Server("court-1", "Court 1", emptyList()))
        store.resolved(Server("court-1", "Main court", listOf("192.168.1.10")))

        assertEquals(
            listOf(
                DiscoveredServer(
                    server = Server("court-1", "Main court", listOf("192.168.1.10")),
                    status = DiscoveryStatus.Available
                )
            ),
            store.servers
        )
    }

    @Test
    fun discoveredServerIsResolvingUntilItsDetailsAreAvailable() {
        val store = ServerDiscoveryStore<Server> { it.key }
        val server = Server("court-1", "Court 1", emptyList())

        store.discovered(server)

        assertEquals(
            listOf(DiscoveredServer(server, DiscoveryStatus.Resolving)),
            store.servers
        )
    }

    @Test
    fun removedServerIsNoLongerAvailableForSelection() {
        val store = ServerDiscoveryStore<Server> { it.key }
        val server = Server("court-1", "Main court", listOf("192.168.1.10"))

        store.discovered(server)
        store.removed(server.key)

        assertEquals(emptyList(), store.servers)
    }

    private data class Server(val key: String, val name: String, val addresses: List<String>)
}
