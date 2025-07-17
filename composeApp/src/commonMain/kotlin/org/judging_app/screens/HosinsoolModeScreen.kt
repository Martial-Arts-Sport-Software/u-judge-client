package org.judging_app.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import judging_app_client.composeapp.generated.resources.belt
import judging_app_client.composeapp.generated.resources.clothes
import judging_app_client.composeapp.generated.resources.cross_icon
import judging_app_client.composeapp.generated.resources.fist
import judging_app_client.composeapp.generated.resources.foot
import judging_app_client.composeapp.generated.resources.knife
import judging_app_client.composeapp.generated.resources.wrist
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.input.Modes
import org.judging_app.ui.input.RangeInputComponent
import org.judging_app.ui.navbar.NavbarComponent
import org.judging_app.ui.navbar.NavbarStyles
import org.judging_app.ui.popup.SettingsPopupComponent
import org.judging_app.screens.TechniqueScreen.DISPLAY

object HosinsoolModeScreen : TechniqueScreen {
    @Composable
    override fun load() {
        var currentDisplay by remember { mutableStateOf(DISPLAY.TECHNIQUE) }
        var direction by remember { mutableStateOf(1) }
        var btnDirection by remember { mutableStateOf(1) }
        fun switchDisplay() {
            when(currentDisplay) {
                DISPLAY.TECHNIQUE -> {
                    direction = 1
                    currentDisplay = DISPLAY.PRESENTATION
                }
                DISPLAY.PRESENTATION -> {
                    if (direction == 1) {
                        currentDisplay = DISPLAY.RESULT
                        btnDirection = -1
                    }
                    else {
                        currentDisplay = DISPLAY.TECHNIQUE
                        btnDirection = 1
                    }
                }
                DISPLAY.RESULT -> {
                    direction = -1
                    currentDisplay = DISPLAY.PRESENTATION
                }
            }
        }

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
                                .fillMaxHeight()
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SettingsPopupComponent()
                        }
                    }
                    else -> {
                        Row(
                            Modifier.fillMaxWidth().fillMaxHeight(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NavbarComponent(
                                style = NavbarStyles.VERTICAL,
                                modifier = Modifier.weight(3f),
                                switchDisplay = { switchDisplay() },
                                direction = btnDirection
                            )
                            Spacer(Modifier.weight(1f))
                            AnimatedContent(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(36f)
                                    .clip(RoundedCornerShape(15.dp)),
                                targetState = currentDisplay,
                                transitionSpec = {
                                    if (direction > 0) {
                                        slideInHorizontally { width -> width } togetherWith
                                                slideOutHorizontally { width -> -width }
                                    } else {
                                        slideInHorizontally { width -> -width } togetherWith
                                                slideOutHorizontally { width -> width }
                                    }
                                }
                            ) { target ->
                                when(target) {
                                    DISPLAY.TECHNIQUE -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(State.Colors.BUTTON_GRAY.color)
                                        ) {
                                            Box(
                                                Modifier
                                                    .padding(
                                                        start = 15.dp,
                                                        top = 10.dp,
                                                        end = 15.dp,
                                                        bottom = 15.dp
                                                    )
                                            ) {
                                                RangeInputComponent(
                                                    currentValue = 0f,
                                                    onValueChange = {},
                                                    mode = Modes.NUMBERS_ONLY,
                                                    icon = Res.drawable.cross_icon
                                                )
                                                Column(
                                                    Modifier
                                                        .padding(top = 25.dp)
                                                        .fillMaxHeight(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    RangeInputComponent(
                                                        currentValue = 0f,
                                                        onValueChange = {},
                                                        icon = Res.drawable.wrist
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = 0f,
                                                        onValueChange = {},
                                                        icon = Res.drawable.clothes
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = 0f,
                                                        onValueChange = {},
                                                        icon = Res.drawable.fist
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = 0f,
                                                        onValueChange = {},
                                                        icon = Res.drawable.foot
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = 0f,
                                                        onValueChange = {},
                                                        icon = Res.drawable.knife
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = 0f,
                                                        onValueChange = {},
                                                        icon = Res.drawable.belt
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    DISPLAY.PRESENTATION -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(State.Colors.BUTTON_GRAY.color)
                                        ) { Text(text = "pere")}
                                    }
                                    DISPLAY.RESULT -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(State.Colors.BUTTON_GRAY.color)
                                        ) { Text(text = "result")}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}