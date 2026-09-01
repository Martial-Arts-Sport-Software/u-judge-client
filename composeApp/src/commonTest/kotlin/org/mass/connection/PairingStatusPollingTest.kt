package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class PairingStatusPollingTest {
    @Test
    fun continuesPollingWhilePendingAndStopsAfterA terminalStatus() = runTest {
        var requests = 0
        val client = PairingStatusClient(
            HttpClient(MockEngine {
                requests++
                respond(
                    if (requests == 1) {
                        """{"type":"pairing_status","state":"pending","deviceId":"android-8"}"""
                    } else {
                        """{"type":"pairing_status","state":"rejected","deviceId":"android-8","code":"operator_rejected"}"""
                    },
                    HttpStatusCode.OK
                )
            }),
            Url("http://court.local")
        )
        val statuses = mutableListOf<PairingStatusFetchResult>()

        PairingStatusPolling(client, pollingIntervalMillis = 1).poll("request-1", statuses::add)

        assertEquals(2, requests)
        assertEquals(
            listOf(
                PairingStatusFetchResult.Success(PairingStatus.Pending("android-8")),
                PairingStatusFetchResult.Success(PairingStatus.Rejected("android-8", "operator_rejected"))
            ),
            statuses
        )
    }
}
