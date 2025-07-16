package org.judging_app.ui.input

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.range_input_point
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.judging_app.State
import kotlin.math.floor
import kotlin.math.roundToInt

enum class Modes {
    NUMBERS_ONLY,
    WITH_TEXT,
    DEFAULT
}

@Composable
fun RangeInputComponent(
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    steps: Int = 10,
    mode: Modes = Modes.DEFAULT,
    ratio: Float = 1f,
    icon: DrawableResource? = null,
    text: String = ""
) {
    var trackSize by remember { mutableStateOf(IntSize.Zero)}
    var thumbSize by remember { mutableStateOf(IntSize.Zero) }

    val stepBoxSize by remember(trackSize, thumbSize) {
        mutableStateOf((trackSize.width - thumbSize.width) / steps)
    }
    val edgeBoxSize by remember(stepBoxSize) {
        mutableStateOf((thumbSize.width + stepBoxSize) / 2)
    }
    val markSizeDp = 5.dp
    val coroutineScope = rememberCoroutineScope()

    val stepPositions = remember(edgeBoxSize) {
        mutableListOf<Float>().apply {
            for (i in 0..< steps + 1) {
                add(when(i) {
                    0 -> 0f
                    steps -> (edgeBoxSize + (steps - 1) * stepBoxSize +
                            edgeBoxSize - thumbSize.width).toFloat()
                    else -> i * stepBoxSize.toFloat()
                })
            }
        }
    }
    val anchors = remember(stepPositions) { DraggableAnchors {
        stepPositions.forEachIndexed { index, value ->
            index.toFloat() at value
        }
    } }
    val state = remember(anchors) {
        AnchoredDraggableState(
            initialValue = currentValue,
            anchors = anchors,
        )
    }
    fun getMarkModifier(i: Int, maxWidth: Dp, clip: Boolean = false): Modifier {
        return Modifier
            .offset(
                x = with(State.density!!) {
                    when(i) {
                        0 -> (thumbSize.width.toDp() - markSizeDp) / 2
                        steps -> maxWidth - (thumbSize.width.toDp() + markSizeDp) / 2
                        else -> (maxWidth - markSizeDp) / 2
                    }
                }
            )
            .fillMaxHeight()
            .width(markSizeDp)
            .clip(if (clip) RoundedCornerShape(
                topStartPercent = 50,
                topEndPercent = 50
            ) else RectangleShape)
            .background(
                if (state.currentValue >= i)
                    State.Colors.SLIDER_TRACK_ACTIVE.color
                else State.Colors.PRIMARY.color
            )
    }

    LaunchedEffect(state) {
        snapshotFlow { state.currentValue }.collect { value ->
            onValueChange(value)
        }
    }
    Row(
        Modifier
            .fillMaxWidth(ratio),
        verticalAlignment = Alignment.Bottom
    ) {
        if (icon != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(1 / 24f)
                    .alpha(if (mode != Modes.NUMBERS_ONLY) 1f else 0f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    painter = painterResource(icon),
                    contentDescription = null
                )
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (mode == Modes.WITH_TEXT) {
                Row(
                    Modifier
                        .wrapContentHeight()
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Left
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(20 / 1f),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { trackSize = it },
                    verticalAlignment = Alignment.Bottom
                ){
                    for (i in IntRange(0, steps)) {
                        val stepModifier = Modifier
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                enabled = mode != Modes.NUMBERS_ONLY,
                                onClick = {
                                    coroutineScope.launch {
                                        state.animateTo(
                                            targetValue = i.toFloat(),
                                            animationSpec = tween(300)
                                        )
                                    }
                                }
                            )
                            .width(with(State.density!!) {
                                when(i) {
                                    0, steps -> edgeBoxSize.toDp()
                                    else -> stepBoxSize.toDp()
                                }
                            })
                        Column(
                            modifier = stepModifier
                        ) {
                            BoxWithConstraints(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(if (mode != Modes.NUMBERS_ONLY) 0.15f else 0.5f)
                            ) {
                                if (mode == Modes.NUMBERS_ONLY) {
                                    Text(
                                        modifier = Modifier
                                            .offset(
                                                x = with(State.density!!) {
                                                    when(i) {
                                                        0 -> (thumbSize.width.toDp() - maxWidth) / 2
                                                        steps -> maxWidth - (thumbSize.width.toDp() + maxWidth) / 2
                                                        else -> (maxWidth - maxWidth) / 2
                                                    }
                                                }
                                            )
                                            .fillMaxHeight()
                                            .align(Alignment.Center)
                                            .width(maxWidth),
                                        text = "${floor(i / 10f).roundToInt()}.${i % 10}",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                } else Box(modifier = getMarkModifier(i, maxWidth, true))
                            }
                            BoxWithConstraints(
                                Modifier
                                    .alpha(if (mode != Modes.NUMBERS_ONLY) 1f else 0f)
                                    .fillMaxSize()
                                    .clip(when(i) {
                                        0 -> RoundedCornerShape(topStartPercent = 25, bottomStartPercent = 25)
                                        steps -> RoundedCornerShape(topEndPercent = 25, bottomEndPercent = 25)
                                        else -> RoundedCornerShape(0)
                                    })
                                    .background(State.Colors.PRIMARY.color)
                            ) { Box(modifier = getMarkModifier(i, maxWidth)) }
                        }
                    }
                }
                Box(
                    Modifier
                        .alpha(if (mode != Modes.NUMBERS_ONLY) 1f else 0f)
                        .width(with(State.density!!) {
                                (state.requireOffset().roundToInt() + thumbSize.width).toDp()
                        })
                        .fillMaxHeight(0.85f)
                        .clip(RoundedCornerShape(25))
                        .align(Alignment.BottomStart)
                        .background(State.Colors.SLIDER_TRACK_ACTIVE.color)
                )
                Image(
                    modifier = Modifier
                        .alpha(if (mode != Modes.NUMBERS_ONLY) 1f else 0f)
                        .fillMaxHeight(0.85f)
                        .aspectRatio(1f)
                        .offset {
                            IntOffset(
                                x = state.requireOffset().roundToInt(),
                                y = 0,
                            )
                        }
                        .align(Alignment.BottomStart)
                        .anchoredDraggable(
                            state,
                            Orientation.Horizontal,
                            enabled = mode != Modes.NUMBERS_ONLY,
                        )
                        .onSizeChanged { thumbSize = it },
                    painter = painterResource(Res.drawable.range_input_point),
                    contentDescription = null
                )
            }
        }
    }
}