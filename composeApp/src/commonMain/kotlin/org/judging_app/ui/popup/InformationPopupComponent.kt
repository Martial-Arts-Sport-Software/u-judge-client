package org.judging_app.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.cross_icon
import kotlinx.coroutines.launch
import org.judging_app.State
import org.judging_app.enums.Colors
import org.judging_app.locale.Localization
import org.judging_app.ui.button.ButtonComponent
import org.judging_app.ui.button.ButtonStyles

/**
 * Renders information popup
 */
@Composable
fun InformationPopupComponent(
    lines: List<String>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var textColumnSize by remember { mutableStateOf(IntSize.Zero) }
    var scrollbarSize by remember { mutableStateOf(IntSize.Zero) }
    var thumbSize by remember { mutableStateOf(IntSize.Zero) }

    val scrollState = rememberScrollState()
    val scrollFraction = if (scrollState.maxValue > 0)
        scrollState.value.toFloat() / scrollState.maxValue.toFloat()
    else 0f


    Box(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight(0.2f)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 15.dp),
                text = Localization.getString("hosinsool-info-title"),
                style = MaterialTheme.typography.displayLarge
            )
        }
        ButtonComponent(
            modifier = Modifier
                .fillMaxHeight(0.2f)
                .align(Alignment.TopEnd),
            style = ButtonStyles.Icon,
            iconSrc = Res.drawable.cross_icon,
            onclick = {
                State.currentPopupMode = Popup.Modes.NONE
            }
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(start = 15.dp, end = 15.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier
                    .verticalScroll(scrollState)
                    .fillMaxHeight()
                    .fillMaxWidth(0.98f)
                    .onSizeChanged { textColumnSize = it }
            ) {
                lines.forEach { line ->
                    Text(
                        text = line,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.Black
                    )
                }
            }
            Box(
                Modifier
                    .width(10.dp)
                    .fillMaxHeight()
                    .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .onSizeChanged { scrollbarSize = it }
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1 / 3f)
                        .offset(y = with(State.density!!) { (scrollFraction * (scrollbarSize.height - thumbSize.height)).toDp() })
                        .background(Colors.PRIMARY.color, RoundedCornerShape(50))
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                val maxDrag = textColumnSize.height - scrollbarSize.height
                                val adjustedDelta = delta * 6f
                                val newScrollPosition = (scrollState.value + adjustedDelta / maxDrag * scrollState.maxValue)
                                    .coerceIn(0f, scrollState.maxValue.toFloat())

                                coroutineScope.launch {
                                    scrollState.scrollTo(newScrollPosition.toInt())
                                }
                            }
                        )
                        .onSizeChanged { thumbSize = it }
                )
            }
        }
    }
}
