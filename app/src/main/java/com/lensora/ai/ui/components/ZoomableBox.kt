package com.lensora.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import com.lensora.ai.ui.theme.RoyalBlue

enum class CameraSwipeDecision {
    NAVIGATE_NEXT_TAB,
    NONE
}

enum class TouchMode {
    DRAW,
    PAN,
    ZOOM_PAN,
    NONE
}

enum class StrokePaintStyle {
    FILL,
    STROKE,
    NONE
}

object ZoomTransformMath {
    fun decideTouchMode(
        pointerCount: Int,
        scale: Float,
        isDrawToolActive: Boolean,
        isStudio: Boolean,
    ): TouchMode {
        if (pointerCount <= 0 || scale <= 0f) return TouchMode.NONE

        return when {
            pointerCount >= 2 -> TouchMode.ZOOM_PAN
            isStudio && isDrawToolActive -> TouchMode.DRAW
            scale > 1.05f -> TouchMode.PAN
            else -> TouchMode.NONE
        }
    }

    fun decideStrokePaintStyle(pointCount: Int): StrokePaintStyle {
        return when {
            pointCount > 1 -> StrokePaintStyle.STROKE
            pointCount == 1 -> StrokePaintStyle.FILL
            else -> StrokePaintStyle.NONE
        }
    }

    fun snapSensorDegreesToSurfaceRotation(degrees: Int, lastRotationDegrees: Int = 0): Int {
        if (degrees == -1) return lastRotationDegrees
        val normDegrees = (degrees % 360 + 360) % 360
        return when (normDegrees) {
            in 45..134 -> 270
            in 135..224 -> 180
            in 225..314 -> 90
            else -> 0
        }
    }

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

    fun calculateLocalCanvasBrushWidthPx(screenDp: Float, density: Float, scale: Float): Float {
        if (scale <= 0f) return screenDp * density
        val screenPx = screenDp * density
        return screenPx / scale
    }

    fun calculateImageRadiusFromCanvasPx(radiusCanvasPx: Float, canvasSize: Size, imageSize: Size): Float {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f || imageSize.width <= 0f || imageSize.height <= 0f) {
            return radiusCanvasPx
        }
        val scaleFactor = minOf(canvasSize.width / imageSize.width, canvasSize.height / imageSize.height)
        return if (scaleFactor > 0f) radiusCanvasPx / scaleFactor else radiusCanvasPx
    }

    fun mapCanvasLocalPointToImagePixel(
        canvasPoint: Offset,
        canvasSize: Size,
        imageSize: Size,
    ): Offset {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f || imageSize.width <= 0f || imageSize.height <= 0f) {
            return Offset.Zero
        }

        val scaleFactor = minOf(canvasSize.width / imageSize.width, canvasSize.height / imageSize.height)
        val drawnWidth = imageSize.width * scaleFactor
        val drawnHeight = imageSize.height * scaleFactor

        val imageLeft = (canvasSize.width - drawnWidth) / 2f
        val imageTop = (canvasSize.height - drawnHeight) / 2f

        val imageX = ((canvasPoint.x - imageLeft) / scaleFactor).coerceIn(0f, imageSize.width)
        val imageY = ((canvasPoint.y - imageTop) / scaleFactor).coerceIn(0f, imageSize.height)

        return Offset(imageX, imageY)
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

        return mapCanvasLocalPointToImagePixel(Offset(unscaledX, unscaledY), containerSize, imageSize)
    }

    fun formatZoomPercentage(scale: Float): String {
        return "${(scale * 100).toInt()}%"
    }

    fun resolveInitialPageIndex(targetUriStr: String?, items: List<String>): Int {
        if (targetUriStr.isNullOrBlank()) return 0
        val index = items.indexOf(targetUriStr)
        return if (index != -1) index else 0
    }

    fun evaluateCameraSwipeGesture(
        startX: Float,
        startY: Float,
        totalDx: Float,
        totalDy: Float,
        pointerCount: Int,
        screenWidth: Float,
        screenHeight: Float,
        edgeInsetPx: Float = 60f,
        excludedRects: List<Rect> = emptyList(),
    ): CameraSwipeDecision {
        if (pointerCount != 1) return CameraSwipeDecision.NONE
        if (screenWidth <= 0f || screenHeight <= 0f) return CameraSwipeDecision.NONE

        if (startX < edgeInsetPx || startX > (screenWidth - edgeInsetPx)) return CameraSwipeDecision.NONE

        val startPoint = Offset(startX, startY)
        if (excludedRects.any { rect -> rect.contains(startPoint) }) return CameraSwipeDecision.NONE

        val minSwipeDistance = screenWidth * 0.15f
        val isLeftSwipe = totalDx <= -minSwipeDistance

        val isHorizontalDominant = kotlin.math.abs(totalDx) > (1.5f * kotlin.math.abs(totalDy))

        return if (isLeftSwipe && isHorizontalDominant) {
            CameraSwipeDecision.NAVIGATE_NEXT_TAB
        } else {
            CameraSwipeDecision.NONE
        }
    }
}

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    maxScale: Float = 8.0f,
    isStudio: Boolean = false,
    isDrawToolActive: Boolean = false,
    onScaleChanged: (Float) -> Unit = {},
    resetKey: Any? = null,
    content: @Composable BoxScope.(scale: Float, pan: Offset) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    val currentOnScaleChanged by rememberUpdatedState(onScaleChanged)
    val currentMaxScale by rememberUpdatedState(maxScale)
    val currentIsStudio by rememberUpdatedState(isStudio)
    val currentIsDrawToolActive by rememberUpdatedState(isDrawToolActive)

    LaunchedEffect(resetKey) {
        scale = 1.0f
        pan = Offset.Zero
        currentOnScaleChanged(1.0f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it.toSize() }
            .pointerInput(resetKey, currentIsDrawToolActive) {
                detectTapGestures(
                    onDoubleTap = {
                        if (!currentIsDrawToolActive) {
                            val newScale = ZoomTransformMath.toggleDoubleTapScale(scale)
                            scale = newScale
                            pan = if (newScale == 1.0f) Offset.Zero else pan
                            currentOnScaleChanged(newScale)
                        }
                    },
                )
            }
            .pointerInput(resetKey, currentIsStudio, currentIsDrawToolActive) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointerCount = event.changes.size
                        val mode = ZoomTransformMath.decideTouchMode(
                            pointerCount = pointerCount,
                            scale = scale,
                            isDrawToolActive = currentIsDrawToolActive,
                            isStudio = currentIsStudio,
                        )

                        if (mode == TouchMode.ZOOM_PAN) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val newScale = ZoomTransformMath.calculateScale(scale, zoomChange, maxScale = currentMaxScale)
                            val newPan = ZoomTransformMath.calculateClampedPan(pan, panChange, newScale, containerSize)
                            scale = newScale
                            pan = newPan
                            currentOnScaleChanged(newScale)
                        } else if (mode == TouchMode.PAN) {
                            val panChange = event.calculatePan()
                            val newPan = ZoomTransformMath.calculateClampedPan(pan, panChange, scale, containerSize)
                            pan = newPan
                        }

                        if (event.changes.all { !it.pressed }) {
                            break
                        }
                    }
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

        // Live Zoom Percentage Badge & "Fit" Reset Chip Overlay (Top Right)
        AnimatedVisibility(
            visible = scale > 1.02f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Zoom Percentage Badge
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

                // "Fit" Reset Chip Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RoyalBlue)
                        .clickable {
                            scale = 1.0f
                            pan = Offset.Zero
                            onScaleChanged(1.0f)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Fit",
                        color = ColorWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
