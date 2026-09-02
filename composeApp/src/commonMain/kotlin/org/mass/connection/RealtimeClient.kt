package org.mass.connection

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Clock

interface RealtimeSocket {
    suspend fun send(payload: String)

    suspend fun receive(): String

    suspend fun close()
}

fun interface RealtimeSocketOpener {
    suspend fun open(endpoint: Url): RealtimeSocket
}

class KtorRealtimeSocketOpener(private val httpClient: HttpClient) : RealtimeSocketOpener {
    override suspend fun open(endpoint: Url): RealtimeSocket = KtorRealtimeSocket(
        httpClient.webSocketSession(urlString = endpoint.toString())
    )
}

data class RealtimeHandshakeRequest(
    val deviceId: String,
    val reconnectCredential: String
)

sealed interface RealtimeHandshakeResult {
    data class Accepted(val socket: RealtimeSocket) : RealtimeHandshakeResult
    data class Rejected(val code: String) : RealtimeHandshakeResult
    data class ClockSyncRejected(val code: String) : RealtimeHandshakeResult
    data object ClockSyncInvalidResponse : RealtimeHandshakeResult
    data object InvalidResponse : RealtimeHandshakeResult
    data object Unavailable : RealtimeHandshakeResult
}

/** Opens the authenticated realtime channel and accepts online access only after the server reply. */
class RealtimeClient(
    private val endpoint: Url,
    private val socketOpener: RealtimeSocketOpener,
    private val protocolVersion: String = "1.0",
    private val clockSyncClient: ClockSyncClient = ClockSyncClient {
        Clock.System.now().toEpochMilliseconds()
    }
) {
    suspend fun connect(
        request: RealtimeHandshakeRequest,
        store: ConnectionStateStore
    ): RealtimeHandshakeResult {
        var socket: RealtimeSocket? = null
        return try {
            val openedSocket = socketOpener.open(endpoint.realtimeUrl())
            socket = openedSocket
            openedSocket.send(request.toJson())
            when (val response = decodeResponse(openedSocket.receive())) {
                HandshakeResponse.Accepted -> {
                    when (val clockSyncResult = clockSyncClient.synchronize(openedSocket)) {
                        is ClockSyncResult.Synchronized -> {
                            store.dispatch(
                                ConnectionEvent.AcceptPairing(
                                    deviceId = request.deviceId,
                                    clockOffsetMillis = clockSyncResult.offsetMillis
                                )
                            )
                            RealtimeHandshakeResult.Accepted(openedSocket)
                        }
                        is ClockSyncResult.Rejected -> {
                            openedSocket.close()
                            store.dispatch(
                                ConnectionEvent.RejectRealtime(
                                    ConnectionFailure.ClockSyncRejected(clockSyncResult.code)
                                )
                            )
                            RealtimeHandshakeResult.ClockSyncRejected(clockSyncResult.code)
                        }
                        ClockSyncResult.InvalidResponse -> {
                            openedSocket.close()
                            store.dispatch(ConnectionEvent.RejectRealtime(ConnectionFailure.ClockSyncResponseInvalid))
                            RealtimeHandshakeResult.ClockSyncInvalidResponse
                        }
                    }
                }
                is HandshakeResponse.Rejected -> {
                    openedSocket.close()
                    store.dispatch(ConnectionEvent.RejectRealtime(ConnectionFailure.RealtimeHandshakeRejected(response.code)))
                    RealtimeHandshakeResult.Rejected(response.code)
                }
                HandshakeResponse.Invalid -> {
                    openedSocket.close()
                    store.dispatch(ConnectionEvent.RejectRealtime(ConnectionFailure.RealtimeResponseInvalid))
                    RealtimeHandshakeResult.InvalidResponse
                }
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) {
                try {
                    socket?.close()
                } catch (_: Exception) {
                    // Cancellation must not be replaced by a close failure.
                }
                throw exception
            }
            try {
                socket?.close()
            } catch (_: Exception) {
                // The original transport failure is more useful than a close failure.
            }
            store.dispatch(ConnectionEvent.RejectRealtime(ConnectionFailure.RealtimeUnavailable))
            RealtimeHandshakeResult.Unavailable
        }
    }

    private fun RealtimeHandshakeRequest.toJson(): String = buildJsonObject {
        put("type", "handshake")
        put("protocolVersion", protocolVersion)
        put("reconnectCredential", reconnectCredential)
    }.toString()

    private fun decodeResponse(payload: String): HandshakeResponse = try {
        val body = Json.parseToJsonElement(payload).jsonObject
        when (body["type"]?.jsonPrimitive?.content) {
            "handshake_accepted" -> HandshakeResponse.Accepted
            "handshake_rejected" -> body["code"]?.jsonPrimitive?.content
                ?.takeIf { it.isNotBlank() }
                ?.let(HandshakeResponse::Rejected)
                ?: HandshakeResponse.Invalid
            else -> HandshakeResponse.Invalid
        }
    } catch (_: Exception) {
        HandshakeResponse.Invalid
    }

    private fun Url.realtimeUrl(): Url = URLBuilder(this).apply {
        protocol = if (protocol == URLProtocol.HTTPS) URLProtocol.WSS else URLProtocol.WS
        appendPathSegments("v1", "realtime")
    }.build()

    private sealed interface HandshakeResponse {
        data object Accepted : HandshakeResponse
        data class Rejected(val code: String) : HandshakeResponse
        data object Invalid : HandshakeResponse
    }
}

private class KtorRealtimeSocket(private val session: DefaultClientWebSocketSession) : RealtimeSocket {
    override suspend fun send(payload: String) {
        session.send(Frame.Text(payload))
    }

    override suspend fun receive(): String = (session.incoming.receive() as? Frame.Text)?.readText()
        ?: throw IllegalStateException("Expected a text WebSocket frame")

    override suspend fun close() {
        session.close()
    }
}
