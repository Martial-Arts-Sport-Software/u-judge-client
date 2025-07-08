package org.judging_app.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.sensitiveContent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.judging_app.State

enum class KerugiButtonPositions {
    LEFT,
    RIGHT,
    CENTER
}

@Composable
fun KerugiButtonComponent(
    position: KerugiButtonPositions,
    color: State.Colors,
    icon: DrawableResource,
    onclick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                if (position == KerugiButtonPositions.RIGHT)
                    scaleX = -1f
            },
        border = BorderStroke(3.dp, State.Colors.BUTTON_BROWN.color),
        shape = when (position) {
            KerugiButtonPositions.LEFT -> RoundedCornerShape(
                topStart = 10.dp,
                topEnd = 20.dp,
                bottomStart = 10.dp,
                bottomEnd = 20.dp
            )
            KerugiButtonPositions.RIGHT -> RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 10.dp,
                bottomStart = 20.dp,
                bottomEnd = 10.dp
            )
            KerugiButtonPositions.CENTER -> RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = 20.dp,
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