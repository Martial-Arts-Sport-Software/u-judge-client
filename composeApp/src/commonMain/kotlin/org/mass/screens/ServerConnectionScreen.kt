package org.mass.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appstractive.dnssd.key
import io.ktor.http.Url
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.mass.PairedServerSession
import org.mass.State
import org.mass.State.availableServers
import org.mass.State.connection
import org.mass.State.pairingIdentity
import org.mass.State.selectManualServer
import org.mass.State.selectServer
import org.mass.connection.ConnectionState
import org.mass.connection.ConnectionStatusPresentation
import org.mass.connection.ManualServerEndpointResult
import org.mass.connection.PairedServer
import org.mass.connection.PairingClient
import org.mass.connection.PairingFlow
import org.mass.connection.PairingRequest
import org.mass.connection.PairingResult
import org.mass.connection.PairingStatusClient
import org.mass.connection.PairingStatusFlow
import org.mass.connection.PairingStatusPolling
import org.mass.connection.PairingStatusResult
import org.mass.connection.RecentServer
import org.mass.connection.ReconnectCredentialRepository
import org.mass.connection.ServerMetadataClient
import org.mass.connection.ServerTrust
import org.mass.connection.connectionStatusPresentation
import org.mass.connection.createHttpClient
import org.mass.connection.createReconnectCredentialStorage
import org.mass.connection.formatVerificationCode
import org.mass.connection.manualServerEndpoint
import org.mass.connection.metadataEndpoint
import org.mass.connection.relativeTime
import org.mass.discovery.DiscoveryStatus
import org.mass.enums.Colors
import org.mass.enums.Routes
import org.mass.getContext
import org.mass.getPlatformName
import org.mass.locale.Localization
import org.mass.ui.button.ButtonComponent
import org.mass.ui.button.ButtonStyles
import org.mass.ui.button.clickWithTransition
import org.mass.ui.input.TextInputComponent
import org.mass.ui.popup.Popup
import org.mass.utils.DiscoveryPermissionEffect
import org.mass.utils.ServerConnectionUtil
import u_judge_client.composeapp.generated.resources.Res
import u_judge_client.composeapp.generated.resources.arrow_right_icon
import u_judge_client.composeapp.generated.resources.back_icon
import u_judge_client.composeapp.generated.resources.check_small_icon
import u_judge_client.composeapp.generated.resources.monitor_icon
import kotlin.time.Clock

/**
 * Server connection: Wi-Fi search or a manual address while unpaired, the pairing progress with the verification code,
 * and the current server with its status once the device is paired.
 */
object ServerConnectionScreen : Screen {
    private enum class Tab { SEARCH, MANUAL }

    @Composable
    override fun Load() {
        val goBackOnclick = remember { {
            if (PairedServerSession.isPaired) {
                State.currentPopupMode = Popup.Modes.LEAVE_PAIRED_SERVER
            } else {
                clickWithTransition(Routes.BACK)
            }
        } }
        val coroutineScope = rememberCoroutineScope()
        val context = getContext()

        var tab by remember { mutableStateOf(Tab.SEARCH) }
        var manualHost by remember { mutableStateOf("") }
        var manualPort by remember { mutableStateOf("8443") }
        var manualEndpointError by remember { mutableStateOf(false) }
        var pairingStatus by remember { mutableStateOf<PairingStatusResult?>(null) }
        var pairingJob by remember { mutableStateOf<Job?>(null) }
        var serverTrust by remember { mutableStateOf<ServerTrust?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                pairingJob?.cancel()
                ServerConnectionUtil.stopScan()
            }
        }

        fun startPairing(endpoint: Url) {
            pairingJob?.cancel()
            pairingStatus = null
            pairingJob = coroutineScope.launch {
                val trust = ServerTrust().also { serverTrust = it }
                createHttpClient(trust).use { httpClient ->
                    val credentialRepository = ReconnectCredentialRepository(createReconnectCredentialStorage(context))
                    val deliveryProof = PairingClient.newDeliveryProof()
                    credentialRepository.savePairingDeliveryProof(deliveryProof)
                    val pairingResult = PairingFlow(
                        ServerMetadataClient(httpClient, endpoint),
                        PairingClient(httpClient, endpoint)
                    ).connect(
                        PairingRequest(
                            deviceId = pairingIdentity.deviceId(),
                            surname = State.judgeSurname.trim(),
                            platform = getPlatformName(),
                            deliveryProof = deliveryProof
                        ),
                        connection
                    )
                    pairingStatus = pairingResult.pollStatus(httpClient, endpoint, credentialRepository)
                    startRealtimeAfterPairingAcceptance(pairingStatus, endpoint, trust)
                }
            }
        }

        fun connectManually(host: String, port: String) {
            when (val result = manualServerEndpoint(host, port)) {
                ManualServerEndpointResult.Invalid -> manualEndpointError = true
                is ManualServerEndpointResult.Valid -> {
                    manualEndpointError = false
                    selectManualServer(result)
                    startPairing(result.endpoint)
                }
            }
        }

        val pairedServer = PairedServerSession.pairedServer
        val state = connection.state
        // Once paired the judge sees the current server; switching servers starts with "Forget this server".
        val showCurrent = pairedServer != null && (
            state is ConnectionState.ConnectedIdle || state is ConnectionState.Reconnecting || state is ConnectionState.Rejected
            )

        val searching = !showCurrent && tab == Tab.SEARCH
        if (searching) {
            DiscoveryPermissionEffect(key = searching) {
                // A running pairing must not be reset to discovery by the permission answer or a tab switch.
                if (pairingJob?.isActive != true) ServerConnectionUtil.scan(coroutineScope)
            }
        } else {
            LaunchedEffect(Unit) { ServerConnectionUtil.stopScan() }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                ButtonComponent(
                    style = ButtonStyles.Icon,
                    iconSrc = Res.drawable.back_icon,
                    onclick = goBackOnclick,
                    modifier = Modifier.height(48.dp)
                )
                Spacer(Modifier.weight(0.8f))
                Text(text = Localization.getString("connection_title"), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            // A gray panel like the server's, not the whole screen, carries the connection content. The tab selector stays in
            // place; only the server list and the recent addresses scroll.
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Colors.GRAY.color)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (showCurrent && pairedServer != null) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    ) {
                        CurrentServer(pairedServer, state) {
                            pairingJob?.cancel()
                            pairingStatus = null
                            serverTrust = null
                            PairedServerSession.forget()
                        }
                    }
                } else {
                    TabSelector(tab) { tab = it }
                    when (tab) {
                        Tab.SEARCH -> SearchTab(
                            onConnect = { service, address ->
                                selectServer(service)
                                startPairing(metadataEndpoint(address, service.port))
                            },
                            onManual = { tab = Tab.MANUAL }
                        )
                        Tab.MANUAL -> ManualTab(
                            host = manualHost,
                            port = manualPort,
                            error = manualEndpointError,
                            onHost = { manualHost = it },
                            onPort = { manualPort = it },
                            onConnect = { connectManually(manualHost, manualPort) },
                            onRecent = { recent ->
                                manualHost = recent.host
                                manualPort = recent.port.toString()
                                connectManually(recent.host, recent.port.toString())
                            }
                        )
                    }
                    PairingProgress(connectionStatus(pairingStatus), serverTrust?.verificationCode)
                }
            }
        }
    }

    @Composable
    private fun TabSelector(selected: Tab, onSelect: (Tab) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Colors.SECONDARY.color)
                .padding(4.dp)
        ) {
            listOf(Tab.SEARCH to "connection_tab_search", Tab.MANUAL to "connection_tab_manual").forEach { (tab, key) ->
                val active = tab == selected
                Text(
                    text = Localization.getString(key),
                    color = if (active) Color.White else Colors.PRIMARY.color,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) Colors.PRIMARY.color else Color.Transparent)
                        .clickable(role = Role.Tab) { onSelect(tab) }
                        .padding(vertical = 10.dp)
                )
            }
        }
    }

    /** The status line, one block with the found servers that fills the free height and scrolls, and the manual hint. */
    @Composable
    private fun ColumnScope.SearchTab(
        onConnect: (com.appstractive.dnssd.DiscoveredService, String) -> Unit,
        onManual: () -> Unit
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(50)).background(Colors.GREEN.color))
            Spacer(Modifier.width(10.dp))
            Text(Localization.getString("connection_searching"), style = MaterialTheme.typography.bodyLarge, color = Color.White)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(CARD_SHAPE)
                .background(Colors.SECONDARY.color)
        ) {
            if (availableServers.servers.isEmpty()) {
                Text(
                    Localization.getString("connection_search_empty"),
                    color = Colors.PRIMARY.color,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)
                ) {
                    availableServers.servers.forEach { discovered ->
                        val service = discovered.server
                        val address = service.addresses.firstOrNull()
                        val available = discovered.status == DiscoveryStatus.Available && address != null
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.55f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            IconTile(Res.drawable.monitor_icon)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(service.name, color = Colors.PRIMARY.color, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    address?.let { "$it:${service.port}" } ?: Localization.getString("connection_court_resolving"),
                                    color = Colors.PRIMARY.color.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            PrimaryButton(
                                text = Localization.getString(if (available) "connection_connect_btn" else "connection_court_resolving"),
                                enabled = available && connection.state is ConnectionState.Discovering,
                                onClick = { if (address != null) onConnect(service, address) }
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = Localization.getString("connection_search_hint"),
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onManual).padding(vertical = 4.dp)
        )
    }

    /**
     * Host and port with an icon connect button on the same line, the button in the column of the recent addresses'
     * buttons and the fields as high as it; the recent addresses scroll on their own below.
     */
    @Composable
    private fun ColumnScope.ManualTab(
        host: String,
        port: String,
        error: Boolean,
        onHost: (String) -> Unit,
        onPort: (String) -> Unit,
        onConnect: () -> Unit,
        onRecent: (RecentServer) -> Unit
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(3f)) {
                FieldLabel("connection_host_label")
                TextInputComponent(
                    inputValue = host,
                    onChange = onHost,
                    modifier = Modifier.fillMaxWidth(),
                    fieldHeight = ICON_TILE_SIZE,
                    bottomSpacing = 0.dp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1.2f)) {
                FieldLabel("connection_port_label")
                TextInputComponent(
                    inputValue = port,
                    onChange = onPort,
                    modifier = Modifier.fillMaxWidth(),
                    fieldHeight = ICON_TILE_SIZE,
                    bottomSpacing = 0.dp
                )
            }
            Spacer(Modifier.width(12.dp))
            val label = Localization.getString("connection_manual_connect_btn")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    // Same size and right inset as the recent addresses' buttons inside their cards.
                    .padding(end = CARD_HORIZONTAL_PADDING)
                    .size(ICON_TILE_SIZE)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Colors.PRIMARY.color)
                    .clickable(role = Role.Button, onClickLabel = label, onClick = onConnect)
                    .semantics { contentDescription = label }
            ) {
                Image(
                    painterResource(Res.drawable.arrow_right_icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (error) {
            Text(
                Localization.getString("connection_error_manual_endpoint"),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        val recent = State.recentServers.all()
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            FieldLabel("connection_recent_title")
            val now = Clock.System.now().toEpochMilliseconds()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
            ) {
                recent.forEach { server ->
                    val time = relativeTime(now, server.lastUsedMillis)
                    val timeText = Localization.getString(time.key).replace("%s", time.amount?.toString().orEmpty())
                    Card {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(Colors.PRIMARY.color))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                server.address,
                                color = Colors.PRIMARY.color,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(timeText, color = Colors.PRIMARY.color.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                        }
                        IconTile(
                            Res.drawable.arrow_right_icon,
                            modifier = Modifier
                                .clickable(role = Role.Button) { onRecent(server) }
                                .semantics { contentDescription = "${Localization.getString("connection_connect_btn")} ${server.address}" }
                        )
                    }
                }
            }
        } else {
            // Keeps the pairing progress at the bottom of the panel, as on the search tab.
            Spacer(Modifier.weight(1f))
        }
    }

    /** Pending approval with the code to compare, or the reason the last attempt failed. */
    @Composable
    private fun PairingProgress(status: ConnectionStatusPresentation?, code: String?) {
        status ?: return
        val pending = connection.state is ConnectionState.PairingPending
        val failed = connection.state is ConnectionState.Rejected
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CARD_SHAPE)
                .background(if (failed) ERROR_BACKGROUND else Colors.SECONDARY.color)
                .then(if (failed) Modifier.border(1.dp, ERROR_BORDER, CARD_SHAPE) else Modifier)
                .padding(16.dp)
        ) {
            Text(
                Localization.getString(if (pending) "connection_pending_title" else status.statusKey),
                color = if (failed) ERROR_TEXT else Colors.PRIMARY.color,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (pending && code != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    formatVerificationCode(code),
                    color = Colors.PRIMARY.color,
                    fontSize = 34.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Text(
                    Localization.getString("connection_code_hint"),
                    color = Colors.PRIMARY.color.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            status.reasonKey?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    Localization.getString(it),
                    color = if (failed) ERROR_TEXT else Colors.PRIMARY.color,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun CurrentServer(server: PairedServer, state: ConnectionState, onForget: () -> Unit) {
        FieldLabel("connection_current_server")
        val subline = when (state) {
            is ConnectionState.ConnectedIdle -> "connection_state_connected"
            is ConnectionState.Reconnecting -> "connection_state_reconnecting"
            else -> "connection_state_no_access"
        }
        Card {
            IconTile(Res.drawable.monitor_icon)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    server.endpoint.removePrefix("https://"),
                    color = Colors.PRIMARY.color,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    listOf(server.surname, Localization.getString(subline)).filter(String::isNotBlank).joinToString(" · "),
                    color = Colors.PRIMARY.color.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        val presentation = connectionStatusPresentation(state)
            ?: (state as? ConnectionState.Rejected)?.let { ConnectionStatusPresentation(it.failure.localizationKey) }
        val (background, border, text) = when (state) {
            is ConnectionState.ConnectedIdle -> Triple(SUCCESS_BACKGROUND, SUCCESS_BORDER, SUCCESS_TEXT)
            is ConnectionState.Reconnecting -> Triple(WARNING_BACKGROUND, WARNING_BORDER, WARNING_TEXT)
            else -> Triple(ERROR_BACKGROUND, ERROR_BORDER, ERROR_TEXT)
        }
        presentation?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CARD_SHAPE)
                    .background(background)
                    .border(1.dp, border, CARD_SHAPE)
                    .padding(16.dp)
            ) {
                if (state is ConnectionState.ConnectedIdle) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(50)).background(SUCCESS_BORDER.copy(alpha = 0.2f))) {
                        Image(painterResource(Res.drawable.check_small_icon), contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                }
                Column {
                    Text(Localization.getString(it.statusKey), color = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    it.reasonKey?.let { reason ->
                        Text(Localization.getString(reason), color = text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            Localization.getString("connection_forget_server_btn"),
            color = ERROR_TEXT,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CARD_SHAPE)
                .background(ERROR_BACKGROUND)
                .border(BorderStroke(1.dp, ERROR_BORDER), CARD_SHAPE)
                .clickable(role = Role.Button, onClick = onForget)
                .padding(vertical = 14.dp)
        )
    }

    @Composable
    private fun Card(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CARD_SHAPE)
                .background(Colors.SECONDARY.color)
                .padding(horizontal = CARD_HORIZONTAL_PADDING, vertical = 12.dp),
            content = content
        )
    }

    @Composable
    private fun IconTile(icon: DrawableResource, modifier: Modifier = Modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(ICON_TILE_SIZE)
                .clip(RoundedCornerShape(12.dp))
                .background(Colors.PRIMARY.color.copy(alpha = 0.15f))
                .then(modifier)
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }

    @Composable
    private fun FieldLabel(key: String) {
        Text(
            Localization.getString(key).uppercase(),
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }

    @Composable
    private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Colors.PRIMARY.color,
                disabledContainerColor = Colors.PRIMARY.color.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.8f)
            )
        ) {
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    private suspend fun PairingResult?.pollStatus(
        httpClient: io.ktor.client.HttpClient,
        endpoint: Url,
        credentialRepository: ReconnectCredentialRepository
    ): PairingStatusResult? = when (this) {
        is PairingResult.Pending -> PairingStatusFlow(
            PairingStatusPolling(
                fetch = { requestId -> PairingStatusClient(httpClient, endpoint).fetch(requestId, deliveryProof) }
            ),
            credentialRepository
        ).awaitStatus(this, connection)
        else -> null
    }

    private suspend fun startRealtimeAfterPairingAcceptance(
        pairingStatus: PairingStatusResult?,
        endpoint: Url,
        trust: ServerTrust
    ) {
        if (pairingStatus !is PairingStatusResult.Accepted) return
        val pin = trust.pinnedSpki ?: return
        State.recentServers.record(endpoint.host, endpoint.port)
        PairedServerSession.startAfterApproval(
            PairedServer(endpoint.toString(), pairingIdentity.deviceId(), pin, State.judgeSurname.trim())
        )
    }

    @Composable
    private fun connectionStatus(pairingStatus: PairingStatusResult?): ConnectionStatusPresentation? = when (val state = connection.state) {
        is ConnectionState.Rejected -> ConnectionStatusPresentation(state.failure.localizationKey)
        // The live connection outranks the pairing answer that led to it.
        is ConnectionState.ConnectedIdle, is ConnectionState.Reconnecting -> connectionStatusPresentation(state)
        else -> when (pairingStatus) {
            is PairingStatusResult.Accepted -> ConnectionStatusPresentation("connection_pairing_accepted")
            else -> when (state) {
                is ConnectionState.PairingPending -> ConnectionStatusPresentation("connection_pairing_pending")
                else -> connectionStatusPresentation(state)
            }
        }
    }

    private val CARD_SHAPE = RoundedCornerShape(16.dp)
    private val CARD_HORIZONTAL_PADDING = 14.dp
    private val ICON_TILE_SIZE = 44.dp
    private val SUCCESS_BACKGROUND = Color(0xFFDDF4E4)
    private val SUCCESS_BORDER = Color(0xFF2E9E5B)
    private val SUCCESS_TEXT = Color(0xFF17603A)
    private val WARNING_BACKGROUND = Color(0xFFFFF1D6)
    private val WARNING_BORDER = Color(0xFFD9A441)
    private val WARNING_TEXT = Color(0xFF7A4E00)
    private val ERROR_BACKGROUND = Color(0xFFFDE2E6)
    private val ERROR_BORDER = Color(0xFFE39AA8)
    private val ERROR_TEXT = Color(0xFFA3182F)
}
