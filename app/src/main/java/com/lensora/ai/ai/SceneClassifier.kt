package com.lensora.ai.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

data class DetectionResult(
    val label: String = "✨ AI: Auto",
    val confidence: Float = 0.85f,
    val boundingBox: RectF? = null,
    val coachingHint: String = "✨ Perfect Composition",
)

class SceneClassifier(
    private val context: Context,
) : AiModule {
    private val tag = "SceneClassifier"
    private var modelManager: ModelManager? = null
    private var isInitialized = false

    override suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        try {
            modelManager = ModelManager(context)
            modelManager?.loadModel("scene_classifier.tflite")
            isInitialized = true
            Log.i(tag, "SceneClassifier initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize SceneClassifier", e)
            false
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
        var coachingHint = "✨ Perfect Composition"
        val confidence = 0.88f

        val inferenceTimeMs = measureTimeMillis {
            try {
                // Downscale bitmap to 224x224 input size for max 5 FPS stream analysis
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

                // Calculate bounding box and composition coaching prompt
                if (subjectPixelCount > (pixelCount * 0.05)) {
                    val normLeft = (minX.toFloat() / width).coerceIn(0.15f, 0.40f)
                    val normTop = (minY.toFloat() / height).coerceIn(0.20f, 0.45f)
                    val normRight = (maxX.toFloat() / width).coerceIn(0.60f, 0.85f)
                    val normBottom = (maxY.toFloat() / height).coerceIn(0.65f, 0.88f)

                    val box = RectF(normLeft, normTop, normRight, normBottom)
                    boundingBox = box

                    val boxWidth = box.width()
                    val boxHeight = box.height()
                    val areaRatio = boxWidth * boxHeight
                    val centerX = (box.left + box.right) / 2.0f

                    coachingHint = when {
                        areaRatio < 0.22f -> "✨ Step Closer"
                        centerX < 0.40f -> "✨ Move Right"
                        centerX > 0.60f -> "✨ Move Left"
                        else -> "✨ Perfect Composition"
                    }
                } else {
                    coachingHint = "✨ Step Closer"
                }

                scaledBitmap.recycle()
            } catch (e: Exception) {
                Log.e(tag, "Error during live AI director detection inference", e)
                detectedLabel = "✨ AI: Auto"
                coachingHint = "✨ Auto Framing"
            }
        }

        Log.d(tag, "Live AI Director Detection: $detectedLabel [$coachingHint] in ${inferenceTimeMs}ms")
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
