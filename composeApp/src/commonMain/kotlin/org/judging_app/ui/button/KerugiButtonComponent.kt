package org.judging_app.ui.button

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.kerugi_chestplate
import judging_app_client.composeapp.generated.resources.kerugi_helmet
import judging_app_client.composeapp.generated.resources.tanbon_body
import judging_app_client.composeapp.generated.resources.tanbon_cross
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.judging_app.State

enum class KerugiButtonStyles(val icon: DrawableResource) {
    HELMET(Res.drawable.kerugi_helmet),
    CHESTPLATE(Res.drawable.kerugi_chestplate),
    BODY(Res.drawable.tanbon_body),
    TANBON_CROSS(Res.drawable.tanbon_cross)
}

enum class KerugiButtonPositions {
    LEFT,
    RIGHT
}

@Composable
fun KerugiButtonComponent(
    style: KerugiButtonStyles,
    position: KerugiButtonPositions,
    color: State.Colors,
    icon: DrawableResource,
    onclick: () -> Unit
) {
    Button(
        modifier = Modifier,
        shape = if (position == KerugiButtonPositions.LEFT)
            RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 15.dp,
                bottomStart = 15.dp,
                bottomEnd = 0.dp
            ) else RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 15.dp,
                bottomStart = 15.dp,
                bottomEnd = 0.dp
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