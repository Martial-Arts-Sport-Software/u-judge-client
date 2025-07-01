package org.judging_app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.app_background
import org.judging_app.screens.DisciplineModeScreen
import org.judging_app.screens.EntryScreen

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    State.screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()
    State.screenHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    State.density = LocalDensity.current

    BackHandler(enabled = false) {}

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
                Box(
                    Modifier
                        .padding(10.dp)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = State.Routes.ENTRY.path,
                        contentAlignment = Alignment.Center,
                    ) {
                        animatedComposable(State.Routes.ENTRY.path) {
                            EntryScreen.load(navController)
                        }
                        animatedComposable(State.Routes.DISCIPLINE_MODE.path) {
                            DisciplineModeScreen.load(navController)
                        }
                    }
                }
            }
        }
    }
}

fun NavGraphBuilder.animatedComposable(
    route: String,
    content: @Composable () -> Unit
) {
    composable(
        route = route,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(300)) },
        content = { content() }
    )
}