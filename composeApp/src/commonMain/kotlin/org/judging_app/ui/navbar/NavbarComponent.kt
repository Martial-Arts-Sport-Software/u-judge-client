package org.judging_app.ui.navbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.settings_icon
import judging_app_client.composeapp.generated.resources.warning_icon
import org.judging_app.State
import org.judging_app.locale.Localization
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.ButtonStyles

enum class NavbarStyles {
    VERTICAL,
    HORIZONTAL
}

@Composable
fun NavbarComponent(
    style: NavbarStyles,
    modifier: Modifier = Modifier
) {
    when (style) {
        NavbarStyles.VERTICAL -> {
            Box(
                modifier
                    .background(State.Colors.BUTTON_GRAY.color)
            ) {
                Column {

                }
            }
        }
        NavbarStyles.HORIZONTAL -> {
            Box(
                modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(State.Colors.BUTTON_GRAY.color)
                    .fillMaxSize()
            ) {
                Row(
                    Modifier
                        .padding(6.dp)
                        .fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ButtonComponent(
                        style = ButtonStyles.Icon,
                        iconSrc = Res.drawable.settings_icon,
                        onclick = {},
                    )
                    Text(
                        text = Localization.getString("kerugi_bout") +
                                " №${44}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    ButtonComponent(
                        style = ButtonStyles.Icon,
                        iconSrc = Res.drawable.warning_icon,
                        onclick = {},
                    )
                }
            }
        }
    }
}