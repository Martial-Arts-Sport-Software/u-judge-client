package org.judging_app.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.judging_app.State

enum class ButtonStyles {
    Primary,
    Secondary,
    Plain,
    Icon,
    Solid
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonComponent(
    text: String? = null,
    style: ButtonStyles = ButtonStyles.Primary,
    modifier: Modifier = Modifier,
    onclick: () -> Unit,
    iconSrc: DrawableResource? = null,
    colorFilter: ColorFilter? = null,
    iconPadding: Dp = 10.dp
) {
    when(style) {
        ButtonStyles.Icon -> require(iconSrc != null)
        ButtonStyles.Primary,
             ButtonStyles.Secondary,
                 ButtonStyles.Plain -> require(text != null)
        else -> {}
    }
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        when(style) {
            ButtonStyles.Primary -> {
                Button(
                    modifier = modifier
                        .fillMaxWidth(0.8f),
                    onClick = {
                        if (!State.isAnimating.value) onclick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = State.Colors.PRIMARY.color
                    ),
                    shape = RoundedCornerShape(5.dp),
                    content = {
                        Text(
                            style = MaterialTheme.typography.bodyLarge,
                            text = text!!,
                            textAlign = TextAlign.Center
                        )
                    },
                )
            }
            ButtonStyles.Secondary -> {
                Button(
                    modifier = modifier.fillMaxWidth(0.8f),
                    onClick = {
                        if (!State.isAnimating.value) {
                            onclick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(0.48f)
                    ),
                    shape = RoundedCornerShape(5.dp),
                    content = {
                        Text(
                            text!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = State.Colors.PRIMARY.color
                        )
                    }
                )
            }
            ButtonStyles.Plain -> {
                Button(
                    modifier = modifier
                        .fillMaxWidth(0.8f),
                    onClick = {
                        if (!State.isAnimating.value) { onclick() }
                    },
                    border = BorderStroke(0.dp, Color.Transparent),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    content = {
                        Text(
                            style = MaterialTheme.typography.bodyMedium,
                            text = text!!,
                            color = State.Colors.PRIMARY.color
                        )
                    },
                )
            }
            ButtonStyles.Icon -> {
                TextButton(
                    modifier = modifier
                        .aspectRatio(1f),
                    onClick = {
                        if (!State.isAnimating.value) { onclick() }
                    },
                    contentPadding = PaddingValues(iconPadding)
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
                            if (!State.isAnimating.value) { onclick() }
                        },
                )
            }
        }
    }
}

fun clickWithTransition(
    route: State.Routes
) {
    if (State.isOffline.value || State.isConnectedToServer.value) {
        State.isAnimating.value = true
        if (route == State.Routes.BACK) {
            State.navController!!.popBackStack()
        } else State.navController!!.navigate(route.path)
    } else State.isConnectedToServer.value = false
}