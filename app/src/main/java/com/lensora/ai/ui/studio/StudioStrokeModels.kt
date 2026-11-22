package com.lensora.ai.ui.studio

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.lensora.ai.ui.components.StrokePaintStyle
import com.lensora.ai.ui.components.ZoomTransformMath

data class StudioStroke(
    val points: List<Offset>,
    val isEraseMode: Boolean = true,
    val radiusCanvasPx: Float = 20f,
)

class StrokeHistory(
    private val maxUndoSteps: Int = 25,
) {
    private val _strokes = mutableListOf<StudioStroke>()
    private val _redoStack = mutableListOf<StudioStroke>()

    val strokes: List<StudioStroke> get() = _strokes.toList()
    val canUndo: Boolean get() = _strokes.isNotEmpty()
    val canRedo: Boolean get() = _redoStack.isNotEmpty()

    fun addStroke(stroke: StudioStroke) {
        if (stroke.points.isEmpty()) return
        _strokes.add(stroke)
        if (_strokes.size > maxUndoSteps) {
            _strokes.removeAt(0)
        }
        _redoStack.clear()
    }

    fun undo(): Boolean {
        if (_strokes.isEmpty()) return false
        val last = _strokes.removeAt(_strokes.size - 1)
        _redoStack.add(last)
        return true
    }

    fun redo(): Boolean {
        if (_redoStack.isEmpty()) return false
        val last = _redoStack.removeAt(_redoStack.size - 1)
        _strokes.add(last)
        return true
    }

    fun clear() {
        _strokes.clear()
        _redoStack.clear()
    }
}

object MaskComposer {
    fun buildMaskBitmap(
        imageWidth: Int,
        imageHeight: Int,
        canvasSize: Size,
        strokes: List<StudioStroke>,
    ): Bitmap {
        val width = imageWidth.coerceAtLeast(1)
        val height = imageHeight.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val imageSize = Size(width.toFloat(), height.toFloat())

        val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val restorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach

            val paint = if (stroke.isEraseMode) erasePaint else restorePaint

            // Map canvas-local points to real source image pixels
            val mappedPoints = stroke.points.map { pt ->
                ZoomTransformMath.mapCanvasLocalPointToImagePixel(pt, canvasSize, imageSize)
            }

            val radiusImagePx = ZoomTransformMath.calculateImageRadiusFromCanvasPx(
                radiusCanvasPx = stroke.radiusCanvasPx,
                canvasSize = canvasSize,
                imageSize = imageSize,
            )

            val paintStyle = ZoomTransformMath.decideStrokePaintStyle(mappedPoints.size)

            if (paintStyle == StrokePaintStyle.STROKE) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = radiusImagePx * 2f

                val path = Path()
                path.moveTo(mappedPoints[0].x, mappedPoints[0].y)
                for (i in 1 until mappedPoints.size) {
                    path.lineTo(mappedPoints[i].x, mappedPoints[i].y)
                }
                canvas.drawPath(path, paint)
            } else if (paintStyle == StrokePaintStyle.FILL) {
                paint.style = Paint.Style.FILL
                val point = mappedPoints[0]
                canvas.drawCircle(point.x, point.y, radiusImagePx, paint)
            }
        }

        return bitmap
    }
}

data class StudioPrimitiveState(
    val selectedToolCategory: Int = 0,
    val exposureValue: Float = 0.0f,
    val contrastValue: Float = 0.0f,
    val brushSizeDp: Float = 30.0f,
    val opacityFloat: Float = 0.8f,
    val isEraseModeActive: Boolean = true,
)

val StudioStateSaver: Saver<StudioPrimitiveState, Any> = Saver(
    save = { state ->
        listOf(
            state.selectedToolCategory,
            state.exposureValue,
            state.contrastValue,
            state.brushSizeDp,
            state.opacityFloat,
            state.isEraseModeActive,
        )
    },
    restore = { list ->
        @Suppress("UNCHECKED_CAST")
        val l = list as List<Any>
        StudioPrimitiveState(
            selectedToolCategory = l[0] as Int,
            exposureValue = l[1] as Float,
            contrastValue = l[2] as Float,
            brushSizeDp = l[3] as Float,
            opacityFloat = l[4] as Float,
            isEraseModeActive = l[5] as Boolean,
        )
    },
)
