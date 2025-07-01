package org.judging_app.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import org.judging_app.State
import org.judging_app.ui.ButtonComponent

object DisciplineModeScreen: Screen {
    @Composable
    override fun load(controller: NavController) {
        LaunchedEffect(State.isAnimating) {
            if (State.isAnimating.value) {
                delay(400)
                State.isAnimating.value = false
            }
        }

        val goBackOnclick = remember { {
            State.isAnimating.value = true
            val back = controller.popBackStack()
        } }
        ButtonComponent(
            text = "Go back to entry screen",
            onclick = goBackOnclick
        )
    }
}