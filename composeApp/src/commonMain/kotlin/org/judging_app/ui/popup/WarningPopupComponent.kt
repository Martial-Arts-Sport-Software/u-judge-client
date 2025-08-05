package org.judging_app.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.judging_app.State
import org.judging_app.enums.Colors
import org.judging_app.locale.Localization
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.ButtonStyles

/**
 * Renders warning popup (for kerugi & tanbon)
 */
@Composable
fun WarningPopupComponent() {
    Box(
        Modifier
            .fillMaxHeight(0.8f)
            .fillMaxWidth(0.45f)
            .clip(RoundedCornerShape(25.dp))
            .background(Colors.SECONDARY.color)
            .padding(horizontal = 5.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxHeight()
        ) {
            Text(
                text = Localization.getString("warning_title"),
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            )
            Column(
                Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.weight(0.5f))
                ButtonComponent(
                    modifier = Modifier.weight(2f),
                    text = Localization.getString(
                        "warning_continue"
                    ),
                    onclick = {
                        State.currentPopupMode = Popup.Modes.NONE
                    },
                )
                Spacer(Modifier.weight(0.5f))
                ButtonComponent(
                    modifier = Modifier.weight(2f),
                    text = Localization.getString(
                        "settings_start_fight"
                    ),
                    onclick = {
                        State.currentPopupMode = Popup.Modes.NONE
                    },
                )
                Spacer(Modifier.weight(0.5f))
                Row(
                    Modifier
                        .weight(2f)
                        .fillMaxWidth(0.6f)
                        .padding(bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ButtonComponent(
                        "Русский",
                        ButtonStyles.Plain,
                        onclick = { State.currentLocale = "ru" },
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        color = Color(0xFF7C45E2),
                        thickness = 1.5.dp,
                        modifier = Modifier
                            .fillMaxHeight(0.4f)
                            .clip(RoundedCornerShape(5.dp))
                    )
                    ButtonComponent(
                        "English",
                        ButtonStyles.Plain,
                        onclick = { State.currentLocale = "en" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}