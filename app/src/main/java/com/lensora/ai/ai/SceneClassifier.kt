package com.lensora.ai.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.system.measureTimeMillis

enum class LabelSource {
    TFLITE_MODEL,
    HEURISTIC_ENGINE,
}

data class DetectionResult(
    val label: String = "✨ AI: Auto",
    val confidence: Float = 0.85f,
    val boundingBox: RectF? = null,
    val coachingHint: String? = null,
    val source: LabelSource = LabelSource.HEURISTIC_ENGINE,
)

interface TfliteSceneInterpreter {
    fun isUsable(): Boolean
    fun classify(bitmap: Bitmap, labels: List<String>): DetectionResult?
}

object CoachingHintHelper {
    fun coachingHintFor(box: RectF?): String? {
        if (box == null) return null

        val boxWidth = box.right - box.left
        val boxHeight = box.bottom - box.top
        val areaRatio = boxWidth * boxHeight
        val centerX = (box.left + box.right) / 2.0f

        return when {
            areaRatio < 0.20f -> "✨ Step Closer"
            centerX < 0.40f -> "✨ Move Right"
            centerX > 0.60f -> "✨ Move Left"
            else -> "✨ Perfect Composition"
        }
    }
}

object NchwConverter {
    private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    fun bitmapToNchwBuffer(bitmap: Bitmap, width: Int = 224, height: Int = 224): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * 3 * height * width * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val channelStride = width * height
        val rOffset = 0
        val gOffset = channelStride
        val bOffset = channelStride * 2

        val floatArray = FloatArray(3 * channelStride)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = Color.red(p) / 255.0f
            val g = Color.green(p) / 255.0f
            val b = Color.blue(p) / 255.0f

            floatArray[rOffset + i] = (r - MEAN[0]) / STD[0]
            floatArray[gOffset + i] = (g - MEAN[1]) / STD[1]
            floatArray[bOffset + i] = (b - MEAN[2]) / STD[2]
        }

        buffer.asFloatBuffer().put(floatArray)
        buffer.rewind()
        return buffer
    }

    fun softmax(logits: FloatArray): FloatArray {
        if (logits.isEmpty()) return floatArrayOf()
        var maxLogit = logits[0]
        for (v in logits) {
            if (v > maxLogit) maxLogit = v
        }
        val exps = FloatArray(logits.size)
        var sumExp = 0.0f
        for (i in logits.indices) {
            val expVal = Math.exp((logits[i] - maxLogit).toDouble()).toFloat()
            exps[i] = expVal
            sumExp += expVal
        }
        val probabilities = FloatArray(logits.size)
        if (sumExp > 0f) {
            for (i in logits.indices) {
                probabilities[i] = exps[i] / sumExp
            }
        }
        return probabilities
    }
}

class SceneClassifier(
    private val context: Context,
) : AiModule {
    private val tag = "SceneClassifier"
    private var modelManager: ModelManager? = null
    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private var labels: List<String> = emptyList()

    override suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        try {
            modelManager = ModelManager(context)
            interpreter = modelManager?.loadModel("scene_classifier.tflite")
            labels = SceneLabelMap.loadLabels(context)
            isInitialized = true

            if (interpreter != null) {
                val active = interpreter!!
                val provider = InterpreterTensorInfoProvider(active)
                val modelFile = java.io.File(context.filesDir, "models/scene_classifier.tflite")
                val contractStatus = ModelContractChecker.checkContract(modelFile, ModelContracts.SCENE_CLASSIFIER, provider)
                Log.i(tag, "ModelContractChecker SceneClassifier Status: $contractStatus")
                val inTensor = active.getInputTensor(0)
                val outTensor = active.getOutputTensor(0)
                Log.i(tag, "SceneClassifier TFLite model loaded successfully (${labels.size} Places365 classes). Tensor contract: Input shape=${inTensor.shape().contentToString()}, dtype=${inTensor.dataType()}; Output shape=${outTensor.shape().contentToString()}, dtype=${outTensor.dataType()}")
            } else {
                Log.i(tag, "SceneClassifier TFLite model unavailable. Falling back to color-brightness heuristic engine.")
            }
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize SceneClassifier", e)
            isInitialized = true
            true
        }
    }

    override suspend fun analyze(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val detectionResult = analyzeWithDetection(bitmap)
        return@withContext detectionResult.label
    }

    suspend fun analyzeWithDetection(bitmap: Bitmap): DetectionResult = withContext(Dispatchers.Default) {
        if (!isInitialized) {
            initialize()
        }

        var detectedLabel = "✨ AI: Auto"
        var boundingBox: RectF? = null
        var coachingHint: String? = null
        var confidence = 0.85f
        var source = LabelSource.HEURISTIC_ENGINE

        val activeInterpreter = interpreter

        val inferenceTimeMs = measureTimeMillis {
            try {
                val width = 224
                val height = 224
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

                var totalBrightness = 0L
                var redSum = 0L
                var greenSum = 0L
                var blueSum = 0L
                val pixelCount = width * height

                val pixels = IntArray(pixelCount)
                scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

                var minX = width
                var maxX = 0
                var minY = height
                var maxY = 0
                var subjectPixelCount = 0

                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val index = y * width + x
                        val p = pixels[index]
                        val r = Color.red(p)
                        val g = Color.green(p)
                        val b = Color.blue(p)
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                        totalBrightness += luminance
                        redSum += r
                        greenSum += g
                        blueSum += b

                        if (luminance in 40..220) {
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                            subjectPixelCount++
                        }
                    }
                }

                val avgLuminance = totalBrightness / pixelCount
                val avgGreen = greenSum / pixelCount
                val avgBlue = blueSum / pixelCount
                val avgRed = redSum / pixelCount

                var modelHandled = false

                // 1. Night check has precedence (Places365 has no Night class)
                if (avgLuminance < 45) {
                    detectedLabel = "✨ AI: Night"
                    confidence = 0.90f
                    source = LabelSource.HEURISTIC_ENGINE
                    modelHandled = true
                } else if (activeInterpreter != null && labels.isNotEmpty()) {
                    // 2. Real Places365 TFLite NCHW Inference Pass
                    try {
                        val inputBuffer = NchwConverter.bitmapToNchwBuffer(scaledBitmap, width, height)
                        val outputArray = Array(1) { FloatArray(365) }
                        activeInterpreter.run(inputBuffer, outputArray)

                        val probabilities = NchwConverter.softmax(outputArray[0])
                        var topIndex = 0
                        var maxProb = -1.0f

                        // Top-5 ranking for verification/logging
                        val indexedProbs = probabilities.indices.map { it to probabilities[it] }
                            .sortedByDescending { it.second }
                            .take(5)

                        topIndex = indexedProbs[0].first
                        maxProb = indexedProbs[0].second

                        Log.d(tag, "Places365 Top-5 predictions:")
                        for ((idx, prob) in indexedProbs) {
                            val name = if (idx in labels.indices) labels[idx] else "unknown_$idx"
                            Log.d(tag, "  #$idx ($name): ${String.format("%.4f", prob)}")
                        }

                        if (maxProb >= 0.30f) {
                            detectedLabel = SceneLabelMap.mapIndexToAppLabel(labels, topIndex, maxProb)
                            confidence = maxProb
                            source = LabelSource.TFLITE_MODEL
                            modelHandled = true
                        }
                    } catch (e: Exception) {
                        Log.w(tag, "Places365 TFLite inference failed (${e.localizedMessage}). Falling back to heuristic.", e)
                    }
                }

                // 3. Heuristic fallback for non-night scenes if model did not handle
                if (!modelHandled) {
                    detectedLabel = when {
                        avgGreen > avgRed && avgGreen > avgBlue && avgGreen > 80 -> "✨ AI: Landscape"
                        avgRed > 110 && avgGreen > 90 && avgBlue < 120 -> "✨ AI: Portrait"
                        avgBlue > avgRed && avgBlue > 110 -> "✨ AI: Sky"
                        else -> "✨ AI: Auto"
                    }
                    source = LabelSource.HEURISTIC_ENGINE
                }

                // 4. Bounding box heuristic for Portrait subjects
                if (detectedLabel == "✨ AI: Portrait" && subjectPixelCount > (pixelCount * 0.08)) {
                    val normLeft = (minX.toFloat() / width).coerceIn(0.0f, 1.0f)
                    val normTop = (minY.toFloat() / height).coerceIn(0.0f, 1.0f)
                    val normRight = (maxX.toFloat() / width).coerceIn(0.0f, 1.0f)
                    val normBottom = (maxY.toFloat() / height).coerceIn(0.0f, 1.0f)

                    if (normRight > normLeft + 0.1f && normBottom > normTop + 0.1f) {
                        boundingBox = RectF(normLeft, normTop, normRight, normBottom)
                    } else {
                        boundingBox = null
                    }
                } else {
                    boundingBox = null
                }

                coachingHint = CoachingHintHelper.coachingHintFor(boundingBox)
                scaledBitmap.recycle()
            } catch (e: Exception) {
                Log.e(tag, "Error during live AI director detection inference", e)
                detectedLabel = "✨ AI: Auto"
                boundingBox = null
                coachingHint = null
            }
        }

        Log.d(tag, "AI Director Analysis: box=$boundingBox, hint=$coachingHint, label=$detectedLabel, source=$source in ${inferenceTimeMs}ms")
        return@withContext DetectionResult(
            label = detectedLabel,
            confidence = confidence,
            boundingBox = boundingBox,
            coachingHint = coachingHint,
            source = source,
        )
    }

    override fun close() {
        try {
            interpreter = null
            modelManager?.close()
            modelManager = null
            isInitialized = false
            Log.i(tag, "SceneClassifier closed")
        } catch (e: Exception) {
            Log.e(tag, "Error closing SceneClassifier", e)
        }
    }
}
