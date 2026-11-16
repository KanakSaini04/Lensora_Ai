package com.lensora.ai.ui.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.lensora.ai.ai.ModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ObjectEraser(
    private val context: Context,
) {
    private val tag = "ObjectEraser"
    private val modelManager = ModelManager(context)

    suspend fun eraseObject(
        sourceBitmap: Bitmap,
        maskBitmap: Bitmap,
    ): Bitmap = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val imgWidth = sourceBitmap.width
        val imgHeight = sourceBitmap.height
        Log.i(tag, "Starting LaMa-Dilated AI Inpainting pass on ${imgWidth}x${imgHeight} image...")

        val lamaModelFile = modelManager.getLamaModelFile()

        if (lamaModelFile == null || !lamaModelFile.exists()) {
            Log.w(tag, "lama_dilated.tflite model file missing. Fallback to Content-Aware Inpainting algorithm.")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Please download the LaMa model in Settings", Toast.LENGTH_SHORT).show()
            }
            return@withContext runFastContentAwareInpainting(sourceBitmap, maskBitmap)
        }

        var gpuDelegateObj: AutoCloseable? = null
        var interpreter: Interpreter? = null
        var usedGpu = false

        try {
            val options = Interpreter.Options()
            try {
                val gpuClass = Class.forName("org.tensorflow.lite.gpu.GpuDelegate")
                val delegateInstance = gpuClass.getConstructor().newInstance()
                val delegateClass = Class.forName("org.tensorflow.lite.Delegate")
                val addDelegateMethod = Interpreter.Options::class.java.getMethod("addDelegate", delegateClass)
                addDelegateMethod.invoke(options, delegateInstance)
                if (delegateInstance is AutoCloseable) {
                    gpuDelegateObj = delegateInstance
                }
                usedGpu = true
                Log.i(tag, "GpuDelegate initialized for LaMa Inpainting model ${lamaModelFile.name}")
            } catch (e: Throwable) {
                Log.w(tag, "GpuDelegate init failed (${e.localizedMessage}). Safe fallback to CPU XNNPACK 4-threads.", e)
                options.setNumThreads(4)
            }

            interpreter = Interpreter(lamaModelFile, options)
            Log.i(tag, "Loaded LaMa-Dilated TFLite Model: ${lamaModelFile.name} (GPU: $usedGpu)")

            val resultBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(resultBitmap)

            val tileSize = 512
            val numCols = (imgWidth + tileSize - 1) / tileSize
            val numRows = (imgHeight + tileSize - 1) / tileSize

            val maskPixels = IntArray(maskBitmap.width * maskBitmap.height)
            maskBitmap.getPixels(maskPixels, 0, maskBitmap.width, 0, 0, maskBitmap.width, maskBitmap.height)
            val scaleX = maskBitmap.width.toFloat() / imgWidth.toFloat()
            val scaleY = maskBitmap.height.toFloat() / imgHeight.toFloat()

            for (row in 0 until numRows) {
                for (col in 0 until numCols) {
                    val left = col * tileSize
                    val top = row * tileSize
                    val right = (left + tileSize).coerceAtMost(imgWidth)
                    val bottom = (top + tileSize).coerceAtMost(imgHeight)

                    // Check if mask intersects this tile
                    val maskLeft = (left * scaleX).toInt().coerceIn(0, maskBitmap.width - 1)
                    val maskTop = (top * scaleY).toInt().coerceIn(0, maskBitmap.height - 1)
                    val maskRight = (right * scaleX).toInt().coerceIn(0, maskBitmap.width - 1)
                    val maskBottom = (bottom * scaleY).toInt().coerceIn(0, maskBitmap.height - 1)

                    var tileHasMask = false
                    for (my in maskTop..maskBottom step 8) {
                        for (mx in maskLeft..maskRight step 8) {
                            if (Color.alpha(maskPixels[my * maskBitmap.width + mx]) > 30) {
                                tileHasMask = true
                                break
                            }
                        }
                        if (tileHasMask) break
                    }

                    if (tileHasMask) {
                        try {
                            val tileWidth = right - left
                            val tileHeight = bottom - top
                            val rawTile = Bitmap.createBitmap(sourceBitmap, left, top, tileWidth, tileHeight)

                            val inpaintedTile = runLaMaTiledInference(interpreter, rawTile)
                            if (inpaintedTile != null) {
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                                canvas.drawBitmap(inpaintedTile, left.toFloat(), top.toFloat(), paint)
                                inpaintedTile.recycle() // Memory Shield: Recycle tile
                            }
                            rawTile.recycle()
                        } catch (tileEx: Exception) {
                            Log.e(tag, "Error inpainting tile ($left,$top). Preserving original pixels.", tileEx)
                        }
                    }
                }
            }

            val executionTime = System.currentTimeMillis() - startTime
            Log.i(tag, "LaMa-Dilated GPU Inpainting pass completed in ${executionTime}ms")
            resultBitmap
        } catch (e: Exception) {
            Log.e(tag, "LaMa TFLite inpainting pass failed (${e.localizedMessage}). Fallback to content-aware algorithm.", e)
            runFastContentAwareInpainting(sourceBitmap, maskBitmap)
        } finally {
            try {
                interpreter?.close()
                gpuDelegateObj?.close()
                modelManager.close()
                Log.i(tag, "LaMa Interpreter & GPU resources closed safely")
            } catch (cleanupEx: Exception) {
                Log.w(tag, "Error closing LaMa resources", cleanupEx)
            }
        }
    }

    private fun runLaMaTiledInference(interpreter: Interpreter, tileBitmap: Bitmap): Bitmap? {
        return try {
            val width = tileBitmap.width
            val height = tileBitmap.height

            // Create 4-channel input ByteBuffer (RGB Image + 1-channel Mask) -> 1 x H x W x 4
            val inputBuffer = ByteBuffer.allocateDirect(1 * height * width * 4 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            val pixels = IntArray(width * height)
            tileBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            for (pixel in pixels) {
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                val maskVal = if (Color.alpha(pixel) < 50) 0.0f else 1.0f

                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
                inputBuffer.putFloat(maskVal)
            }

            val outputBuffer = ByteBuffer.allocateDirect(1 * height * width * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            interpreter.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()

            val enhancedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val outPixels = IntArray(width * height)

            for (i in 0 until (width * height)) {
                if (outputBuffer.hasRemaining()) {
                    val r = (outputBuffer.float.coerceIn(0f, 1f) * 255f).toInt()
                    val g = (outputBuffer.float.coerceIn(0f, 1f) * 255f).toInt()
                    val b = (outputBuffer.float.coerceIn(0f, 1f) * 255f).toInt()
                    outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }

            enhancedBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
            enhancedBitmap
        } catch (e: Exception) {
            Log.w(tag, "LaMa tile inference failed (${e.localizedMessage})", e)
            null
        }
    }

    private fun runFastContentAwareInpainting(sourceBitmap: Bitmap, maskBitmap: Bitmap): Bitmap {
        val resultBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        val maskWidth = maskBitmap.width
        val maskHeight = maskBitmap.height
        val maskPixels = IntArray(maskWidth * maskHeight)
        maskBitmap.getPixels(maskPixels, 0, maskWidth, 0, 0, maskWidth, maskHeight)

        var minX = maskWidth
        var minY = maskHeight
        var maxX = -1
        var maxY = -1

        for (y in 0 until maskHeight) {
            for (x in 0 until maskWidth) {
                val pixel = maskPixels[y * maskWidth + x]
                if (Color.alpha(pixel) > 30) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX >= minX && maxY >= minY) {
            val scaleX = sourceBitmap.width.toFloat() / maskWidth.toFloat()
            val scaleY = sourceBitmap.height.toFloat() / maskHeight.toFloat()

            val srcMinX = (minX * scaleX).toInt().coerceIn(0, sourceBitmap.width - 1)
            val srcMaxX = (maxX * scaleX).toInt().coerceIn(0, sourceBitmap.width - 1)
            val srcMinY = (minY * scaleY).toInt().coerceIn(0, sourceBitmap.height - 1)
            val srcMaxY = (maxY * scaleY).toInt().coerceIn(0, sourceBitmap.height - 1)

            val borderSampleX = (srcMinX - 10).coerceIn(0, sourceBitmap.width - 1)
            val borderSampleY = (srcMinY - 10).coerceIn(0, sourceBitmap.height - 1)
            val samplePixel = sourceBitmap.getPixel(borderSampleX, borderSampleY)

            val paint = Paint().apply {
                color = samplePixel
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val eraseRect = Rect(srcMinX, srcMinY, srcMaxX, srcMaxY)
            canvas.drawRect(eraseRect, paint)

            val blendPaint = Paint().apply {
                color = Color.argb(40, Color.red(samplePixel), Color.green(samplePixel), Color.blue(samplePixel))
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(
                (srcMinX + srcMaxX) / 2f,
                (srcMinY + srcMaxY) / 2f,
                (srcMaxX - srcMinX).coerceAtLeast(srcMaxY - srcMinY) / 1.8f,
                blendPaint,
            )
        }
        return resultBitmap
    }
}
