package org.judging_app.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.kerugi_chestplate
import judging_app_client.composeapp.generated.resources.kerugi_helmet
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.button.KerugiButtonComponent
import org.judging_app.ui.button.KerugiButtonPositions
import org.judging_app.ui.input.RangeInputComponent
import org.judging_app.ui.navbar.NavbarComponent
import org.judging_app.ui.navbar.NavbarStyles
import org.judging_app.ui.popup.SettingsPopupComponent
import org.judging_app.ui.popup.WarningPopupComponent
import kotlin.math.roundToInt

object HosinsoolModeScreen : Screen {
    @Composable
    override fun load() {
        Column(Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.9f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Localization.getString(
                        "discipline_hosinsool"
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = Localization.getString(
                        "hosinsool_technique"
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (State.currentCategory?.value
                        == State.Categories.ADULTS.value)
                        Localization.getString("category_adults")
                    else Localization.getString("category_juniors"),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = Localization.getString("kerugi_judge") +
                            " - ${State.judgeSurname.ifEmpty {
                                Localization.getString("kerugi_judge_empty")
                            }}",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.height(20.dp))
            AnimatedContent(
                targetState = State.currentPopupMode.value,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                }
            ) { target ->
                when (target) {
                    State.PopupMode.SETTINGS -> {
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
                    else -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NavbarComponent(
                                style = NavbarStyles.VERTICAL,
                                modifier = Modifier.weight(0.2f)
                            )
                            Spacer(Modifier.weight(0.07f))
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(2f)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(State.Colors.BUTTON_GRAY.color)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(15.dp)
                                ) {
                                    RangeInputComponent(
                                        showNumbers = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}