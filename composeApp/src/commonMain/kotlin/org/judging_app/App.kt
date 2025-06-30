package org.judging_app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.app_background
import org.judging_app.screens.DisciplineModeScreen
import org.judging_app.screens.EntryScreen

@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    MaterialTheme(

    ) {
        Box(
            Modifier
                .fillMaxSize()
        ) {
            Image(
                modifier = Modifier
                    .fillMaxSize(),
                painter = painterResource(Res.drawable.app_background),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier.padding(10.dp)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = State.Routes.ENTRY.path
                ) {
                    composable(State.Routes.ENTRY.path) {
                        EntryScreen.load(navController)
                    }
                    composable(State.Routes.DISCIPLINE_MODE.path) {
                        DisciplineModeScreen.load(navController)
                    }
                }
            }
        }
    }
}