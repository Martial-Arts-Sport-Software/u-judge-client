package org.judging_app.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.cross_icon
import org.judging_app.State
import org.judging_app.entities.PresentationCriteria
import org.judging_app.entities.Rating
import org.judging_app.entities.TechniqueCriteria
import org.judging_app.entities.TechniqueRating
import org.judging_app.enums.Categories
import org.judging_app.enums.Colors
import org.judging_app.locale.Localization
import org.judging_app.readFile
import org.judging_app.ui.input.Modes
import org.judging_app.ui.input.RangeInputComponent
import org.judging_app.ui.navbar.NavbarComponent
import org.judging_app.ui.navbar.NavbarStyles
import org.judging_app.ui.popup.SettingsPopupComponent
import org.judging_app.screens.TechniqueScreen.DISPLAY
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.ButtonStyles
import org.judging_app.ui.popup.InformationPopupComponent
import org.judging_app.ui.popup.Popup

/**
 * Screen of hosinsool discipline
 * @property currentDisplay - display that is currently shown
 * @property nextDisplay - display, that will be switched to
 */
object HosinsoolModeScreen : TechniqueScreen {
    override var currentDisplay by mutableStateOf(DISPLAY.TECHNIQUE)
    override var nextDisplay by mutableStateOf(DISPLAY.TECHNIQUE)
    override var showInformation by mutableStateOf(false)

    @Composable
    override fun load() {
        val filename = "${State.currentLocale}.txt"
        val infoTextLines = readFile(filename)
        val rating by remember { mutableStateOf(
            TechniqueRating(
                State.currentRating.id,
                techniqueCriteria = if (State.currentCategory == Categories.JUNIORS)
                    TechniqueCriteria.Junior(0.1f, 0.1f, 0.1f, 0.1f)
                else TechniqueCriteria.Adult(0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f),
                presentationCriteria = PresentationCriteria.Hosinsool(0.1f, 0.1f, 0.1f, 0.1f)
            )
        ) }
        State.currentRating = rating
        LaunchedEffect(rating.totalScore) {
            State.currentRating = rating
        }
        LaunchedEffect(State.currentCategory) {
            State.currentRating = Rating("offline")
            nextDisplay = DISPLAY.TECHNIQUE
        }
        Column(Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.9f)
        ) {
            TechniqueScreenHeader()
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
                                .fillMaxHeight()
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SettingsPopupComponent()
                        }
                    }
                    Popup.Modes.INFORMATION -> {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NavbarComponent(
                                style = NavbarStyles.VERTICAL_LEFT,
                                rating = rating,
                                modifier = Modifier.weight(3f),
                            )
                            Spacer(Modifier.weight(1f))
                            InformationPopupComponent(
                                lines = infoTextLines,
                                modifier = Modifier.weight(35f)
                            )
                        }
                    }
                    else -> {
                        Row(
                            Modifier.fillMaxWidth().fillMaxHeight(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NavbarComponent(
                                style = NavbarStyles.VERTICAL_LEFT,
                                rating = rating,
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
                                                .background(Colors.BUTTON_GRAY.color)
                                        ) {
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .padding(
                                                        vertical = if (State.currentCategory == Categories.ADULTS)
                                                            10.dp else 20.dp,
                                                        horizontal = 15.dp
                                                    )
                                            ) {
                                                RangeInputComponent(
                                                    modifier = Modifier.align(Alignment.TopCenter),
                                                    currentValue = 0f,
                                                    onValueChange = {},
                                                    showSlider = false,
                                                    icon = Res.drawable.cross_icon
                                                )
                                                Column(
                                                    Modifier
                                                        .padding(top = if (State.currentCategory == Categories.ADULTS)
                                                            30.dp else 40.dp)
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxHeight(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    DefaultTechniqueDisplay(rating)
                                                }
                                            }
                                        }
                                        currentDisplay = DISPLAY.TECHNIQUE
                                    }

                                    DISPLAY.PRESENTATION -> {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(Colors.BUTTON_GRAY.color)
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
                                                    showSlider = false,
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
                                                            .Hosinsool).realism,
                                                        onValueChange = { value ->
                                                            (rating.presentationCriteria as PresentationCriteria
                                                            .Hosinsool).realism = value
                                                        },
                                                        mode = Modes.TEXT_ABOVE,
                                                        text = Localization
                                                            .getString("hosinsool-presentation-criteria-1")
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = (rating.presentationCriteria as PresentationCriteria
                                                        .Hosinsool).power,
                                                        onValueChange = { value ->
                                                            (rating.presentationCriteria as PresentationCriteria
                                                            .Hosinsool).power = value
                                                        },
                                                        mode = Modes.TEXT_ABOVE,
                                                        text = Localization
                                                            .getString("hosinsool-presentation-criteria-2")
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = (rating.presentationCriteria as PresentationCriteria
                                                        .Hosinsool).balance,
                                                        onValueChange = { value ->
                                                            (rating.presentationCriteria as PresentationCriteria
                                                            .Hosinsool).balance = value
                                                        },
                                                        mode = Modes.TEXT_ABOVE,
                                                        text = Localization
                                                            .getString("hosinsool-presentation-criteria-3")
                                                    )
                                                    RangeInputComponent(
                                                        currentValue = (rating.presentationCriteria as PresentationCriteria
                                                        .Hosinsool).harmony,
                                                        onValueChange = { value ->
                                                            (rating.presentationCriteria as PresentationCriteria
                                                            .Hosinsool).harmony = value
                                                        },
                                                        mode = Modes.TEXT_ABOVE,
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
                                                .background(Colors.BUTTON_GRAY.color)
                                        ) {
                                            Column(
                                                Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp)
                                            ) {
                                                Row(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .weight(10f)
                                                ) {
                                                    Box(
                                                        Modifier
                                                            .fillMaxSize()
                                                            .weight(1.2f)
                                                            .clip(RoundedCornerShape(10))
                                                            .background(Colors.SLIDER_TRACK_ACTIVE.color),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${Localization.getString("hosinsool-result-sum")}: " +
                                                                    "${rating.totalScore}",
                                                            style = MaterialTheme.typography.titleLarge
                                                        )
                                                    }
                                                    Spacer(Modifier.width(15.dp))
                                                    Column(
                                                        Modifier.fillMaxSize().weight(1f)
                                                    ) {
                                                        Box(
                                                            Modifier
                                                                .fillMaxSize()
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(15))
                                                                .background(Colors.PRIMARY.color.copy(0.85f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "${Localization.getString("hosinsool-result-technique")}: " +
                                                                        "${rating.techniqueCriteria.getTotalScore()}",
                                                                style = MaterialTheme.typography.titleMedium
                                                            )
                                                        }
                                                        Spacer(Modifier.height(15.dp))
                                                        Box(
                                                            Modifier
                                                                .fillMaxSize()
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(15))
                                                                .background(Colors.PRIMARY.color.copy(0.85f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "${Localization.getString("hosinsool-result-presentation")}: " +
                                                                        "${rating.presentationCriteria.getTotalScore()}",
                                                                style = MaterialTheme.typography.titleMedium
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(15.dp))
                                                Row(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .weight(2f)
                                                ) {
                                                    ButtonComponent(
                                                        modifier = Modifier.weight(1f),
                                                        text = Localization.getString("hosinsool-result-save-btn"),
                                                        onclick = {}
                                                    )
                                                    Spacer(Modifier.width(15.dp))
                                                    ButtonComponent(
                                                        modifier = Modifier.weight(1f),
                                                        style = ButtonStyles.Secondary,
                                                        text = Localization.getString("hosinsool-result-send-btn"),
                                                        onclick = {},
                                                        enabled = !State.isOffline
                                                    )
                                                }
                                            }
                                        }
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