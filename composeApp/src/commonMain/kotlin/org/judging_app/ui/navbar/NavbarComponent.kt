package org.judging_app.ui.navbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.cross_icon
import judging_app_client.composeapp.generated.resources.information_icon
import judging_app_client.composeapp.generated.resources.settings_icon
import judging_app_client.composeapp.generated.resources.switch_icon
import judging_app_client.composeapp.generated.resources.warning_icon
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.screens.HosinsoolModeScreen
import org.judging_app.screens.TechniqueScreen
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.ButtonStyles

enum class NavbarStyles {
    VERTICAL_LEFT,
    VERTICAL_RIGHT,
    HORIZONTAL
}

@Composable
fun NavbarComponent(
    style: NavbarStyles,
    modifier: Modifier = Modifier,
) {
    when (style) {
        NavbarStyles.VERTICAL_LEFT -> {
            Box(
                modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(State.Colors.BUTTON_GRAY.color)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(
                            top = 10.dp,
                            bottom = 15.dp,
                            start = 5.dp,
                            end = 5.dp
                        )
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
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
                    ButtonComponent(
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
                    Spacer(Modifier.fillMaxHeight(0.2f))
                    Text(
                        text = "10.0",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        NavbarStyles.VERTICAL_RIGHT -> {
            Box(
                modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(State.Colors.BUTTON_GRAY.color)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column (
                    modifier = Modifier
                        .fillMaxHeight(0.9f)
                        .fillMaxWidth(0.5f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    ButtonComponent(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (HosinsoolModeScreen.currentDisplay == TechniqueScreen.DISPLAY.TECHNIQUE)
                                    State.Colors.SLIDER_TRACK_ACTIVE.color
                                else Color.White.copy(0.75f)
                            ),
                        style = ButtonStyles.Solid,
                        onclick = {
                            HosinsoolModeScreen.nextDisplay = TechniqueScreen.DISPLAY.TECHNIQUE
                        },
                    )
                    Spacer(Modifier.weight(0.3f))
                    ButtonComponent(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (HosinsoolModeScreen.currentDisplay == TechniqueScreen.DISPLAY.PRESENTATION)
                                    State.Colors.SLIDER_TRACK_ACTIVE.color
                                else Color.White.copy(0.75f)
                            ),
                        style = ButtonStyles.Solid,
                        onclick = {
                            HosinsoolModeScreen.nextDisplay = TechniqueScreen.DISPLAY.PRESENTATION
                        },
                    )
                    Spacer(Modifier.weight(0.3f))
                    ButtonComponent(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (HosinsoolModeScreen.currentDisplay == TechniqueScreen.DISPLAY.RESULT)
                                    State.Colors.SLIDER_TRACK_ACTIVE.color
                                else Color.White.copy(0.75f)
                            ),
                        style = ButtonStyles.Solid,
                        onclick = {
                            HosinsoolModeScreen.nextDisplay = TechniqueScreen.DISPLAY.RESULT
                        },
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