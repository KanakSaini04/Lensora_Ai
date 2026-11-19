package com.lensora.ai.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

class SceneClassifier(
    private val context: Context,
) : AiModule {
    private val tag = "SceneClassifier"
    private var modelManager: ModelManager? = null
    private var isInitialized = false

    override suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        try {
            modelManager = ModelManager(context)
            val interpreter = modelManager?.loadModel("scene_classifier.tflite")
            isInitialized = true
            if (interpreter != null) {
                Log.i(tag, "SceneClassifier TFLite model loaded successfully")
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
        val confidence = 0.88f

        val inferenceTimeMs = measureTimeMillis {
            try {
                // Downscale bitmap to 224x224 input size for stream analysis
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

                        // Detect subject bounding cluster (high contrast / subject region)
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

                detectedLabel = when {
                    avgLuminance < 45 -> "✨ AI: Night"
                    avgGreen > avgRed && avgGreen > avgBlue && avgGreen > 80 -> "✨ AI: Landscape"
                    avgRed > 110 && avgGreen > 90 && avgBlue < 120 -> "✨ AI: Portrait"
                    avgBlue > avgRed && avgBlue > 110 -> "✨ AI: Sky"
                    else -> "✨ AI: Auto"
                }

                // Calculate actual bounding box for Portrait subjects ONLY
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

        Log.d(tag, "AI Director Analysis: box=$boundingBox, hint=$coachingHint, label=$detectedLabel in ${inferenceTimeMs}ms")
        return@withContext DetectionResult(
            label = detectedLabel,
            confidence = confidence,
            boundingBox = boundingBox,
            coachingHint = coachingHint,
        )
    }

    override fun close() {
        try {
            modelManager?.close()
            modelManager = null
            isInitialized = false
            Log.i(tag, "SceneClassifier closed")
        } catch (e: Exception) {
            Log.e(tag, "Error closing SceneClassifier", e)
        }
    }
}
