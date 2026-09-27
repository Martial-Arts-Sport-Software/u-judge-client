package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.http.Url
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The server ends a session by closing the socket (revocation, restart); the OkHttp client must notice it through the
 * heartbeat and reconnect, and a rejected reconnect must end the session.
 */
class ServerClosedSocketTest {
    @Test
    fun revokedSessionClosedByTheServerEndsInDeviceRevoked() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val handshakes = AtomicInteger()
        val server = embeddedServer(CIO, port = port) {
            install(WebSockets)
            routing {
                webSocket("/v1/realtime") {
                    (incoming.receive() as Frame.Text).readText()
                    if (handshakes.incrementAndGet() > 1) {
                        send(Frame.Text("""{"type":"handshake_rejected","code":"invalid_reconnect_credential"}"""))
                        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "invalid_reconnect_credential"))
                        return@webSocket
                    }
                    send(Frame.Text("""{"type":"handshake_accepted"}"""))
                    val sync = (incoming.receive() as Frame.Text).readText()
                    val sent = Regex(""""clientSendTimestamp":"([^"]+)"""").find(sync)!!.groupValues[1]
                    send(Frame.Text("""{"type":"clock_sync_response","clientSendTimestamp":"$sent","serverReceiveTimestamp":"$sent","serverSendTimestamp":"$sent"}"""))
                    repeat(2) {
                        (incoming.receive() as Frame.Text).readText()
                        send(Frame.Text("""{"type":"heartbeat_ack"}"""))
                    }
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "credential_revoked"))
                }
            }
        }.start(wait = false)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val storage = object : ReconnectCredentialStorage {
            var credential: String? = "credential-1"
            override fun load() = credential
            override fun save(credential: String) { this.credential = credential }
            override fun clear() { credential = null }
        }
        val httpClient = HttpClient(OkHttp) { install(ClientWebSockets) }
        val store = ConnectionStateStore().apply { dispatch(ConnectionEvent.ResumePairedServer("device-1")) }
        val lifecycle = RealtimeReconnectLifecycle(
            reconnectCredentialRepository = ReconnectCredentialRepository(storage),
            realtimeClient = RealtimeClient(Url("http://127.0.0.1:$port"), KtorRealtimeSocketOpener(httpClient)),
            heartbeatLifecycle = HeartbeatLifecycle(scope, heartbeatIntervalMillis = 200, heartbeatTimeoutMillis = 1_000),
            retryDelayMillis = { 100 }
        )
        try {
            scope.launch { lifecycle.resume(store) }
            withTimeout(10_000) {
                while (store.state !is ConnectionState.Rejected) delay(50)
            }
            assertEquals(ConnectionState.Rejected(PAIRED_SERVER_KEY, ConnectionFailure.DeviceRevoked), store.state)
            assertEquals(null, storage.credential)
        } finally {
            println("state=${store.state} handshakes=${handshakes.get()}")
            lifecycle.stop()
            scope.cancel()
            httpClient.close()
            server.stop(0, 0)
        }
    }
}
