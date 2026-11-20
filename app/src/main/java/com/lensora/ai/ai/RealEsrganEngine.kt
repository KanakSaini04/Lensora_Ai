package com.lensora.ai.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

object RealEsrganEngine {
    private const val TAG = "RealEsrganEngine"
    const val TILE_IN_SIZE = 128
    const val TILE_OUT_SIZE = 512
    const val SCALE_FACTOR = 4

    fun bitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val buffer = ByteBuffer.allocateDirect(1 * width * height * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (p in pixels) {
            val r = Color.red(p) / 255.0f
            val g = Color.green(p) / 255.0f
            val b = Color.blue(p) / 255.0f
            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }
        buffer.rewind()
        return buffer
    }

    fun bufferToBitmap(buffer: ByteBuffer, width: Int = TILE_OUT_SIZE, height: Int = TILE_OUT_SIZE): Bitmap {
        buffer.rewind()
        val pixels = IntArray(width * height)
        val numPixels = width * height

        for (i in 0 until numPixels) {
            val r = (buffer.getFloat() * 255.0f).coerceIn(0.0f, 255.0f).toInt()
            val g = (buffer.getFloat() * 255.0f).coerceIn(0.0f, 255.0f).toInt()
            val b = (buffer.getFloat() * 255.0f).coerceIn(0.0f, 255.0f).toInt()
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outBitmap
    }

    fun processTileSuperResolution(
        interpreter: Interpreter,
        inTile: Bitmap,
        validator: Validator,
    ): Bitmap {
        // Prepare 128x128 input tile
        val scaledInTile = if (inTile.width != TILE_IN_SIZE || inTile.height != TILE_IN_SIZE) {
            Bitmap.createScaledBitmap(inTile, TILE_IN_SIZE, TILE_IN_SIZE, true)
        } else {
            inTile
        }

        val inputBuffer = bitmapToBuffer(scaledInTile)
        if (scaledInTile != inTile) {
            scaledInTile.recycle()
        }

        val outputBuffer = ByteBuffer.allocateDirect(1 * TILE_OUT_SIZE * TILE_OUT_SIZE * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        interpreter.run(inputBuffer, outputBuffer)
        val outTile = bufferToBitmap(outputBuffer, TILE_OUT_SIZE, TILE_OUT_SIZE)

        // L4 Tile Quality Validation check
        if (!validator.isValid(outTile)) {
            Log.w(TAG, "L4 Validator failed on tile output. Falling back to bicubic scale tile.")
            outTile.recycle()
            return Bitmap.createScaledBitmap(inTile, TILE_OUT_SIZE, TILE_OUT_SIZE, true)
        }

        return outTile
    }

    fun runSuperResolutionPipeline(
        sourceBitmap: Bitmap,
        downscaleFactor: Float,
        interpreter: Interpreter,
        validator: Validator,
        onProgress: ((Int) -> Unit)? = null,
    ): Bitmap {
        val scaledInWidth = (sourceBitmap.width * downscaleFactor).toInt().coerceAtLeast(1)
        val scaledInHeight = (sourceBitmap.height * downscaleFactor).toInt().coerceAtLeast(1)

        val scaledSource = if (downscaleFactor != 1.0f) {
            Bitmap.createScaledBitmap(sourceBitmap, scaledInWidth, scaledInHeight, true)
        } else {
            sourceBitmap
        }

        val outWidth = scaledInWidth * SCALE_FACTOR
        val outHeight = scaledInHeight * SCALE_FACTOR

        // Allocate SINGLE output bitmap directly - memory safe (no second full-size copy)
        val outBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val numCols = (scaledInWidth + TILE_IN_SIZE - 1) / TILE_IN_SIZE
        val numRows = (scaledInHeight + TILE_IN_SIZE - 1) / TILE_IN_SIZE
        val totalTiles = numCols * numRows

        var tileIndex = 0

        try {
            for (row in 0 until numRows) {
                for (col in 0 until numCols) {
                    val left = col * TILE_IN_SIZE
                    val top = row * TILE_IN_SIZE
                    val right = (left + TILE_IN_SIZE).coerceAtMost(scaledInWidth)
                    val bottom = (top + TILE_IN_SIZE).coerceAtMost(scaledInHeight)

                    val tileW = right - left
                    val tileH = bottom - top

                    val inTile = Bitmap.createBitmap(scaledSource, left, top, tileW, tileH)
                    val outTile = processTileSuperResolution(interpreter, inTile, validator)

                    val destLeft = (left * SCALE_FACTOR).toFloat()
                    val destTop = (top * SCALE_FACTOR).toFloat()

                    canvas.drawBitmap(outTile, destLeft, destTop, paint)

                    outTile.recycle()
                    inTile.recycle()

                    tileIndex++
                    val progressPercent = (tileIndex * 100) / totalTiles
                    onProgress?.invoke(progressPercent)
                }
            }
        } finally {
            if (scaledSource != sourceBitmap) {
                scaledSource.recycle()
            }
        }

        Log.i(TAG, "Completed Real-ESRGAN super-resolution pass: ${sourceBitmap.width}x${sourceBitmap.height} -> ${outWidth}x${outHeight} (${SCALE_FACTOR}x scale)")
        return outBitmap
    }
}
