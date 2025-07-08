package org.judging_app.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.judging_app.State

enum class ButtonStyles {
    Primary,
    Secondary,
    Plain,
    Icon
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonComponent(
    text: String? = null,
    style: ButtonStyles = ButtonStyles.Primary,
    modifier: Modifier = Modifier,
    onclick: () -> Unit,
    iconSrc: DrawableResource? = null,
    colorFilter: ColorFilter? = null
) {
    if (style == ButtonStyles.Icon) require(iconSrc != null)
    else require(text != null)
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
                        .fillMaxHeight(),
                    onClick = {
                        if (!State.isAnimating.value) { onclick() }
                    },
                ) {
                    Image(
                        modifier = Modifier
                            .fillMaxHeight(0.9f),
                        painter = painterResource(iconSrc!!),
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
                        colorFilter = colorFilter
                    )
                }
            }
        }
    }
}

fun clickWithTransition(
    route: State.Routes
) {
    State.isAnimating.value = true
    if (route == State.Routes.BACK) {
        State.navController!!.popBackStack()
    } else State.navController!!.navigate(route.path)
}