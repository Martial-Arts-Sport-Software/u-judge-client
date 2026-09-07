package org.mass.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.appstractive.dnssd.key
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mass.State
import org.mass.discovery.DiscoveryStatus
import org.mass.State.availableServers
import org.mass.State.connection
import org.mass.State.pairingIdentity
import org.mass.State.selectedServer
import org.mass.State.selectManualServer
import org.mass.State.selectServer
import org.mass.getPlatformName
import org.mass.connection.ConnectionState
import org.mass.connection.ConnectionStatusPresentation
import org.mass.connection.PairingClient
import org.mass.connection.PairingFlow
import org.mass.connection.PairingRequest
import org.mass.connection.PairingResult
import org.mass.connection.PairingStatusClient
import org.mass.connection.PairingStatusFlow
import org.mass.connection.PairingStatusPolling
import org.mass.connection.PairingStatusResult
import org.mass.connection.createHttpClient
import org.mass.connection.connectionStatusPresentation
import org.mass.connection.metadataEndpoint
import org.mass.connection.manualServerEndpoint
import org.mass.connection.ManualServerEndpointResult
import org.mass.connection.ServerMetadataClient
import org.mass.enums.Colors
import org.mass.enums.Routes
import org.mass.locale.Localization
import org.mass.ui.button.ButtonComponent
import org.mass.ui.button.ButtonStyles
import org.mass.ui.button.clickWithTransition
import org.mass.ui.input.TextInputComponent
import org.mass.utils.ServerConnectionUtil
import u_judge_client.composeapp.generated.resources.Res
import u_judge_client.composeapp.generated.resources.back_icon

object ServerConnectionScreen : Screen {
    @Composable
    override fun Load() {

        val goBackOnclick = remember { {
            clickWithTransition(Routes.BACK)
        } }

        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxHeight(0.15f)
                    .fillMaxWidth()
            ) {
                ButtonComponent(
                    style = ButtonStyles.Icon,
                    iconSrc = Res.drawable.back_icon,
                    onclick = goBackOnclick,
                    modifier = Modifier
                        .fillMaxHeight()
                )
                Spacer(Modifier.weight(0.8f))
                Text(
                    text = Localization.getString("connection_title"),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            var manualHost by remember { mutableStateOf("") }
            var manualPort by remember { mutableStateOf("8080") }
            var manualEndpointError by remember { mutableStateOf(false) }
            var pairingStatus by remember { mutableStateOf<PairingStatusResult?>(null) }
            var pairingJob by remember { mutableStateOf<Job?>(null) }

            DisposableEffect(Unit) {
                onDispose {
                    pairingJob?.cancel()
                    ServerConnectionUtil.stopScan()
                }
            }

            ButtonComponent(
                onclick = {
                    pairingJob?.cancel()
                    pairingStatus = null
                    ServerConnectionUtil.scan(coroutineScope)
                },
                text = Localization.getString("connection_search_btn")
            )
            Text(
                text = Localization.getString("connection_manual_hint"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(0.8f)) {
                TextInputComponent(
                    labelText = Localization.getString("connection_manual_host"),
                    onChange = { manualHost = it },
                    modifier = Modifier.weight(3f)
                )
                Spacer(Modifier.weight(0.1f))
                TextInputComponent(
                    labelText = Localization.getString("connection_manual_port"),
                    inputValue = manualPort,
                    onChange = { manualPort = it },
                    modifier = Modifier.weight(1f)
                )
            }
            ButtonComponent(
                onclick = {
                    when (val result = manualServerEndpoint(manualHost, manualPort)) {
                        ManualServerEndpointResult.Invalid -> manualEndpointError = true
                        is ManualServerEndpointResult.Valid -> {
                            manualEndpointError = false
                            selectManualServer(result)
                            pairingJob?.cancel()
                            pairingStatus = null
                            pairingJob = coroutineScope.launch {
                                createHttpClient().use { httpClient ->
                                    val pairingResult = PairingFlow(
                                        ServerMetadataClient(httpClient, result.endpoint),
                                        PairingClient(httpClient, result.endpoint)
                                    ).connect(
                                        PairingRequest(
                                            deviceId = pairingIdentity.deviceId(),
                                            surname = State.judgeSurname.trim(),
                                            platform = getPlatformName()
                                        ),
                                        connection
                                    )
                                    pairingStatus = pairingResult.pollStatus(httpClient, result.endpoint)
                                }
                            }
                        }
                    }
                },
                text = Localization.getString("connection_manual_connect_btn")
            )
            if (manualEndpointError) {
                Text(
                    text = Localization.getString("connection_error_manual_endpoint"),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            connectionStatus(pairingStatus)?.let { status ->
                Text(
                    text = Localization.getString(status.statusKey),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                status.reasonKey?.let { reasonKey ->
                    Text(
                        text = Localization.getString(reasonKey),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Colors.SECONDARY.color),
            ) {
                if (availableServers.servers.isEmpty()) {
                    Text(
                        text = Localization.getString("connection_server_not_found"),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableServers.servers, key = { it.server.key }) { discoveredServer ->
                            val service = discoveredServer.server
                            val isAvailable = discoveredServer.status == DiscoveryStatus.Available
                            val status = if (isAvailable) {
                                Localization.getString("connection_court_available")
                            } else {
                                Localization.getString("connection_court_resolving")
                            }
                            ButtonComponent(
                                modifier = Modifier
                                    .background((if (service == selectedServer) Colors.SECONDARY else Colors.PRIMARY).color),
                                text = "${service.name}\n${Localization.getString("connection_court_address")}: ${service.addresses.joinToString()}\n$status",
                                onclick = {
                                    selectServer(service)
                                    pairingJob?.cancel()
                                    pairingStatus = null
                                    pairingJob = coroutineScope.launch {
                                        val address = service.addresses.firstOrNull()
                                        if (address != null) {
                                            createHttpClient().use { httpClient ->
                                                val endpoint = metadataEndpoint(address, service.port)
                                                val pairingResult = PairingFlow(
                                                    ServerMetadataClient(httpClient, endpoint),
                                                    PairingClient(httpClient, endpoint)
                                                ).connect(
                                                    PairingRequest(
                                                        deviceId = pairingIdentity.deviceId(),
                                                        surname = State.judgeSurname.trim(),
                                                        platform = getPlatformName()
                                                    ),
                                                    connection
                                                )
                                                pairingStatus = pairingResult.pollStatus(httpClient, endpoint)
                                            }
                                        }
                                    }
                                },
                                enabled = isAvailable && connection.state is ConnectionState.Discovering
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun PairingResult?.pollStatus(
        httpClient: io.ktor.client.HttpClient,
        endpoint: io.ktor.http.Url
    ): PairingStatusResult? = when (this) {
        is PairingResult.Pending -> PairingStatusFlow(
            PairingStatusPolling(PairingStatusClient(httpClient, endpoint)::fetch)
        ).awaitStatus(requestId, connection)
        else -> null
    }

    @Composable
    private fun connectionStatus(pairingStatus: PairingStatusResult?): ConnectionStatusPresentation? = when (pairingStatus) {
        is PairingStatusResult.Accepted -> ConnectionStatusPresentation("connection_pairing_accepted")
        else -> when (val state = connection.state) {
        is ConnectionState.PairingPending -> ConnectionStatusPresentation("connection_pairing_pending")
        is ConnectionState.Rejected -> ConnectionStatusPresentation(state.failure.localizationKey)
        else -> connectionStatusPresentation(state)
        }
    }

}
