package org.judging_app.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.judging_app.enums.Colors
import judging_app_client.composeapp.generated.resources.Res

/**
 * Different positions on the screen
 */
enum class CombatButtonPositions {
    LEFT,
    RIGHT,
    CENTER
}

/**
 * Button component especially for combat screens (kerugi & tanbon)
 * @param position - component's position on the screen
 * @param color - background color of the button
 * @param icon - button's icon, instance of [Res.drawable]
 * @param onclick - callback, that is called on button's click
 * @param modifier - [Modifier], that is applied to the component
 */
@Composable
fun CombatButtonComponent(
    position: CombatButtonPositions,
    color: Colors,
    icon: DrawableResource,
    onclick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                if (position == CombatButtonPositions.RIGHT)
                    scaleX = -1f
            },
        border = BorderStroke(3.dp, Colors.BUTTON_BROWN.color),
        shape = when (position) {
            CombatButtonPositions.CENTER -> RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            )
            else -> RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 20.dp,
                bottomStart = 10.dp,
                bottomEnd = 20.dp
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = color.color
        ),
        onClick = { onclick() },
    ) {
        Image(
            modifier = Modifier
                .fillMaxSize(0.9f),
            painter = painterResource(icon),
            contentDescription = null,
            contentScale = ContentScale.Fit
        )
    }
}