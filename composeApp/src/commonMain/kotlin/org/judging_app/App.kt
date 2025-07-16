package org.judging_app

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

import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.app_background
import kotlinx.coroutines.delay
import org.judging_app.screens.CategorySelectScreen
import org.judging_app.screens.DisciplineSelectScreen
import org.judging_app.screens.EntryScreen
import org.judging_app.screens.FreestyleModeScreen
import org.judging_app.screens.HosinsoolModeScreen
import org.judging_app.screens.KerugiModeScreen
import org.judging_app.screens.TanbonModeScreen
import org.judging_app.ui.popup.ConnectionLostPopupComponent

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
            LocalAppLocale provides State.currentLocale
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
                    visible = State.isConnectedToServer.value ||
                            State.isOffline.value,
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
                            startDestination = State.Routes.ENTRY.path,
                            contentAlignment = Alignment.Center,
                        ) {
                            animatedComposable(State.Routes.ENTRY) {
                                EntryScreen.load()
                            }
                            animatedComposable(State.Routes.DISCIPLINE_SELECT) {
                                DisciplineSelectScreen.load()
                            }
                            animatedComposable(State.Routes.CATEGORY_SELECT) {
                                CategorySelectScreen.load()
                            }
                            animatedComposable(State.Routes.KERUGI_MODE) {
                                KerugiModeScreen.load()
                            }
                            animatedComposable(State.Routes.TANBON_MODE) {
                                TanbonModeScreen.load()
                            }
                            animatedComposable(State.Routes.HOSINSOOL_MODE) {
                                HosinsoolModeScreen.load()
                            }
                            animatedComposable(State.Routes.FREESTYLE_MODE) {
                                FreestyleModeScreen.load()
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = !State.isConnectedToServer.value &&
                            !State.isOffline.value,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    BackHandler {
                        if (State.navController!!.currentBackStackEntry?.destination?.route !in arrayOf(
                                State.Routes.KERUGI_MODE.path,
                                State.Routes.TANBON_MODE.path,
                                State.Routes.HOSINSOOL_MODE.path,
                                State.Routes.FREESTYLE_MODE.path,
                                State.Routes.ENTRY.path
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

@OptIn(ExperimentalComposeUiApi::class)
fun NavGraphBuilder.animatedComposable(
    route: State.Routes,
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
                if (State.isAnimating.value) {
                    delay(400)
                    State.isAnimating.value = false
                }
            }
            BackHandler {
                if (State.navController!!.currentBackStackEntry?.destination?.route !in arrayOf(
                        State.Routes.KERUGI_MODE.path,
                        State.Routes.TANBON_MODE.path,
                        State.Routes.HOSINSOOL_MODE.path,
                        State.Routes.FREESTYLE_MODE.path,
                        State.Routes.ENTRY.path
                    )
                ) State.navController!!.popBackStack()
            }
            content()
        }
    )
}