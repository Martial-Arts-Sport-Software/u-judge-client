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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import judging_app_client.composeapp.generated.resources.Res
import judging_app_client.composeapp.generated.resources.range_input_point
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.judging_app.State
import org.judging_app.enums.Colors
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Modes of range input
 * @property NUMBERS_ONLY - display only numbers without input itself
 * @property DEFAULT - range input display by default
 * @property WITH_TEXT - 
 */
enum class Modes {
    NUMBERS_ONLY,
    WITH_TEXT,
    DEFAULT
}

/**
 * Renders range input for criterion rating
 * @param currentValue initial value on render
 * @param onValueChange callback, that is called on range value's change
 * @param modifier [Modifier], that is applied to the component
 * @param steps count of range values
 * @param mode [Modes] variant of range input
 * @param ratio part that component is contained in its parent
 * @param icon [DrawableResource] instance of required icon
 * @param text optional text displayed above range input ([Modes.WITH_TEXT] is required)
 */
@Composable
fun RangeInputComponent(
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 10,
    mode: Modes = Modes.DEFAULT,
    ratio: Float = 1f,
    icon: DrawableResource? = null,
    text: String = ""
) {
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    val markSizeDp by remember(trackSize) {
            derivedStateOf {
                with(State.density!!) {
                    (trackSize.height * 0.2f).toDp()
            }
        }
    }
    val stepBoxSize by remember(trackSize) {
        derivedStateOf { (trackSize.width - trackSize.height) / steps + 1 }
    }
    val edgeBoxSize by remember(stepBoxSize) {
        derivedStateOf { (stepBoxSize + trackSize.height) / 2 }
    }
    val coroutineScope = rememberCoroutineScope()
    val stepPositions by remember(edgeBoxSize, stepBoxSize, steps, trackSize) {
        derivedStateOf {
            List(steps + 1) { i ->
                when (i) {
                    0 -> 0f
                    steps -> (trackSize.width - trackSize.height).toFloat()
                    else -> (edgeBoxSize + (i - 1) * stepBoxSize + (stepBoxSize - trackSize.height) / 2).toFloat()
                }
            }
        }
    }
    val anchors = remember(stepPositions) {
        DraggableAnchors {
            stepPositions.forEachIndexed { index, value ->
                index.toFloat() at value
            }
        }
    }
    val state = remember(anchors) {
        AnchoredDraggableState(
            initialValue = currentValue * 10,
            anchors = anchors,
        )
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()

    LaunchedEffect(anchors) { state.updateAnchors(anchors) }
    LaunchedEffect(currentValue) {
        if (state.currentValue != currentValue && !state.isAnimationRunning) {
            state.animateTo(currentValue * 10)
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isAnimationRunning }
            .collect { running ->
                if (!running) onValueChange(state.currentValue / 10f)
            }
    }

    LaunchedEffect(isDragged) {
        if (!isDragged) onValueChange(state.currentValue / 10f)
    }

    Row(
        modifier = modifier
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
            Spacer(Modifier.width(15.dp))
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
                    .aspectRatio(22 / 1f),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .onSizeChanged { trackSize = it },
                    verticalAlignment = Alignment.Bottom,
                ){
                    for (i in 0..steps) {
                        val boxWidth = with(State.density!!) {
                            if (i == 0 || i == steps) edgeBoxSize.toDp() else stepBoxSize.toDp()
                        }
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
                            .width(boxWidth)
                        Column(
                            modifier = stepModifier
                        ) {
                            if (mode == Modes.NUMBERS_ONLY) {
                                BoxWithConstraints(
                                    Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .offset(
                                                x = with(State.density!!) {
                                                    when(i) {
                                                        0 -> (trackSize.height.toDp() - maxWidth) / 2
                                                        steps -> maxWidth - (trackSize.height.toDp() + maxWidth) / 2
                                                        else -> 0.dp
                                                    }
                                                }
                                            )
                                            .fillMaxHeight()
                                            .width(maxWidth),
                                        text = "${floor(i / 10f).roundToInt()}.${i % 10}",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            } else {
                                BoxWithConstraints(
                                    Modifier
                                        .alpha(if (mode != Modes.NUMBERS_ONLY) 0.85f else 0f)
                                        .fillMaxSize()
                                        .clip(when(i) {
                                            0 -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                                            steps -> RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
                                            else -> RoundedCornerShape(0)
                                        })
                                        .background(Colors.PRIMARY.color),
                                ) {
                                    Box(
                                        Modifier
                                            .offset(
                                                x = with(State.density!!) {
                                                    when(i) {
                                                        0 -> 0.dp
                                                        steps -> maxWidth - (trackSize.height.toDp() + markSizeDp) / 2
                                                        else -> (maxWidth - markSizeDp) / 2
                                                    }
                                                }
                                            )
                                            .width(markSizeDp)
                                            .fillMaxHeight(0.5f)
                                            .clip(CircleShape)
                                            .align(Alignment.CenterStart)
                                            .background(Colors.SLIDER_TRACK_ACTIVE.color)
                                    )
                                }
                            }
                        }
                    }
                }
                if (mode != Modes.NUMBERS_ONLY) {
                    Box(
                        Modifier
                            .width(with(State.density!!) {
                                (state.requireOffset().roundToInt() + trackSize.height).toDp()
                            })
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .align(Alignment.BottomStart)
                            .background(Colors.SLIDER_TRACK_ACTIVE.color)
                    ) {

                    }
                    Image(
                        modifier = Modifier
                            .fillMaxHeight()
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
                                interactionSource = interactionSource
                            ),
                        painter = painterResource(Res.drawable.range_input_point),
                        contentDescription = null
                    )
                }
            }
        }
    }
}