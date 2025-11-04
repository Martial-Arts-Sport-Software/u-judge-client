package org.u_judge_client.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import u_judge_client.composeapp.generated.resources.Res
import u_judge_client.composeapp.generated.resources.kerugi_chestplate
import u_judge_client.composeapp.generated.resources.kerugi_helmet
import org.u_judge_client.State
import org.u_judge_client.enums.Colors
import org.u_judge_client.locale.Localization
import org.u_judge_client.ui.button.CombatButtonComponent
import org.u_judge_client.ui.button.CombatButtonPositions
import org.u_judge_client.ui.navbar.NavbarComponent
import org.u_judge_client.ui.navbar.NavbarStyles
import org.u_judge_client.ui.popup.Popup
import org.u_judge_client.ui.popup.SettingsPopupComponent
import org.u_judge_client.ui.popup.WarningPopupComponent

/**
 * Screen of kerugi discipline
 */
object KerugiModeScreen : Screen {
    @Composable
    override fun Load() {
        Column(
            Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                Modifier.fillMaxHeight(0.15f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavbarComponent(
                    NavbarStyles.HORIZONTAL,
                    Modifier.weight(0.45f),
                )
                Spacer(Modifier.weight(0.25f))
                Text(
                    modifier = Modifier
                        .weight(0.3f)
                        .padding(horizontal = 3.dp),
                    text = Localization.getString("kerugi_judge") +
                            " - ${State.judgeSurname.ifEmpty {
                                Localization.getString("kerugi_judge_empty")
                            }
                            }",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Right
                )
            }
            Spacer(Modifier.height(15.dp))
            AnimatedContent(
                targetState = State.currentPopupMode,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                }
            ) { target ->
                when (target) {
                    Popup.Modes.SETTINGS -> {
                        Column(
                            Modifier
                                .fillMaxHeight(0.95f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SettingsPopupComponent()
                        }
                    }
                    Popup.Modes.WARNING -> {
                        Column(
                            Modifier
                                .fillMaxHeight(0.95f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            WarningPopupComponent()
                        }
                    }
                    else -> {
                        Column(
                            Modifier
                                .fillMaxHeight(0.95f)
                                .fillMaxWidth()
                        ) {
                            Row(
                                Modifier
                                    .weight(0.38f)
                            ) {
                                CombatButtonComponent(
                                    position = CombatButtonPositions.LEFT,
                                    color = Colors.BUTTON_BLUE,
                                    icon = Res.drawable.kerugi_helmet,
                                    onclick = {},
                                    modifier = Modifier
                                        .weight(1f)
                                )
                                Spacer(Modifier.weight(0.02f))
                                CombatButtonComponent(
                                    position = CombatButtonPositions.RIGHT,
                                    color = Colors.BUTTON_RED,
                                    icon = Res.drawable.kerugi_helmet,
                                    onclick = {},
                                    modifier = Modifier
                                        .weight(1f)
                                )
                            }
                            Spacer(Modifier.weight(0.02f))
                            Row(
                                Modifier
                                    .weight(0.38f)
                            ) {
                                CombatButtonComponent(
                                    position = CombatButtonPositions.LEFT,
                                    color = Colors.BUTTON_BLUE,
                                    icon = Res.drawable.kerugi_chestplate,
                                    onclick = {},
                                    modifier = Modifier
                                        .weight(1f)
                                )
                                Spacer(Modifier.weight(0.02f))
                                CombatButtonComponent(
                                    position = CombatButtonPositions.RIGHT,
                                    color = Colors.BUTTON_RED,
                                    icon = Res.drawable.kerugi_chestplate,
                                    onclick = {},
                                    modifier = Modifier
                                        .weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}