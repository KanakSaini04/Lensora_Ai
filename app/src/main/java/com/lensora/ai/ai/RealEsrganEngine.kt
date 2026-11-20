package com.lensora.ai.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

object RealEsrganEngine {
    private const val TAG = "RealEsrganEngine"
    const val TILE_IN_SIZE = 128
    const val TILE_OUT_SIZE = 512
    const val SCALE_FACTOR = 4

    fun createInputBuffer(width: Int = TILE_IN_SIZE, height: Int = TILE_IN_SIZE): ByteBuffer {
        return ByteBuffer.allocateDirect(1 * width * height * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    fun createOutputBuffer(width: Int = TILE_OUT_SIZE, height: Int = TILE_OUT_SIZE): ByteBuffer {
        return ByteBuffer.allocateDirect(1 * width * height * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    fun fillBufferFromBitmap(bitmap: Bitmap, buffer: ByteBuffer) {
        buffer.rewind()
        val width = bitmap.width
        val height = bitmap.height
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
    }

    fun bitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = createInputBuffer(bitmap.width, bitmap.height)
        fillBufferFromBitmap(bitmap, buffer)
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

    /**
     * Pads edge tiles smaller than 128x128 by edge replication to prevent stretching distortion.
     */
    fun padTileTo128(inTile: Bitmap): Bitmap {
        val tileW = inTile.width
        val tileH = inTile.height
        if (tileW == TILE_IN_SIZE && tileH == TILE_IN_SIZE) {
            return inTile
        }

        val padded = Bitmap.createBitmap(TILE_IN_SIZE, TILE_IN_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(padded)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Draw original tile at top-left
        canvas.drawBitmap(inTile, 0f, 0f, paint)

        // Pad right strip if tileW < 128
        if (tileW < TILE_IN_SIZE) {
            val srcRightStrip = Rect(tileW - 1, 0, tileW, tileH)
            val dstRightStrip = Rect(tileW, 0, TILE_IN_SIZE, tileH)
            canvas.drawBitmap(inTile, srcRightStrip, dstRightStrip, paint)
        }

        // Pad bottom strip if tileH < 128
        if (tileH < TILE_IN_SIZE) {
            val srcBottomStrip = Rect(0, tileH - 1, tileW, tileH)
            val dstBottomStrip = Rect(0, tileH, tileW, TILE_IN_SIZE)
            canvas.drawBitmap(inTile, srcBottomStrip, dstBottomStrip, paint)
        }

        // Pad bottom-right corner if both < 128
        if (tileW < TILE_IN_SIZE && tileH < TILE_IN_SIZE) {
            val srcCorner = Rect(tileW - 1, tileH - 1, tileW, tileH)
            val dstCorner = Rect(tileW, tileH, TILE_IN_SIZE, TILE_IN_SIZE)
            canvas.drawBitmap(inTile, srcCorner, dstCorner, paint)
        }

        return padded
    }

    fun processTileSuperResolution(
        interpreter: Interpreter,
        inTile: Bitmap,
        validator: Validator,
        inputBuffer: ByteBuffer? = null,
        outputBuffer: ByteBuffer? = null,
    ): Bitmap {
        val tileW = inTile.width
        val tileH = inTile.height

        val paddedTile = padTileTo128(inTile)

        val inBuf = inputBuffer ?: createInputBuffer()
        val outBuf = outputBuffer ?: createOutputBuffer()

        fillBufferFromBitmap(paddedTile, inBuf)
        outBuf.rewind()

        if (paddedTile != inTile) {
            paddedTile.recycle()
        }

        interpreter.run(inBuf, outBuf)
        val raw512Tile = bufferToBitmap(outBuf, TILE_OUT_SIZE, TILE_OUT_SIZE)

        val expectedOutW = tileW * SCALE_FACTOR
        val expectedOutH = tileH * SCALE_FACTOR

        val croppedTile = if (expectedOutW != TILE_OUT_SIZE || expectedOutH != TILE_OUT_SIZE) {
            val cropped = Bitmap.createBitmap(raw512Tile, 0, 0, expectedOutW, expectedOutH)
            if (cropped != raw512Tile) {
                raw512Tile.recycle()
            }
            cropped
        } else {
            raw512Tile
        }

        // L4 Tile Quality Validation check
        if (!validator.isValid(croppedTile)) {
            Log.w(TAG, "L4 Validator failed on tile output. Falling back to bicubic scale tile.")
            croppedTile.recycle()
            return Bitmap.createScaledBitmap(inTile, expectedOutW, expectedOutH, true)
        }

        return croppedTile
    }

    fun runSuperResolutionPipeline(
        sourceBitmap: Bitmap,
        downscaleFactor: Float,
        interpreter: Interpreter,
        validator: Validator,
        colorMatrix: ColorMatrix? = null,
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

        // Reuse ByteBuffers across all tiles
        val inputBuffer = createInputBuffer()
        val outputBuffer = createOutputBuffer()

        var outBitmap: Bitmap? = null

        try {
            outBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                if (colorMatrix != null) {
                    colorFilter = ColorMatrixColorFilter(colorMatrix)
                }
                isFilterBitmap = true
            }

            val numCols = (scaledInWidth + TILE_IN_SIZE - 1) / TILE_IN_SIZE
            val numRows = (scaledInHeight + TILE_IN_SIZE - 1) / TILE_IN_SIZE
            val totalTiles = numCols * numRows

            var tileIndex = 0

            for (row in 0 until numRows) {
                for (col in 0 until numCols) {
                    val left = col * TILE_IN_SIZE
                    val top = row * TILE_IN_SIZE
                    val right = (left + TILE_IN_SIZE).coerceAtMost(scaledInWidth)
                    val bottom = (top + TILE_IN_SIZE).coerceAtMost(scaledInHeight)

                    val tileW = right - left
                    val tileH = bottom - top

                    val inTile = Bitmap.createBitmap(scaledSource, left, top, tileW, tileH)
                    val outTile = processTileSuperResolution(
                        interpreter = interpreter,
                        inTile = inTile,
                        validator = validator,
                        inputBuffer = inputBuffer,
                        outputBuffer = outputBuffer,
                    )

                    val destLeft = (left * SCALE_FACTOR).toFloat()
                    val destTop = (top * SCALE_FACTOR).toFloat()

                    canvas.drawBitmap(outTile, destLeft, destTop, paint)

                    outTile.recycle()
                    if (inTile != scaledSource) {
                        inTile.recycle()
                    }

                    tileIndex++
                    val progressPercent = (tileIndex * 100) / totalTiles
                    onProgress?.invoke(progressPercent)
                }
            }

            Log.i(TAG, "Completed Real-ESRGAN super-resolution pass: ${sourceBitmap.width}x${sourceBitmap.height} -> ${outWidth}x${outHeight} (${SCALE_FACTOR}x scale)")
            return outBitmap
        } catch (e: Throwable) {
            outBitmap?.recycle()
            throw e
        } finally {
            if (scaledSource != sourceBitmap) {
                scaledSource.recycle()
            }
        }
    }
}
