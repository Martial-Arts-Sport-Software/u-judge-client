package org.mass

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.mass.connection.ConnectionEvent
import org.mass.connection.ConnectionState
import org.mass.connection.DueRealtimeOutboxReplay
import org.mass.connection.HeartbeatLifecycle
import org.mass.connection.InitialRealtimeLifecycle
import org.mass.connection.KtorRealtimeSocketOpener
import org.mass.connection.PairedServer
import org.mass.connection.RealtimeClient
import org.mass.connection.RealtimeCommandClient
import org.mass.connection.RealtimeCommandDispatcher
import org.mass.connection.RealtimeReconnectLifecycle
import org.mass.connection.ReconnectCredentialRepository
import org.mass.connection.ServerTrust
import org.mass.connection.createHttpClient
import org.mass.connection.createReconnectCredentialStorage

/**
 * The realtime session with the paired server, owned by the app rather than a screen: it survives navigation, is restored
 * after the app process is killed, and ends only when the device is revoked, the server identity changes or the judge
 * forgets the server.
 */
object PairedServerSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var credentials: ReconnectCredentialRepository
    private var reconnectLifecycle: RealtimeReconnectLifecycle? = null
    private var transport: HttpClient? = null

    /** The stored pairing, observable by the UI; null when the device must pair again. */
    var pairedServer: PairedServer? by mutableStateOf(null)
        private set

    fun initialize(context: Any?) {
        if (!::credentials.isInitialized) {
            credentials = ReconnectCredentialRepository(createReconnectCredentialStorage(context))
            pairedServer = credentials.loadPairedServer()
            pairedServer?.surname?.takeIf(String::isNotBlank)?.let { State.judgeSurname = it }
        }
    }

    /** Connects right after the operator approved this device; the credential is already stored. */
    suspend fun startAfterApproval(server: PairedServer) {
        stop()
        credentials.savePairedServer(server)
        pairedServer = server
        val (realtimeClient, lifecycle) = build(server)
        InitialRealtimeLifecycle(credentials, realtimeClient, lifecycle).start(server.deviceId, State.connection)
    }

    /** Reconnects with the stored credential after the app restarted, without a new pairing (`CLI-017`). */
    fun restore() {
        if (State.connection.state != ConnectionState.Offline || reconnectLifecycle != null) return
        val server = credentials.loadPairedServer() ?: return
        if (credentials.load() == null) return
        val (_, lifecycle) = build(server)
        State.connection.dispatch(ConnectionEvent.ResumePairedServer(server.deviceId))
        scope.launch { lifecycle.resume(State.connection) }
    }

    /** Drops the pairing on this device; a new operator approval is needed to go online again. */
    fun forget() {
        scope.launch {
            stop()
            credentials.forget()
            pairedServer = null
            State.connection.dispatch(ConnectionEvent.UseOffline)
        }
    }

    private fun build(server: PairedServer): Pair<RealtimeClient, RealtimeReconnectLifecycle> {
        val trust = ServerTrust(server.spkiSha256)
        val httpClient = createHttpClient(trust).also { transport = it }
        val dispatcher = RealtimeCommandDispatcher(scope, RealtimeCommandClient(State.eventOutbox))
        State.realtimeCommands = dispatcher
        val realtimeClient = RealtimeClient(Url(server.endpoint), KtorRealtimeSocketOpener(httpClient))
        val lifecycle = RealtimeReconnectLifecycle(
            reconnectCredentialRepository = credentials,
            realtimeClient = realtimeClient,
            heartbeatLifecycle = HeartbeatLifecycle(scope),
            outboxReplay = DueRealtimeOutboxReplay(
                State.eventOutbox,
                RealtimeCommandClient(State.eventOutbox, State.kerugiCommands::recordTerminalOutcome)
            ),
            onChannelReady = dispatcher::activate,
            onChannelClosed = dispatcher::deactivate,
            isServerIdentityChanged = { trust.identityChanged }
        )
        reconnectLifecycle = lifecycle
        return realtimeClient to lifecycle
    }

    private suspend fun stop() {
        reconnectLifecycle?.stop()
        reconnectLifecycle = null
        transport?.close()
        transport = null
    }
}
