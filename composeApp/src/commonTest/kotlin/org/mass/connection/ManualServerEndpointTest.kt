package org.mass.connection

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class ManualServerEndpointTest {
    @Test
    fun normalizesManualHostAndPortForMetadataValidation() {
        val result = manualServerEndpoint(" court.local ", " 8080 ")

        assertEquals(
            ManualServerEndpointResult.Valid(Url("http://court.local:8080")),
            result
        )
    }

    @Test
    fun rejectsBlankHostAndOutOfRangePort() {
        assertIs<ManualServerEndpointResult.Invalid>(manualServerEndpoint("   ", "8080"))
        assertIs<ManualServerEndpointResult.Invalid>(manualServerEndpoint("192.168.1.10", "65536"))
    }

    @Test
    fun selectingManualEndpointDoesNotGrantOnlineState() {
        val store = ConnectionStateStore()
        store.dispatch(ConnectionEvent.StartDiscovery)
        store.dispatch(ConnectionEvent.SelectServer("http://192.168.1.10:8080"))

        assertEquals(
            ConnectionState.ServerSelected("http://192.168.1.10:8080"),
            store.state
        )
        assertFalse(store.isPaired)
    }
}
