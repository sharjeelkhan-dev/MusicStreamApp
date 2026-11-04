package com.musicstream.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    inactiveColor: Color = Color.Unspecified,
    barCount: Int = 40,
    height: Dp = 60.dp,
    centered: Boolean = true,
    onSeekStarted: ((Float) -> Unit)? = null,
    onSeekProgress: ((Float) -> Unit)? = null,
    onSeekFinished: ((Float) -> Unit)? = null
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }

    LaunchedEffect(progress) {
        if (!isDragging) {
            dragProgress = progress
        }
    }

    val currentDisplayProgress = if (isDragging) dragProgress else progress
    val resolvedInactiveColor = if (inactiveColor != Color.Unspecified) {
        inactiveColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    }

    // Continuous, seamless phase accumulator - runs without any loop resets or GIF-like restarts
    var phase by remember { mutableFloatStateOf(0f) }
    val animatedAmplitude = remember { Animatable(if (isPlaying) 1f else 0f) }

    LaunchedEffect(isPlaying) {
        animatedAmplitude.animateTo(
            targetValue = if (isPlaying) 1f else 0f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        var lastFrameTime = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTime != 0L) {
                    val dt = (frameTimeNanos - lastFrameTime) / 1_000_000_000f
                    val clampedDt = dt.coerceIn(0f, 0.05f)
                    val amp = animatedAmplitude.value
                    if (amp > 0.001f) {
                        phase = (phase + clampedDt * 2.8f) % 10000f
                    }
                }
                lastFrameTime = frameTimeNanos
            }
        }
    }

    val baseHeights = remember(barCount) {
        FloatArray(barCount) { i ->
            val norm = i.toFloat() / (barCount - 1).coerceAtLeast(1)
            val envelope = sin(norm * Math.PI.toFloat()).coerceIn(0.45f, 1f)
            val variation = 0.5f + 0.5f * abs(sin(i * 1.75f))
            (envelope * variation).coerceIn(0.3f, 1f)
        }
    }

    val pointerModifier = if (onSeekFinished != null) {
        modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                        onSeekStarted?.invoke(newProgress)
                        onSeekFinished(newProgress)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                        onSeekStarted?.invoke(newProgress)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragProgress = newProgress
                        onSeekProgress?.invoke(newProgress)
                    },
                    onDragEnd = {
                        onSeekFinished(dragProgress)
                        isDragging = false
                    },
                    onDragCancel = {
                        onSeekFinished(dragProgress)
                        isDragging = false
                    }
                )
            }
    } else {
        modifier
    }

    Canvas(
        modifier = pointerModifier
            .fillMaxWidth()
            .height(height)
    ) {
        val width = size.width
        val canvasHeight = size.height
        val barSpacing = 3.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = ((width - totalSpacing) / barCount).coerceAtLeast(3f)
        val amp = animatedAmplitude.value

        for (i in 0 until barCount) {
            val baseHeightFactor = baseHeights[i]

            val wave1 = sin(phase + (i * 0.32f))
            val wave2 = sin((phase * 0.72f) - (i * 0.22f))
            val wave3 = cos((phase * 1.35f) + (i * 0.45f))
            val waveFactor = ((wave1 * 0.45f + wave2 * 0.35f + wave3 * 0.2f + 1f) / 2f).coerceIn(0f, 1f)

            val dynamicFactor = baseHeightFactor * (0.35f + 0.65f * (waveFactor * amp + (1f - amp) * 0.4f))
            val factor = dynamicFactor.coerceIn(0.18f, 1.0f)

            val barHeight = (canvasHeight * factor).coerceAtLeast(barWidth)
            val x = i * (barWidth + barSpacing)
            val y = if (centered) (canvasHeight - barHeight) / 2f else canvasHeight - barHeight

            val barProgressThreshold = i.toFloat() / barCount
            val isPlayed = barProgressThreshold <= currentDisplayProgress

            val brush = if (isPlayed) {
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.85f),
                        accentColor
                    ),
                    startY = y,
                    endY = y + barHeight
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        resolvedInactiveColor.copy(alpha = 0.35f),
                        resolvedInactiveColor.copy(alpha = 0.6f)
                    ),
                    startY = y,
                    endY = y + barHeight
                )
            }

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
