package org.judging_app.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.cross_icon
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.ButtonStyles
import org.judging_app.ui.button.clickWithTransition

@Composable
fun SettingsPopupComponent(

) {
    Box(
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.45f)
            .clip(RoundedCornerShape(25.dp))
            .background(State.Colors.SECONDARY.color)
            .padding(5.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .fillMaxHeight(0.2f)
                    .fillMaxWidth()
            ) {
                Text(
                    text = Localization.getString("settings_title"),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
                ButtonComponent(
                    style = ButtonStyles.Icon,
                    iconSrc = Res.drawable.cross_icon,
                    onclick = {
                        State.isShowingSettings.value = false
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            Column(
                Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.fillMaxHeight(0.08f))
                ButtonComponent(
                    text = if (
                        State.currentDiscipline in arrayOf(
                            State.Disciplines.KERUGI,
                            State.Disciplines.TANBON
                        )
                    ) Localization.getString(
                        "settings_start_fight"
                    ) else Localization.getString(
                        "settings_start_performance"
                    ),
                    onclick = {
                        State.isShowingSettings.value = false
                    },
                )
                Spacer(Modifier.fillMaxHeight(0.05f))
                ButtonComponent(
                    text = Localization.getString(
                        "settings_choose_category"
                    ),
                    onclick = {
                        State.isShowingSettings.value = false
                        clickWithTransition(State.Routes.BACK)
                    },
                )
                Spacer(Modifier.fillMaxHeight(0.05f))
                ButtonComponent(
                    text = Localization.getString(
                        "settings_choose_discipline"
                    ),
                    onclick = {
                        State.isShowingSettings.value = false
                        clickWithTransition(State.Routes.BACK)
                        clickWithTransition(State.Routes.BACK)
                    },
                )
                Row(
                    Modifier
                        .fillMaxWidth(0.6f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ButtonComponent(
                        "Русский",
                        ButtonStyles.Plain,
                        onclick = { State.currentLocale.value = "ru" },
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
                        onclick = { State.currentLocale.value = "en" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}