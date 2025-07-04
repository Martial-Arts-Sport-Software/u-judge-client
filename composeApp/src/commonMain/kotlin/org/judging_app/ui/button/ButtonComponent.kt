package org.judging_app.ui.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.judging_app.State

enum class Styles() {
    Primary,
    Secondary,
    Plain
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButtonComponent(
    text: String,
    style: Styles = Styles.Primary,
    modifier: Modifier = Modifier,
    onclick: () -> Unit
) {
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        when(style) {
            Styles.Primary -> {
                Button(
                    modifier = modifier.fillMaxWidth(0.8f),
                    onClick = {
                        if (!State.isAnimating.value) onclick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7C45E2)
                    ),
                    shape = RoundedCornerShape(5.dp),
                    content = {
                        Text(
                            style = MaterialTheme.typography.bodyLarge,
                            text = text
                        )
                    },
                )
            }
            Styles.Secondary -> {
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
                            text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF7C45E2)
                        )
                    }
                )
            }
            Styles.Plain -> {
                Button(
                    modifier = modifier
                        .fillMaxWidth(0.8f),
                    onClick = {
                        if (!State.isAnimating.value) {
                            onclick()
                        }
                    },
                    border = BorderStroke(0.dp, Color.Transparent),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    content = {
                        Text(
                            style = MaterialTheme.typography.bodyMedium,
                            text = text,
                            color = Color(0xFF7C45E2)
                        )
                    },
                )
            }
        }
    }
}

fun clickWithTransition(
    route: State.Routes
) {
    State.isAnimating.value = true
    State.navController!!.navigate(route.path)
}