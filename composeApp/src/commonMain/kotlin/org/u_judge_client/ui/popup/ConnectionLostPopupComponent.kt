package org.u_judge_client.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.u_judge_client.State
import org.u_judge_client.enums.Colors
import org.u_judge_client.enums.Routes
import org.u_judge_client.locale.Localization
import org.u_judge_client.ui.button.ButtonComponent
import org.u_judge_client.ui.button.clickWithTransition

/**
 * Renders popup on connection lost
 */
@Composable
fun ConnectionLostPopupComponent() {
    Box(
        Modifier
            .fillMaxHeight(0.5f)
            .fillMaxWidth(0.45f)
            .clip(RoundedCornerShape(25.dp))
            .background(Colors.SECONDARY.color)
            .padding(horizontal = 5.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .weight(0.35f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = Localization.getString("connection_lost_title"),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Column(
                Modifier
                    .weight(0.65f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ButtonComponent(
                    text = Localization.getString(
                        "connection_lost_reconnect"
                    ),
                    onclick = {
                    },
                )
                Spacer(Modifier.fillMaxHeight(0.05f))
                ButtonComponent(
                    text = Localization.getString(
                        "connection_lost_change_server"
                    ),
                    onclick = {
                        State.isOffline = true
                        State.currentPopupMode = Popup.Modes.NONE
                        clickWithTransition(Routes.ENTRY)
                    },
                )
                Spacer(Modifier.fillMaxHeight(0.05f))
            }
        }
    }
}