package org.mass.ui.popup

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
import org.mass.PairedServerSession
import org.mass.State
import org.mass.enums.Colors
import org.mass.enums.Routes
import org.mass.locale.Localization
import org.mass.ui.button.ButtonComponent
import org.mass.ui.button.clickWithTransition

/**
 * Renders popup when a paired judge leaves the connection screen: leaving disconnects and forgets the server
 */
@Composable
fun LeavePairedServerPopupComponent() {
    Box(
        Modifier
            .fillMaxHeight(0.7f)
            .fillMaxWidth(0.55f)
            .clip(RoundedCornerShape(25.dp))
            .background(Colors.SECONDARY.color)
            .padding(horizontal = 5.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .weight(0.3f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = Localization.getString("leave_paired_title"),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Box(
                Modifier
                    .weight(0.3f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Localization.getString("leave_paired_text"),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                Modifier
                    .weight(0.4f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ButtonComponent(
                    text = Localization.getString("leave_paired_stay"),
                    onclick = {
                        State.currentPopupMode = Popup.Modes.NONE
                    },
                )
                Spacer(Modifier.fillMaxHeight(0.05f))
                ButtonComponent(
                    text = Localization.getString("leave_paired_exit"),
                    onclick = {
                        State.currentPopupMode = Popup.Modes.NONE
                        PairedServerSession.forget()
                        clickWithTransition(Routes.BACK)
                    },
                )
                Spacer(Modifier.fillMaxHeight(0.05f))
            }
        }
    }
}
