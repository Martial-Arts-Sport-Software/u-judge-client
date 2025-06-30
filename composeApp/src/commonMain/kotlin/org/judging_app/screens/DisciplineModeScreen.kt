package org.judging_app.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import org.judging_app.State
import org.judging_app.ui.ButtonComponent

object DisciplineModeScreen: Screen {
    @Composable
    override fun load(controller: NavController) {
        ButtonComponent.render(
            "Go back to entry screen",
            controller,
            State.Routes.ENTRY
        )
    }
}