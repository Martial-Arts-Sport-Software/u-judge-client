package org.judging_app.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.navbar.NavbarComponent
import org.judging_app.ui.navbar.NavbarStyles



object KerugiModeScreen : Screen {
    @Composable
    override fun load() {
        Column {
            Row(
                Modifier
                    .fillMaxHeight(0.2f)
                    .weight(0.16f)
            ) {
                NavbarComponent(
                    NavbarStyles.HORIZONTAL,
                    Modifier.weight(0.45f)
                )
                Spacer(Modifier.weight(0.3f))
                Text(
                    modifier = Modifier
                        .weight(0.25f),
                    text = Localization.getString("kerugi_judge") +
                            " - ${State.judgeSurname}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                Modifier
                    .weight(0.42f)
            ) {

            }
            Row(
                Modifier
                    .weight(0.42f)
            ) {

            }
        }
    }
}