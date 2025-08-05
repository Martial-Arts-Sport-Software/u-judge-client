package org.judging_app.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.judging_app.State
import org.judging_app.enums.Colors
import org.judging_app.enums.Routes

/**
 * buttons variants
 * @property Primary - primary button with text, first accent
 * @property Secondary - secondary button with text, second accent
 * @property Plain - button with transparent background with text, third accent
 * @property Icon - button with icon without text
 * @property Solid - button without anything but solid background
 */
enum class ButtonStyles {
    Primary,
    Secondary,
    Plain,
    Icon,
    Solid
}

/**
 * Renders button component
 * @param text - button's content. Does not required for [ButtonStyles.Icon] & [ButtonStyles.Solid]
 * @param style - [ButtonStyles] style of button
 * @param modifier - custom [Modifier] applied to component
 * @param onclick - callback that is called on button click
 * @param iconSrc - [DrawableResource] source to image that is provided as icon to [ButtonStyles.Icon]
 * @param colorFilter - [ColorFilter] instance, applied to button's icon ([ButtonStyles.Icon] only)
 * @param iconPadding - inner padding for icon inside button ([ButtonStyles.Icon] only)
 * @param enabled - button's accessibility
 */
@Composable
fun ButtonComponent(
    text: String? = null,
    style: ButtonStyles = ButtonStyles.Primary,
    modifier: Modifier = Modifier,
    onclick: () -> Unit,
    iconSrc: DrawableResource? = null,
    colorFilter: ColorFilter? = null,
    iconPadding: Dp = 10.dp,
    enabled: Boolean = true
) {
    when(style) {
        ButtonStyles.Icon -> require(iconSrc != null)
        ButtonStyles.Primary,
             ButtonStyles.Secondary,
                 ButtonStyles.Plain -> require(text != null)
        else -> {}
    }
    when(style) {
        ButtonStyles.Primary -> {
            Button(
                modifier = modifier
                    .fillMaxWidth(0.8f),
                onClick = {
                    if (!State.isAnimating) onclick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Colors.PRIMARY.color
                ),
                shape = RoundedCornerShape(5.dp),
                content = {
                    Text(
                        style = MaterialTheme.typography.bodyLarge,
                        text = text!!,
                        textAlign = TextAlign.Center
                    )
                },
                enabled = enabled
            )
        }
        ButtonStyles.Secondary -> {
            Button(
                modifier = modifier.fillMaxWidth(0.8f),
                onClick = {
                    if (!State.isAnimating) {
                        onclick()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(0.48f),
                    disabledContainerColor = Color.White.copy(0.35f)
                ),
                shape = RoundedCornerShape(5.dp),
                content = {
                    Text(
                        text!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled) Colors.PRIMARY.color
                        else Color.Black.copy(0.3f)
                    )
                },
                enabled = enabled
            )
        }
        ButtonStyles.Plain -> {
            Button(
                modifier = modifier
                    .fillMaxWidth(0.8f),
                onClick = {
                    if (!State.isAnimating) { onclick() }
                },
                border = BorderStroke(0.dp, Color.Transparent),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                content = {
                    Text(
                        style = MaterialTheme.typography.bodyMedium,
                        text = text!!,
                        color = Colors.PRIMARY.color
                    )
                },
            )
        }
        ButtonStyles.Icon -> {
            TextButton(
                modifier = modifier
                    .aspectRatio(1f),
                onClick = {
                    if (!State.isAnimating) { onclick() }
                },
                contentPadding = PaddingValues(iconPadding),
                enabled = enabled
            ) {
                Image(
                    modifier = Modifier
                        .aspectRatio(1f),
                    painter = painterResource(iconSrc!!),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter
                )
            }
        }
        ButtonStyles.Solid -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .clickable {
                        if (!State.isAnimating && enabled) { onclick() }
                    },
            )
        }
    }
}

fun clickWithTransition(
    route: Routes
) {
    if (State.isOffline || State.isConnectedToServer) {
        State.isAnimating = true
        if (route == Routes.BACK) {
            State.navController!!.popBackStack()
        } else State.navController!!.navigate(route.path)
    } else State.isConnectedToServer = false
}