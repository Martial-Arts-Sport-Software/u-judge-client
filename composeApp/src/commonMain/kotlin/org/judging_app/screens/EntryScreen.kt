package org.judging_app.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import org.judging_app.State
import org.judging_app.ui.ButtonComponent

object EntryScreen: Screen {
    @Composable
    override fun load(controller: NavController) {
        ButtonComponent.render(
            "Go to discipline mode",
            controller,
            State.Routes.DISCIPLINE_MODE
        )
    }
}