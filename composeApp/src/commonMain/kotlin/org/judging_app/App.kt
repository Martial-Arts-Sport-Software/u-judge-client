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
import org.judging_app.enums.Routes
import org.judging_app.screens.CategorySelectScreen
import org.judging_app.screens.DisciplineSelectScreen
import org.judging_app.screens.EntryScreen
import org.judging_app.screens.FreestyleModeScreen
import org.judging_app.screens.HosinsoolModeScreen
import org.judging_app.screens.KerugiModeScreen
import org.judging_app.screens.TanbonModeScreen
import org.judging_app.ui.popup.ConnectionLostPopupComponent

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
                                EntryScreen.load()
                            }
                            animatedComposable(Routes.DISCIPLINE_SELECT) {
                                DisciplineSelectScreen.load()
                            }
                            animatedComposable(Routes.CATEGORY_SELECT) {
                                CategorySelectScreen.load()
                            }
                            animatedComposable(Routes.KERUGI_MODE) {
                                KerugiModeScreen.load()
                            }
                            animatedComposable(Routes.TANBON_MODE) {
                                TanbonModeScreen.load()
                            }
                            animatedComposable(Routes.HOSINSOOL_MODE) {
                                HosinsoolModeScreen.load()
                            }
                            animatedComposable(Routes.FREESTYLE_MODE) {
                                FreestyleModeScreen.load()
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
            }
            content()
        }
    )
}