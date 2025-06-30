package org.judging_app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.judging_app.State

object ButtonComponent {
    @Composable
    fun render(text: String, controller: NavController? = null, clickRoute: State.Routes? = null) {
        Button(
            modifier = Modifier.padding(5.dp),
            onClick = { onclick(controller, clickRoute) },
            content = { Text(text) }
        )
    }

    private fun onclick(controller: NavController? = null, route: State.Routes? = null) {
        if (route != null) controller?.navigate(route.path)
        else controller?.navigate(State.Routes.ENTRY)
        State.currentLocale.value = "en"
    }
}