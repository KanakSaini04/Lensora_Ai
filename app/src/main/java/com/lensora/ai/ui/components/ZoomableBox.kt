package com.lensora.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground

object ZoomTransformMath {
    fun calculateScale(currentScale: Float, zoomChange: Float, minScale: Float = 1.0f, maxScale: Float = 8.0f): Float {
        return (currentScale * zoomChange).coerceIn(minScale, maxScale)
    }

    fun toggleDoubleTapScale(currentScale: Float, targetScale: Float = 2.0f): Float {
        return if (currentScale > 1.05f) 1.0f else targetScale
    }

    fun calculateClampedPan(
        currentPan: Offset,
        panChange: Offset,
        scale: Float,
        containerSize: Size,
    ): Offset {
        if (scale <= 1.0f || containerSize.width <= 0f || containerSize.height <= 0f) return Offset.Zero

        val scaledWidth = containerSize.width * scale
        val scaledHeight = containerSize.height * scale

        val maxPanX = (scaledWidth - containerSize.width) / 2f
        val maxPanY = (scaledHeight - containerSize.height) / 2f

        val newX = (currentPan.x + panChange.x).coerceIn(-maxPanX, maxPanX)
        val newY = (currentPan.y + panChange.y).coerceIn(-maxPanY, maxPanY)

        return Offset(newX, newY)
    }

    fun mapScreenPointToImagePixel(
        screenPoint: Offset,
        containerSize: Size,
        imageSize: Size,
        scale: Float,
        pan: Offset,
    ): Offset {
        if (containerSize.width <= 0f || containerSize.height <= 0f || imageSize.width <= 0f || imageSize.height <= 0f) {
            return Offset.Zero
        }

        val centerX = containerSize.width / 2f
        val centerY = containerSize.height / 2f

        val unscaledX = (screenPoint.x - centerX - pan.x) / scale + centerX
        val unscaledY = (screenPoint.y - centerY - pan.y) / scale + centerY

        val scaleFactor = minOf(containerSize.width / imageSize.width, containerSize.height / imageSize.height)
        val drawnWidth = imageSize.width * scaleFactor
        val drawnHeight = imageSize.height * scaleFactor

        val imageLeft = (containerSize.width - drawnWidth) / 2f
        val imageTop = (containerSize.height - drawnHeight) / 2f

        val imageX = ((unscaledX - imageLeft) / scaleFactor).coerceIn(0f, imageSize.width)
        val imageY = ((unscaledY - imageTop) / scaleFactor).coerceIn(0f, imageSize.height)

        return Offset(imageX, imageY)
    }

    fun formatZoomPercentage(scale: Float): String {
        return "${(scale * 100).toInt()}%"
    }

    fun resolveInitialPageIndex(targetUriStr: String?, items: List<String>): Int {
        if (targetUriStr.isNullOrBlank()) return 0
        val index = items.indexOf(targetUriStr)
        return if (index != -1) index else 0
    }
}

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    maxScale: Float = 8.0f,
    onScaleChanged: (Float) -> Unit = {},
    resetKey: Any? = null,
    content: @Composable BoxScope.(scale: Float, pan: Offset) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(resetKey) {
        scale = 1.0f
        pan = Offset.Zero
        onScaleChanged(1.0f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it.toSize() }
            .pointerInput(resetKey) {
                detectTapGestures(
                    onDoubleTap = {
                        val newScale = ZoomTransformMath.toggleDoubleTapScale(scale)
                        scale = newScale
                        pan = if (newScale == 1.0f) Offset.Zero else pan
                        onScaleChanged(newScale)
                    },
                )
            }
            .pointerInput(resetKey) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    val newScale = ZoomTransformMath.calculateScale(scale, zoomChange, maxScale = maxScale)
                    val newPan = ZoomTransformMath.calculateClampedPan(pan, panChange, newScale, containerSize)
                    scale = newScale
                    pan = newPan
                    onScaleChanged(newScale)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = pan.x
                    translationY = pan.y
                },
            contentAlignment = Alignment.Center,
        ) {
            content(scale, pan)
        }

        // Live Zoom Percentage Pill Badge Overlay (Top Right)
        AnimatedVisibility(
            visible = scale > 1.02f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PitchBlackBackground.copy(alpha = 0.8f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = ZoomTransformMath.formatZoomPercentage(scale),
                    color = ColorWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
