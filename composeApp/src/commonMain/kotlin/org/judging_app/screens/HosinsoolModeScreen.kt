package org.judging_app.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import org.judging_app.entities.PresentationCriteria
import org.judging_app.entities.Rating
import org.judging_app.entities.TechniqueCriteria
import org.judging_app.entities.TechniqueRating
import org.judging_app.locale.Localization
import org.judging_app.ui.input.Modes
import org.judging_app.ui.input.RangeInputComponent
import org.judging_app.ui.navbar.NavbarComponent
import org.judging_app.ui.navbar.NavbarStyles
import org.judging_app.ui.popup.SettingsPopupComponent
import org.judging_app.screens.TechniqueScreen.DISPLAY

object HosinsoolModeScreen : TechniqueScreen {
    var currentDisplay by mutableStateOf(DISPLAY.TECHNIQUE)
    var nextDisplay by mutableStateOf(DISPLAY.TECHNIQUE)

    @Composable
    override fun load() {
        val rating by remember { mutableStateOf(
            if (State.currentRating is TechniqueRating) State.currentRating as TechniqueRating
            else TechniqueRating(
                State.currentRating.id,
                techniqueCriteria = if (State.currentCategory == State.Categories.JUNIORS)
                    TechniqueCriteria.Junior(0.1f, 0.1f, 0.1f, 0.1f)
                else TechniqueCriteria.Adult(0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f),
                presentationCriteria = PresentationCriteria.HosinsoolPresentationCriteria(0.1f, 0.1f, 0.1f, 0.1f)
            )
        ) }
        LaunchedEffect(rating) {
            State.currentRating = rating
        }
        LaunchedEffect(State.currentCategory) {
            State.currentRating = Rating("empty")
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
                                style = NavbarStyles.VERTICAL_LEFT,
                                score = rating.techniqueCriteria.getTotalScore() +
                                        rating.presentationCriteria.getTotalScore(),
                                modifier = Modifier.weight(3f),
                            )
                            Spacer(Modifier.weight(1f))
                            AnimatedContent(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(32f)
                                    .clip(RoundedCornerShape(15.dp)),
                                targetState = nextDisplay,
                                transitionSpec = {
                                    if (nextDisplay.ordinal > currentDisplay.ordinal) {
                                        slideInVertically(
                                            animationSpec = tween(500)
                                        ) { height -> height } togetherWith
                                                slideOutVertically(
                                                    animationSpec = tween(500)
                                                ) { height -> -height }
                                    } else {
                                        slideInVertically(
                                            animationSpec = tween(500)
                                        ) { height -> -height } togetherWith
                                                slideOutVertically(
                                                    animationSpec = tween(500)
                                                ) { height -> height }
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
                                                    .fillMaxSize()
                                                    .padding(
                                                        vertical = 10.dp,
                                                        horizontal = 15.dp
                                                    )
                                            ) {
                                                RangeInputComponent(
                                                    modifier = Modifier.align(Alignment.TopCenter),
                                                    currentValue = 0f,
                                                    onValueChange = {},
                                                    mode = Modes.NUMBERS_ONLY,
                                                    icon = Res.drawable.cross_icon
                                                )
                                                Column(
                                                    Modifier
                                                        .padding(top = 25.dp)
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxHeight(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = if (State.currentCategory == State.Categories.ADULTS)
                                                        Arrangement.SpaceBetween else Arrangement.SpaceAround
                                                ) {
                                                    RangeInputComponent(
                                                        currentValue = (rating.techniqueCriteria as TechniqueCriteria.Junior)
                                                            .wristHold,
                                                        onValueChange = { value ->
                                                            (rating.techniqueCriteria as TechniqueCriteria.Junior).wristHold = value
                                                        },
                                                        icon = Res.drawable.wrist
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = (rating.techniqueCriteria as TechniqueCriteria.Junior)
                                                            .clothesHold,
                                                        onValueChange = { value ->
                                                            (rating.techniqueCriteria as TechniqueCriteria.Junior).clothesHold = value
                                                        },
                                                        icon = Res.drawable.clothes
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = (rating.techniqueCriteria as TechniqueCriteria.Junior)
                                                            .fistPunch,
                                                        onValueChange = { value ->
                                                            (rating.techniqueCriteria as TechniqueCriteria.Junior).fistPunch = value
                                                        },
                                                        icon = Res.drawable.fist
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = (rating.techniqueCriteria as TechniqueCriteria.Junior)
                                                            .legKick,
                                                        onValueChange = { value ->
                                                            (rating.techniqueCriteria as TechniqueCriteria.Junior).legKick = value
                                                        },
                                                        icon = Res.drawable.foot
                                                    )
                                                    if (State.currentCategory == State.Categories.ADULTS) {
                                                        RangeInputComponent(
                                                            currentValue = (rating.techniqueCriteria as TechniqueCriteria.Adult)
                                                                .knifeLock,
                                                            onValueChange = { value ->
                                                                (rating.techniqueCriteria as TechniqueCriteria.Adult).knifeLock = value
                                                            },
                                                            icon = Res.drawable.knife
                                                        )
                                                        RangeInputComponent(
                                                            currentValue = (rating.techniqueCriteria as TechniqueCriteria.Adult)
                                                                .weaponLock,
                                                            onValueChange = { value ->
                                                                (rating.techniqueCriteria as TechniqueCriteria.Adult).weaponLock = value
                                                            },
                                                            icon = Res.drawable.belt
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        currentDisplay = DISPLAY.TECHNIQUE
                                    }

                                    DISPLAY.PRESENTATION -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(State.Colors.BUTTON_GRAY.color)
                                        ) {
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .padding(
                                                        vertical = 10.dp,
                                                        horizontal = 15.dp
                                                    )
                                            ) {
                                                RangeInputComponent(
                                                    modifier = Modifier.align(Alignment.TopCenter),
                                                    currentValue = 0f,
                                                    onValueChange = {},
                                                    mode = Modes.NUMBERS_ONLY,
                                                )
                                                Column(
                                                    Modifier
                                                        .padding(top = 20.dp)
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxHeight(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.SpaceAround
                                                ) {
                                                    RangeInputComponent(
                                                        currentValue = (rating.presentationCriteria as PresentationCriteria
                                                            .HosinsoolPresentationCriteria).realism,
                                                        onValueChange = { value ->
                                                            (rating.presentationCriteria as PresentationCriteria
                                                            .HosinsoolPresentationCriteria).realism = value
                                                        },
                                                        mode = Modes.WITH_TEXT,
                                                        text = Localization
                                                            .getString("hosinsool-presentation-criteria-1")
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = (rating.presentationCriteria as PresentationCriteria
                                                        .HosinsoolPresentationCriteria).power,
                                                        onValueChange = { value ->
                                                            (rating.presentationCriteria as PresentationCriteria
                                                            .HosinsoolPresentationCriteria).power = value
                                                        },
                                                        mode = Modes.WITH_TEXT,
                                                        text = Localization
                                                            .getString("hosinsool-presentation-criteria-2")
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = (rating.presentationCriteria as PresentationCriteria
                                                        .HosinsoolPresentationCriteria).balance,
                                                        onValueChange = { value ->
                                                            (rating.presentationCriteria as PresentationCriteria
                                                            .HosinsoolPresentationCriteria).balance = value
                                                        },
                                                        mode = Modes.WITH_TEXT,
                                                        text = Localization
                                                            .getString("hosinsool-presentation-criteria-3")
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = (rating.presentationCriteria as PresentationCriteria
                                                        .HosinsoolPresentationCriteria).harmony,
                                                        onValueChange = { value ->
                                                            (rating.presentationCriteria as PresentationCriteria
                                                            .HosinsoolPresentationCriteria).harmony = value
                                                        },
                                                        mode = Modes.WITH_TEXT,
                                                        text = Localization
                                                            .getString("hosinsool-presentation-criteria-4")
                                                    )
                                                }
                                            }
                                        }
                                        currentDisplay = DISPLAY.PRESENTATION
                                    }
                                    DISPLAY.RESULT -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(State.Colors.BUTTON_GRAY.color)
                                        ) { Text(text = "result")}
                                        currentDisplay = DISPLAY.RESULT
                                    }
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            NavbarComponent(
                                style = NavbarStyles.VERTICAL_RIGHT,
                                modifier = Modifier.weight(2f),
                            )
                        }
                    }
                }
            }
        }
    }
}