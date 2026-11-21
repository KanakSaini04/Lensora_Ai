package com.lensora.ai.ui.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
            Log.e(tag, "LaMa TFLite model file missing. Eraser pass aborted.")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "LaMa Model missing! Please download model in Settings.", Toast.LENGTH_LONG).show()
            }
            return@withContext sourceBitmap
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

            // Contract Validation at Load Time (Input 0: [1,512,512,3] image, Input 1: [1,512,512,1] mask, Output 0: [1,512,512,3])
            val isContractValid = validateTensorContract(interpreter)
            if (!isContractValid) {
                Log.e(tag, "Tensor contract validation failed for LaMa model ${lamaModelFile.name}. Aborting inpainting pass safely.")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "LaMa Model contract mismatch! Model expects two separate inputs: image [1,512,512,3] and mask [1,512,512,1].", Toast.LENGTH_LONG).show()
                }
                return@withContext sourceBitmap
            }

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
                            val rawMaskTile = Bitmap.createBitmap(maskBitmap, maskLeft, maskTop, (maskRight - maskLeft + 1).coerceAtLeast(1), (maskBottom - maskTop + 1).coerceAtLeast(1))

                            val inpaintedTile = runLaMaTiledInferenceTwoInput(interpreter, rawTile, rawMaskTile)
                            rawMaskTile.recycle()

                            if (inpaintedTile != null) {
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                                canvas.drawBitmap(inpaintedTile, left.toFloat(), top.toFloat(), paint)
                                inpaintedTile.recycle()
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
            Log.e(tag, "LaMa TFLite inpainting pass failed (${e.localizedMessage})", e)
            sourceBitmap
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

    fun validateTensorContract(interpreter: Interpreter): Boolean {
        return try {
            if (interpreter.inputTensorCount < 2) {
                Log.e(tag, "Tensor contract fail: Expected 2 inputs, got ${interpreter.inputTensorCount}")
                return false
            }

            val imageTensor = interpreter.getInputTensor(0)
            val maskTensor = interpreter.getInputTensor(1)
            val outputTensor = interpreter.getOutputTensor(0)

            val imageShape = imageTensor.shape()
            val maskShape = maskTensor.shape()
            val outputShape = outputTensor.shape()

            Log.i(tag, "LaMa TFLite Contract - Image Input [0]: ${imageShape.contentToString()}, Mask Input [1]: ${maskShape.contentToString()}, Output [0]: ${outputShape.contentToString()}")

            val isImageValid = imageShape.size == 4 && imageShape[0] == 1 && imageShape[1] == 512 && imageShape[2] == 512 && imageShape[3] == 3
            val isMaskValid = maskShape.size == 4 && maskShape[0] == 1 && maskShape[1] == 512 && maskShape[2] == 512 && maskShape[3] == 1
            val isOutputValid = outputShape.size == 4 && outputShape[0] == 1 && outputShape[1] == 512 && outputShape[2] == 512 && outputShape[3] == 3

            if (!isImageValid || !isMaskValid || !isOutputValid) {
                Log.e(tag, "Tensor contract fail: Shapes invalid! Image: ${imageShape.contentToString()} (expected [1,512,512,3]), Mask: ${maskShape.contentToString()} (expected [1,512,512,1]), Output: ${outputShape.contentToString()} (expected [1,512,512,3])")
                return false
            }

            true
        } catch (e: Exception) {
            Log.e(tag, "Exception during tensor contract validation", e)
            false
        }
    }

    private fun runLaMaTiledInferenceTwoInput(
        interpreter: Interpreter,
        tileBitmap: Bitmap,
        maskTileBitmap: Bitmap,
    ): Bitmap? {
        return try {
            val targetSize = 512

            // Prepare 512x512 scaled tile bitmaps for inference if needed
            val scaledTile = if (tileBitmap.width == targetSize && tileBitmap.height == targetSize) {
                tileBitmap
            } else {
                Bitmap.createScaledBitmap(tileBitmap, targetSize, targetSize, true)
            }

            val scaledMask = if (maskTileBitmap.width == targetSize && maskTileBitmap.height == targetSize) {
                maskTileBitmap
            } else {
                Bitmap.createScaledBitmap(maskTileBitmap, targetSize, targetSize, true)
            }

            // 1. Separate Input 0 "image" Buffer: [1, 512, 512, 3] FLOAT32 NHWC
            val imageBuffer = ByteBuffer.allocateDirect(1 * targetSize * targetSize * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            // 2. Separate Input 1 "mask" Buffer: [1, 512, 512, 1] FLOAT32 NHWC
            val maskBuffer = ByteBuffer.allocateDirect(1 * targetSize * targetSize * 1 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            val imgPixels = IntArray(targetSize * targetSize)
            scaledTile.getPixels(imgPixels, 0, targetSize, 0, 0, targetSize, targetSize)

            val maskPixels = IntArray(targetSize * targetSize)
            scaledMask.getPixels(maskPixels, 0, targetSize, 0, 0, targetSize, targetSize)

            var unmaskedCount = 0
            var totalUnmaskedDiffSum = 0.0

            for (i in 0 until (targetSize * targetSize)) {
                val pixel = imgPixels[i]
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                imageBuffer.putFloat(r)
                imageBuffer.putFloat(g)
                imageBuffer.putFloat(b)

                val maskPixel = maskPixels[i]
                val isMasked = Color.alpha(maskPixel) > 30 || ((maskPixel shr 16) and 0xFF) > 128
                val maskVal = if (isMasked) 1.0f else 0.0f
                maskBuffer.putFloat(maskVal)
            }

            imageBuffer.rewind()
            maskBuffer.rewind()

            // 3. Output 0 "painted_image" Buffer: [1, 512, 512, 3] FLOAT32 NHWC
            val outputBuffer = ByteBuffer.allocateDirect(1 * targetSize * targetSize * 3 * 4).apply {
                order(ByteOrder.nativeOrder())
            }

            val inputs = arrayOf<Any>(imageBuffer, maskBuffer)
            val outputs = mapOf<Int, Any>(0 to outputBuffer)

            interpreter.runForMultipleInputsOutputs(inputs, outputs)
            outputBuffer.rewind()

            val outPixels = IntArray(targetSize * targetSize)
            var hasNanOrInf = false

            for (i in 0 until (targetSize * targetSize)) {
                if (outputBuffer.hasRemaining()) {
                    val outR = outputBuffer.float
                    val outG = outputBuffer.float
                    val outB = outputBuffer.float

                    if (outR.isNaN() || outR.isInfinite() || outG.isNaN() || outG.isInfinite() || outB.isNaN() || outB.isInfinite()) {
                        hasNanOrInf = true
                    }

                    val clampedR = (outR.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                    val clampedG = (outG.coerceIn(0.0f, 1.0f) * 255.0f).toInt()
                    val clampedB = (outB.coerceIn(0.0f, 1.0f) * 255.0f).toInt()

                    outPixels[i] = (0xFF shl 24) or (clampedR shl 16) or (clampedG shl 8) or clampedB

                    // Mean absolute difference calculation on unmasked region
                    val maskVal = if (Color.alpha(maskPixels[i]) > 30) 1.0f else 0.0f
                    if (maskVal == 0.0f) {
                        val inR = ((imgPixels[i] shr 16) and 0xFF) / 255.0f
                        val inG = ((imgPixels[i] shr 8) and 0xFF) / 255.0f
                        val inB = (imgPixels[i] and 0xFF) / 255.0f

                        val diff = (Math.abs(inR - outR) + Math.abs(inG - outG) + Math.abs(inB - outB)) / 3.0f
                        totalUnmaskedDiffSum += diff
                        unmaskedCount++
                    }
                }
            }

            if (hasNanOrInf) {
                Log.w(tag, "Warning: NaN or Inf value detected in LaMa model output buffer!")
            }

            val meanAbsDiff = if (unmaskedCount > 0) totalUnmaskedDiffSum / unmaskedCount else 0.0
            Log.i(tag, "LaMa tile inference complete: unmasked region mean abs diff = $meanAbsDiff (samples: $unmaskedCount)")

            val result512 = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            result512.setPixels(outPixels, 0, targetSize, 0, 0, targetSize, targetSize)

            val finalTile = if (scaledTile != tileBitmap) {
                scaledTile.recycle()
            } else result512

            if (scaledMask != maskTileBitmap) {
                scaledMask.recycle()
            }

            if (result512 != finalTile && result512 != scaledTile) {
                if (tileBitmap.width != targetSize || tileBitmap.height != targetSize) {
                    val rescaled = Bitmap.createScaledBitmap(result512, tileBitmap.width, tileBitmap.height, true)
                    result512.recycle()
                    rescaled
                } else {
                    result512
                }
            } else {
                result512
            }
        } catch (e: Exception) {
            Log.e(tag, "LaMa two-input tile inference failed (${e.localizedMessage})", e)
            null
        }
    }
}

