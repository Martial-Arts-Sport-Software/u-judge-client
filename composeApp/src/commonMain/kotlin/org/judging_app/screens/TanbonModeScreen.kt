package org.judging_app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.kerugi_chestplate
import judging_app_client.composeapp.generated.resources.kerugi_helmet
import judging_app_client.composeapp.generated.resources.tanbon_body
import judging_app_client.composeapp.generated.resources.tanbon_cross
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.button.KerugiButtonComponent
import org.judging_app.ui.button.KerugiButtonPositions
import org.judging_app.ui.navbar.NavbarComponent
import org.judging_app.ui.navbar.NavbarStyles

object TanbonModeScreen : Screen {
    @Composable
    override fun load() {
        Column(
            Modifier
                .fillMaxHeight(0.9f)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                Modifier
                    .fillMaxHeight(0.2f)
                    .weight(0.16f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavbarComponent(
                    NavbarStyles.HORIZONTAL,
                    Modifier.weight(0.45f)
                )
                Spacer(Modifier.weight(0.25f))
                Text(
                    modifier = Modifier
                        .weight(0.3f)
                        .padding(horizontal = 3.dp),
                    text = Localization.getString("kerugi_judge") +
                            " - ${
                                State.judgeSurname.ifEmpty {
                                Localization.getString("kerugi_judge_empty")
                            }
                            }",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Right
                )
            }
            Spacer(Modifier.weight(0.06f))
            Row(
                Modifier
                    .weight(0.38f)
            ) {
                KerugiButtonComponent(
                    position = KerugiButtonPositions.LEFT,
                    color = State.Colors.BUTTON_BLUE,
                    icon = Res.drawable.kerugi_helmet,
                    onclick = {},
                    modifier = Modifier
                        .weight(1f)
                )
                Spacer(Modifier.weight(0.02f))
                KerugiButtonComponent(
                    position = KerugiButtonPositions.RIGHT,
                    color = State.Colors.BUTTON_RED,
                    icon = Res.drawable.kerugi_helmet,
                    onclick = {},
                    modifier = Modifier
                        .weight(1f)
                )
            }
            Spacer(Modifier.weight(0.02f))
            Row(
                Modifier
                    .weight(0.38f)
            ) {
                KerugiButtonComponent(
                    position = KerugiButtonPositions.LEFT,
                    color = State.Colors.BUTTON_BLUE,
                    icon = Res.drawable.tanbon_body,
                    onclick = {},
                    modifier = Modifier
                        .weight(1f)
                )
                Spacer(Modifier.weight(0.04f))
                KerugiButtonComponent(
                    position = KerugiButtonPositions.CENTER,
                    color = State.Colors.BUTTON_GRAY,
                    icon = Res.drawable.tanbon_cross,
                    onclick = {},
                    modifier = Modifier
                        .weight(1f)
                )
                Spacer(Modifier.weight(0.04f))
                KerugiButtonComponent(
                    position = KerugiButtonPositions.RIGHT,
                    color = State.Colors.BUTTON_RED,
                    icon = Res.drawable.tanbon_body,
                    onclick = {},
                    modifier = Modifier
                        .weight(1f)
                )
            }
        }
    }
}