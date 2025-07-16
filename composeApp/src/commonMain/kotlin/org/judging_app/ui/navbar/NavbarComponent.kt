package org.judging_app.ui.navbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.back_icon
import judging_app_client.composeapp.generated.resources.information_icon
import judging_app_client.composeapp.generated.resources.settings_icon
import judging_app_client.composeapp.generated.resources.switch_icon
import judging_app_client.composeapp.generated.resources.warning_icon
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.ButtonStyles

enum class NavbarStyles {
    VERTICAL,
    HORIZONTAL
}

@Composable
fun NavbarComponent(
    style: NavbarStyles,
    modifier: Modifier = Modifier
) {
    when (style) {
        NavbarStyles.VERTICAL -> {
            Box(
                modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(State.Colors.BUTTON_GRAY.color)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(
                        vertical = 10.dp,
                        horizontal = 5.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ButtonComponent(
                        modifier = Modifier.weight(1f),
                        style = ButtonStyles.Icon,
                        iconSrc = Res.drawable.settings_icon,
                        onclick = {
                            State.currentPopupMode.value =
                                if (State.currentPopupMode.value == State.PopupMode.SETTINGS)
                                    State.PopupMode.NONE
                                else State.PopupMode.SETTINGS
                        },
                        colorFilter = if (
                            State.currentPopupMode.value == State.PopupMode.SETTINGS
                        )
                            ColorFilter.tint(State.Colors.PRIMARY.color)
                        else null
                    )
                    ButtonComponent(
                        modifier = Modifier.weight(1f),
                        style = ButtonStyles.Icon,
                        iconSrc = Res.drawable.information_icon,
                        onclick = {
                            State.currentPopupMode.value =
                                if (State.currentPopupMode.value == State.PopupMode.INFORMATION)
                                    State.PopupMode.NONE
                                else State.PopupMode.INFORMATION
                        },
                        colorFilter = if (
                            State.currentPopupMode.value == State.PopupMode.INFORMATION
                        )
                            ColorFilter.tint(State.Colors.PRIMARY.color)
                        else null
                    )
                    Spacer(Modifier.weight(0.3f))
                    Text(
                        text = "10.0",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    ButtonComponent(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(15.dp))
                            .background(State.Colors.PRIMARY.color),
                        style = ButtonStyles.Icon,
                        iconSrc = Res.drawable.switch_icon,
                        onclick = {
                        },
                        iconPadding = 12.dp
                    )
                }
            }
        }
        NavbarStyles.HORIZONTAL -> {
            Box(
                modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(State.Colors.BUTTON_GRAY.color)
                    .fillMaxSize()
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ButtonComponent(
                        style = ButtonStyles.Icon,
                        iconSrc = Res.drawable.settings_icon,
                        onclick = {
                            State.currentPopupMode.value =
                                if (State.currentPopupMode.value == State.PopupMode.SETTINGS)
                                    State.PopupMode.NONE
                                else State.PopupMode.SETTINGS
                        },
                        colorFilter = if (
                                State.currentPopupMode.value == State.PopupMode.SETTINGS
                            )
                            ColorFilter.tint(State.Colors.PRIMARY.color)
                        else null
                    )
                    Text(
                        text = Localization.getString("kerugi_bout") +
                                " №${44}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    ButtonComponent(
                        style = ButtonStyles.Icon,
                        iconSrc = Res.drawable.warning_icon,
                        onclick = {
                            State.currentPopupMode.value =
                                if (State.currentPopupMode.value == State.PopupMode.WARNING)
                                    State.PopupMode.NONE
                                else State.PopupMode.WARNING
                        },
                        colorFilter = if (
                            State.currentPopupMode.value == State.PopupMode.WARNING
                        )
                            ColorFilter.colorMatrix(ColorMatrix(
                                floatArrayOf(
                                    1f, 0f, 0f, 0f, 0f,
                                    0f, 1f, 0f, 0f, 0f,
                                    0f, 0f, 1f, 0f, 0f,
                                    0f, 0f, 0f, 0.6f, 0f
                                )
                            ))
                        else null
                    )
                }
            }
        }
    }
}