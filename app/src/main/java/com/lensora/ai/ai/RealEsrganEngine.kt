package com.lensora.ai.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class TileGrid(val numCols: Int, val numRows: Int, val totalTiles: Int)
data class TileBounds(val left: Int, val top: Int, val right: Int, val bottom: Int, val width: Int, val height: Int)
data class TileCropDimensions(val cropWidth: Int, val cropHeight: Int)
data class DestCoordinates(val destLeft: Float, val destTop: Float)
data class PaddedSize(val width: Int, val height: Int)

data class EsrganInterpreter(
    val interpreter: Interpreter,
    val gpuDelegate: GpuDelegate?,
    val usingGpu: Boolean,
) {
    fun close() {
        try { interpreter.close() } catch (_: Throwable) {}
        try { gpuDelegate?.close() } catch (_: Throwable) {}
    }
}

object RealEsrganEngine {
    private const val TAG = "RealEsrganEngine"
    const val TILE_IN_SIZE = 128
    const val TILE_OUT_SIZE = 512
    const val SCALE_FACTOR = 4

    const val TILE_PAD = 8
    const val TILE_CORE = TILE_IN_SIZE - 2 * TILE_PAD   // 112
    const val CROP_OFFSET = TILE_PAD * SCALE_FACTOR      // 32

    private fun validateGpuOutput(gpuOutput: ByteBuffer, cpuOutput: ByteBuffer): Boolean {
        gpuOutput.rewind()
        cpuOutput.rewind()
        val numFloats = TILE_OUT_SIZE * TILE_OUT_SIZE * 3
        val gpuBuf = gpuOutput.asFloatBuffer()
        val cpuBuf = cpuOutput.asFloatBuffer()

        var sumAbsDiff = 0.0

        for (i in 0 until numFloats) {
            val g = gpuBuf.get(i)
            val c = cpuBuf.get(i)

            if (g.isNaN() || g.isInfinite()) {
                Log.w(TAG, "GPU output validation fail: NaN or Infinite value detected at index $i")
                return false
            }
            if (g < 0.0f || g > 1.0f) {
                Log.w(TAG, "GPU output validation fail: Out of bounds value $g at index $i")
                return false
            }

            sumAbsDiff += Math.abs((g - c).toDouble())
        }

        val meanAbsDiff = sumAbsDiff / numFloats
        if (meanAbsDiff > 0.02) {
            Log.w(TAG, "GPU output validation fail: Mean abs diff $meanAbsDiff > 0.02 vs CPU")
            return false
        }

        return true
    }

    fun createInterpreter(modelFile: File): EsrganInterpreter {
        val testInput = createInputBuffer()
        val numPixels = TILE_IN_SIZE * TILE_IN_SIZE
        for (i in 0 until numPixels) {
            val r = (i % 256) / 255.0f
            val g = ((i * 2) % 256) / 255.0f
            val b = ((i * 3) % 256) / 255.0f
            testInput.putFloat(r)
            testInput.putFloat(g)
            testInput.putFloat(b)
        }
        testInput.rewind()

        val cpuOutput = createOutputBuffer()
        val cpuOptions = Interpreter.Options().apply { setNumThreads(4) }
        val cpuInterpreter = Interpreter(modelFile, cpuOptions)

        testInput.rewind()
        cpuInterpreter.run(testInput, cpuOutput)

        val compat = CompatibilityList()
        var isGpuSupported = false
        try {
            isGpuSupported = compat.isDelegateSupportedOnThisDevice
        } catch (_: Throwable) {
        } finally {
            try { compat.close() } catch (_: Throwable) {}
        }

        if (isGpuSupported) {
            var delegate: GpuDelegate? = null
            var gpuInterpreter: Interpreter? = null
            try {
                delegate = GpuDelegate()
                val options = Interpreter.Options().addDelegate(delegate)
                gpuInterpreter = Interpreter(modelFile, options)
                val gpuOutput = createOutputBuffer()
                testInput.rewind()
                gpuInterpreter.run(testInput, gpuOutput)

                if (validateGpuOutput(gpuOutput, cpuOutput)) {
                    cpuInterpreter.close()
                    Log.i(TAG, "Real-ESRGAN using GPU delegate")
                    return EsrganInterpreter(gpuInterpreter, delegate, true)
                } else {
                    Log.w(TAG, "GPU delegate output validation failed. Falling back to CPU.")
                    try { gpuInterpreter.close() } catch (_: Throwable) {}
                    try { delegate.close() } catch (_: Throwable) {}
                }
            } catch (t: Throwable) {
                Log.w(TAG, "GPU delegate failed, falling back to CPU: ${t.message}")
                try { gpuInterpreter?.close() } catch (_: Throwable) {}
                try { delegate?.close() } catch (_: Throwable) {}
            }
        }

        Log.i(TAG, "Real-ESRGAN using CPU (4 threads)")
        return EsrganInterpreter(cpuInterpreter, null, false)
    }

    fun calculatePaddedSize(grid: TileGrid): PaddedSize =
        PaddedSize(grid.numCols * TILE_CORE + 2 * TILE_PAD, grid.numRows * TILE_CORE + 2 * TILE_PAD)

    fun calculateTileGrid(scaledInWidth: Int, scaledInHeight: Int): TileGrid {
        val numCols = (scaledInWidth + TILE_CORE - 1) / TILE_CORE
        val numRows = (scaledInHeight + TILE_CORE - 1) / TILE_CORE
        return TileGrid(numCols, numRows, numCols * numRows)
    }

    fun calculateTileBounds(col: Int, row: Int, scaledInWidth: Int, scaledInHeight: Int): TileBounds {
        val left = col * TILE_CORE
        val top = row * TILE_CORE
        val right = (left + TILE_CORE).coerceAtMost(scaledInWidth)
        val bottom = (top + TILE_CORE).coerceAtMost(scaledInHeight)
        return TileBounds(left, top, right, bottom, right - left, bottom - top)
    }

    fun calculateOutputCropDimensions(tileWidth: Int, tileHeight: Int): TileCropDimensions {
        return TileCropDimensions(tileWidth * SCALE_FACTOR, tileHeight * SCALE_FACTOR)
    }

    fun calculateDestCoordinates(col: Int, row: Int): DestCoordinates {
        val left = col * TILE_CORE
        val top = row * TILE_CORE
        return DestCoordinates((left * SCALE_FACTOR).toFloat(), (top * SCALE_FACTOR).toFloat())
    }

    fun buildEdgePaddedSource(src: Bitmap, grid: TileGrid): Bitmap {
        val size = calculatePaddedSize(grid)
        val out = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint()
        val w = src.width
        val h = src.height
        val right = TILE_PAD + w
        val bottom = TILE_PAD + h
        c.drawBitmap(src, TILE_PAD.toFloat(), TILE_PAD.toFloat(), p)
        c.drawBitmap(src, Rect(0, 0, 1, h), Rect(0, TILE_PAD, TILE_PAD, bottom), p)
        c.drawBitmap(src, Rect(w - 1, 0, w, h), Rect(right, TILE_PAD, size.width, bottom), p)
        c.drawBitmap(src, Rect(0, 0, w, 1), Rect(TILE_PAD, 0, right, TILE_PAD), p)
        c.drawBitmap(src, Rect(0, h - 1, w, h), Rect(TILE_PAD, bottom, right, size.height), p)
        c.drawBitmap(src, Rect(0, 0, 1, 1), Rect(0, 0, TILE_PAD, TILE_PAD), p)
        c.drawBitmap(src, Rect(w - 1, 0, w, 1), Rect(right, 0, size.width, TILE_PAD), p)
        c.drawBitmap(src, Rect(0, h - 1, 1, h), Rect(0, bottom, TILE_PAD, size.height), p)
        c.drawBitmap(src, Rect(w - 1, h - 1, w, h), Rect(right, bottom, size.width, size.height), p)
        return out
    }

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
     * Pads edge tiles smaller than 128x128 by edge pixel replication to prevent stretching distortion.
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

        val cropDims = calculateOutputCropDimensions(tileW, tileH)
        val expectedOutW = cropDims.cropWidth
        val expectedOutH = cropDims.cropHeight

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

    fun processTileWithContext(
        interpreter: Interpreter,
        window: Bitmap,
        coreWidth: Int,
        coreHeight: Int,
        validator: Validator,
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer,
    ): Bitmap {
        fillBufferFromBitmap(window, inputBuffer)
        outputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)
        val raw = bufferToBitmap(outputBuffer, TILE_OUT_SIZE, TILE_OUT_SIZE)
        val dims = calculateOutputCropDimensions(coreWidth, coreHeight)
        val cropped = Bitmap.createBitmap(raw, CROP_OFFSET, CROP_OFFSET, dims.cropWidth, dims.cropHeight)
        if (cropped != raw) raw.recycle()
        if (!validator.isValid(cropped)) {
            Log.w(TAG, "L4 Validator failed on tile output. Falling back to bicubic scale tile.")
            cropped.recycle()
            val core = Bitmap.createBitmap(window, TILE_PAD, TILE_PAD, coreWidth, coreHeight)
            val scaled = Bitmap.createScaledBitmap(core, dims.cropWidth, dims.cropHeight, true)
            if (core != window) core.recycle()
            return scaled
        }
        return cropped
    }

    fun runSuperResolutionPipeline(
        sourceBitmap: Bitmap,
        downscaleFactor: Float,
        interpreter: Interpreter,
        validator: Validator,
        colorMatrix: ColorMatrix? = null,
        modelFile: File? = null,
        onProgress: ((Int) -> Unit)? = null,
    ): Bitmap {
        val pipelineStartMs = SystemClock.elapsedRealtime()
        val scaledInWidth = (sourceBitmap.width * downscaleFactor).toInt().coerceAtLeast(1)
        val scaledInHeight = (sourceBitmap.height * downscaleFactor).toInt().coerceAtLeast(1)

        val scaledSource = if (downscaleFactor != 1.0f) {
            Bitmap.createScaledBitmap(sourceBitmap, scaledInWidth, scaledInHeight, true)
        } else {
            sourceBitmap
        }

        val outWidth = scaledInWidth * SCALE_FACTOR
        val outHeight = scaledInHeight * SCALE_FACTOR
        val grid = calculateTileGrid(scaledInWidth, scaledInHeight)

        // Reuse ByteBuffers across all tiles
        val inputBuffer = createInputBuffer()
        val outputBuffer = createOutputBuffer()

        var outBitmap: Bitmap? = null
        var padded: Bitmap? = null

        var totalFillMs = 0L
        var totalInferenceMs = 0L
        var totalConvertMs = 0L
        var totalDrawMs = 0L

        var activeInterpreter = interpreter

        try {
            padded = buildEdgePaddedSource(scaledSource, grid)
            outBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(outBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                if (colorMatrix != null) {
                    colorFilter = ColorMatrixColorFilter(colorMatrix)
                }
                isFilterBitmap = true
            }

            var tileIndex = 0

            for (row in 0 until grid.numRows) {
                for (col in 0 until grid.numCols) {
                    val fillStart = SystemClock.elapsedRealtime()
                    val core = calculateTileBounds(col, row, scaledInWidth, scaledInHeight)
                    val window = Bitmap.createBitmap(padded, col * TILE_CORE, row * TILE_CORE, TILE_IN_SIZE, TILE_IN_SIZE)
                    fillBufferFromBitmap(window, inputBuffer)
                    outputBuffer.rewind()
                    val fillTime = SystemClock.elapsedRealtime() - fillStart
                    totalFillMs += fillTime

                    val infStart = SystemClock.elapsedRealtime()
                    try {
                        activeInterpreter.run(inputBuffer, outputBuffer)
                    } catch (t: Throwable) {
                        Log.w(TAG, "Tile $tileIndex inference failed on active interpreter (${t.message}). Falling back to CPU 4-thread interpreter.")
                        if (modelFile != null && modelFile.exists()) {
                            val cpuOptions = Interpreter.Options().apply { setNumThreads(4) }
                            activeInterpreter = Interpreter(modelFile, cpuOptions)
                            activeInterpreter.run(inputBuffer, outputBuffer)
                        } else {
                            throw t
                        }
                    }
                    val infTime = SystemClock.elapsedRealtime() - infStart
                    totalInferenceMs += infTime

                    val convStart = SystemClock.elapsedRealtime()
                    val raw = bufferToBitmap(outputBuffer, TILE_OUT_SIZE, TILE_OUT_SIZE)
                    val dims = calculateOutputCropDimensions(core.width, core.height)
                    var outTile = Bitmap.createBitmap(raw, CROP_OFFSET, CROP_OFFSET, dims.cropWidth, dims.cropHeight)
                    if (outTile != raw) raw.recycle()
                    if (!validator.isValid(outTile)) {
                        Log.w(TAG, "L4 Validator failed on tile output. Falling back to bicubic scale tile.")
                        outTile.recycle()
                        val coreBitmap = Bitmap.createBitmap(window, TILE_PAD, TILE_PAD, core.width, core.height)
                        outTile = Bitmap.createScaledBitmap(coreBitmap, dims.cropWidth, dims.cropHeight, true)
                        if (coreBitmap != window) coreBitmap.recycle()
                    }
                    val convTime = SystemClock.elapsedRealtime() - convStart
                    totalConvertMs += convTime

                    val drawStart = SystemClock.elapsedRealtime()
                    val destCoords = calculateDestCoordinates(col, row)
                    canvas.drawBitmap(outTile, destCoords.destLeft, destCoords.destTop, paint)

                    outTile.recycle()
                    window.recycle()
                    val drawTime = SystemClock.elapsedRealtime() - drawStart
                    totalDrawMs += drawTime

                    tileIndex++
                    if (tileIndex > 0 && (tileIndex % 20 == 0 || tileIndex == grid.totalTiles)) {
                        val avgFill = totalFillMs / tileIndex
                        val avgInf = totalInferenceMs / tileIndex
                        val avgConv = totalConvertMs / tileIndex
                        val avgDraw = totalDrawMs / tileIndex
                        val avgTotal = (SystemClock.elapsedRealtime() - pipelineStartMs) / tileIndex
                        Log.i(TAG, "Tile $tileIndex/${grid.totalTiles} [fill: ${avgFill}ms, inf: ${avgInf}ms, convert: ${avgConv}ms, draw: ${avgDraw}ms, avgTotal: ${avgTotal}ms]")
                    }

                    val progressPercent = (tileIndex * 100) / grid.totalTiles
                    onProgress?.invoke(progressPercent)
                }
            }

            val netUpscaleRatio = (outWidth.toFloat() / sourceBitmap.width.toFloat())
            Log.i(TAG, "Completed Real-ESRGAN super-resolution pass: ${sourceBitmap.width}x${sourceBitmap.height} -> ${outWidth}x${outHeight} (${netUpscaleRatio}x net scale vs source)")
            return outBitmap
        } catch (e: Throwable) {
            outBitmap?.recycle()
            throw e
        } finally {
            padded?.recycle()
            if (scaledSource != sourceBitmap) {
                scaledSource.recycle()
            }
        }
    }
}
