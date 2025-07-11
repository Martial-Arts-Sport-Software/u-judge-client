package org.judging_app.ui.input

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.forEach
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.range_input_point
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.judging_app.State
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt



@Composable
fun RangeInputComponent(
    steps: Int = 10,
    ratio: Float = 1f,
    icon: DrawableResource? = null,
    showNumbers: Boolean = false
) {
    var trackSize by remember { mutableStateOf(IntSize.Zero)}
    var thumbSize by remember { mutableStateOf(IntSize.Zero) }
    var trackOffset by remember { mutableStateOf(0f) }
    val stepBoxSize by remember(trackSize, thumbSize) {
        mutableStateOf((trackSize.width - thumbSize.width) / steps)
    }
    val edgeBoxSize by remember(stepBoxSize, thumbSize) {
        mutableStateOf((thumbSize.width + stepBoxSize) / 2)
    }
    val markSizeDp by remember(thumbSize) { with(State.density!!) {
        mutableStateOf((thumbSize.width / 5).toDp())
    } }
    val coroutineScope = rememberCoroutineScope()

    val stepPositions = remember(steps, trackSize, thumbSize, trackOffset) {
        mutableListOf<Float>().apply {
            for (i in 0..< steps + 1) {
                add(
                    trackOffset + when(i) {
                        0 -> 0f
                        steps -> (edgeBoxSize + (steps - 1) * stepBoxSize + edgeBoxSize - thumbSize.width).toFloat()
                        else -> i * stepBoxSize.toFloat()
                    }
                )
            }
        }
    }
    val anchors = remember(stepPositions, thumbSize) { DraggableAnchors {
        stepPositions.forEachIndexed { index, value ->
            index.toFloat() at value
        }
    } }
    val state = remember(anchors) {
        AnchoredDraggableState(
            initialValue = 0f,
            anchors = anchors,
        )
    }

    Row(
        Modifier.fillMaxWidth(ratio)
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .weight(1 / 12f)
                    .aspectRatio(1f)
            )
        }
        Column(
            Modifier
                .weight(11 / 12f)
                .aspectRatio(37 / 3f)
        ) {
            if (showNumbers) {
                Row(
                    Modifier
                        .weight(0.5f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (i in IntRange(0, steps)) {
                        val stepModifier = Modifier
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = {
                                    coroutineScope.launch {
                                        state.animateTo(
                                            targetValue = i.toFloat(),
                                            animationSpec = tween(300)
                                        )
                                    }
                                }
                            )
                        BoxWithConstraints(
                            modifier = stepModifier
                                .width(with(State.density!!) {
                                    when(i) {
                                        0, steps -> edgeBoxSize.toDp()
                                        else -> stepBoxSize.toDp()
                                    }
                                }),
                        ) {
                            Text(
                                modifier = Modifier
                                    .offset(
                                        x = with(State.density!!) {
                                            when(i) {
                                                0 -> (thumbSize.width.toDp() - maxWidth * 0.8f) / 2
                                                steps -> maxWidth - (thumbSize.width.toDp() + maxWidth * 0.8f) / 2
                                                else -> (maxWidth - maxWidth * 0.8f) / 2
                                            }
                                        }
                                    )
                                    .width(maxWidth * 0.8f)
                                    .fillMaxHeight(),
                                text = "${floor(i / 10f).roundToInt()}.${i % 10}",
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(0.2f))
            }
            Row(
                Modifier
                    .weight(0.15f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in IntRange(0, steps)) {
                    val stepModifier = Modifier
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = {
                                coroutineScope.launch {
                                    state.animateTo(
                                        targetValue = i.toFloat(),
                                        animationSpec = tween(300)
                                    )
                                }
                            }
                        )
                    BoxWithConstraints(
                        modifier = stepModifier
                            .width(with(State.density!!) {
                                when(i) {
                                    0, steps -> edgeBoxSize.toDp()
                                    else -> stepBoxSize.toDp()
                                }
                            }),
                    ) {
                        Box(
                            Modifier
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
                                .clip(RoundedCornerShape(
                                    topStart = 50.dp,
                                    topEnd = 50.dp
                                ))
                                .background(
                                    if (state.currentValue >= i)
                                        State.Colors.SLIDER_TRACK_ACTIVE.color
                                    else State.Colors.PRIMARY.color
                                )
                        )
                    }
                }
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(0.85f)
                    .onSizeChanged { trackSize = it }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.Center
                ){
                    for (i in IntRange(0, steps)) {
                        val stepModifier = Modifier
                            .fillMaxHeight()
                            .clip(
                                when(i) {
                                    0 -> RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                                    steps -> RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                                    else -> RoundedCornerShape(0.dp)
                                }
                            )
                            .background(State.Colors.PRIMARY.color)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = {
                                    coroutineScope.launch {
                                        state.animateTo(
                                            targetValue = i.toFloat(),
                                            animationSpec = tween(300)
                                        )
                                    }
                                }
                            )
                            .onGloballyPositioned { coordinates ->
                                if (i == 0) trackOffset = coordinates.positionInParent().x
                            }
                        BoxWithConstraints(
                            modifier = stepModifier
                                .width(with(State.density!!) {
                                    when(i) {
                                        0, steps -> edgeBoxSize.toDp()
                                        else -> stepBoxSize.toDp()
                                    }
                                }),
                        ) {
                            Box(
                                Modifier
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
                                    .background(
                                        if (state.currentValue >= i)
                                            State.Colors.SLIDER_TRACK_ACTIVE.color
                                        else State.Colors.PRIMARY.color
                                    )
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .width(
                            with(State.density!!) {
                                (state.requireOffset().roundToInt() + thumbSize.width).toDp()
                            }
                        )
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(State.Colors.SLIDER_TRACK_ACTIVE.color)
                )
                Image(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .offset {
                            IntOffset(
                                x = state
                                    .requireOffset()
                                    .roundToInt(),
                                y = 0,
                            )
                        }
                        .anchoredDraggable(
                            state,
                            Orientation.Horizontal
                        )
                        .onSizeChanged { thumbSize = it },
                    painter = painterResource(Res.drawable.range_input_point),
                    contentDescription = null
                )
            }
        }
    }
}