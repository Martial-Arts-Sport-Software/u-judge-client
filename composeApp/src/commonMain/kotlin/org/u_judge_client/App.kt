package org.u_judge_client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import u_judge_client.composeapp.generated.resources.Res
import u_judge_client.composeapp.generated.resources.app_background
import kotlinx.coroutines.delay
import org.u_judge_client.enums.Routes
import org.u_judge_client.screens.CategorySelectScreen
import org.u_judge_client.screens.DisciplineSelectScreen
import org.u_judge_client.screens.EntryScreen
import org.u_judge_client.screens.FreestyleModeScreen
import org.u_judge_client.screens.HosinsoolModeScreen
import org.u_judge_client.screens.KerugiModeScreen
import org.u_judge_client.screens.TanbonModeScreen
import org.u_judge_client.ui.popup.ConnectionLostPopupComponent
import org.u_judge_client.ui.popup.Popup

/**
 * Main fun, that render the whole application
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    State.navController = rememberNavController()
    State.density = LocalDensity.current
    MaterialTheme(
         typography = getTypography()
    ) {
        CompositionLocalProvider(
            compositionLocalOf { State.currentLocale } provides State.currentLocale
        ) {
            Box(
                Modifier
                    .fillMaxSize(),
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxSize(),
                    painter = painterResource(Res.drawable.app_background),
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
                AnimatedVisibility(
                    visible = State.isConnectedToServer ||
                            State.isOffline,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        Modifier
                            .padding(10.dp)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        NavHost(
                            navController = State.navController!!,
                            startDestination = Routes.ENTRY.path,
                            contentAlignment = Alignment.Center,
                        ) {
                            animatedComposable(Routes.ENTRY) {
                                EntryScreen.Load()
                            }
                            animatedComposable(Routes.DISCIPLINE_SELECT) {
                                DisciplineSelectScreen.Load()
                            }
                            animatedComposable(Routes.CATEGORY_SELECT) {
                                CategorySelectScreen.Load()
                            }
                            animatedComposable(Routes.KERUGI_MODE) {
                                KerugiModeScreen.Load()
                            }
                            animatedComposable(Routes.TANBON_MODE) {
                                TanbonModeScreen.Load()
                            }
                            animatedComposable(Routes.HOSINSOOL_MODE) {
                                HosinsoolModeScreen.Load()
                            }
                            animatedComposable(Routes.FREESTYLE_MODE) {
                                FreestyleModeScreen.Load()
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !State.isConnectedToServer &&
                            !State.isOffline,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    BackHandler {
                        if (State.navController!!.currentBackStackEntry?.destination?.route !in arrayOf(
                                Routes.KERUGI_MODE.path,
                                Routes.TANBON_MODE.path,
                                Routes.HOSINSOOL_MODE.path,
                                Routes.FREESTYLE_MODE.path,
                                Routes.ENTRY.path
                            )
                        ) State.navController!!.popBackStack()
                    }

                    Box(
                        Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ConnectionLostPopupComponent()
                    }
                }
            }
        }
    }
}

/**
 * Renders composable, usually screen instance, by specific route
 * @param route - [Routes] instance, on which content must be rendered
 * @param content - [Composable] fun, usually represents some screen, shown on specific [route]
 */
@OptIn(ExperimentalComposeUiApi::class)
fun NavGraphBuilder.animatedComposable(
    route: Routes,
    content: @Composable () -> Unit
) {
    composable(
        route = route.path,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(300)) },
        content = {
            LaunchedEffect(State.isAnimating) {
                if (State.isAnimating) {
                    delay(400)
                    State.isAnimating = false
                }
            }
            BackHandler {
                if (State.navController!!.currentBackStackEntry?.destination?.route !in arrayOf(
                        Routes.KERUGI_MODE.path,
                        Routes.TANBON_MODE.path,
                        Routes.HOSINSOOL_MODE.path,
                        Routes.FREESTYLE_MODE.path,
                        Routes.ENTRY.path
                    )
                ) State.navController!!.popBackStack()
                else State.currentPopupMode = if (State.currentPopupMode == Popup.Modes.NONE)
                    Popup.Modes.SETTINGS else Popup.Modes.NONE
            }
            content()
        }
    )
}