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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.mass.State.availableServers
import org.mass.State.isConnectedToServer
import org.mass.State.selectedServer
import org.mass.enums.Colors
import org.mass.enums.Routes
import org.mass.locale.Localization
import org.mass.ui.button.ButtonComponent
import org.mass.ui.button.ButtonStyles
import org.mass.ui.button.clickWithTransition
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

            ButtonComponent(
                onclick = {
                    coroutineScope.launch {
                        ServerConnectionUtil.scan()
                    }
                },
                text = Localization.getString("connection_search_btn")
            )
            Box(
                Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Colors.SECONDARY.color),
            ) {
                if (availableServers.isEmpty()) {
                    Text(
                        text = Localization.getString("connection_server_not_found"),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Text("Серверов: ${availableServers.size}")
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableServers.values.toList()) { service ->
                            ButtonComponent(
                                modifier = Modifier
                                    .background((if (service == selectedServer) Colors.SECONDARY else Colors.PRIMARY).color),
                                text = "${service.name}:${service.addresses}",
                                onclick = {
                                    selectedServer = service
                                    isConnectedToServer = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

}