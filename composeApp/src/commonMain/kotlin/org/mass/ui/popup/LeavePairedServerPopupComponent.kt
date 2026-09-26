package org.mass.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import org.mass.ui.button.ButtonStyles
import org.mass.ui.button.clickWithTransition
import u_judge_client.composeapp.generated.resources.Res
import u_judge_client.composeapp.generated.resources.cross_icon

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
            // Taps inside the card stay here instead of reaching the backdrop that dismisses the popup.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(horizontal = 5.dp)
    ) {
        // Equal gaps above the title, between title, text and buttons, and below the buttons.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = Localization.getString("leave_paired_title"),
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = Localization.getString("leave_paired_text"),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ButtonComponent(
                    text = Localization.getString("leave_paired_stay"),
                    onclick = {
                        State.currentPopupMode = Popup.Modes.NONE
                    },
                )
                Spacer(Modifier.height(10.dp))
                ButtonComponent(
                    text = Localization.getString("leave_paired_exit"),
                    onclick = {
                        State.currentPopupMode = Popup.Modes.NONE
                        PairedServerSession.forget()
                        clickWithTransition(Routes.BACK)
                    },
                )
            }
        }
        ButtonComponent(
            style = ButtonStyles.Icon,
            iconSrc = Res.drawable.cross_icon,
            onclick = {
                State.currentPopupMode = Popup.Modes.NONE
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp)
                .height(44.dp),
        )
    }
}
