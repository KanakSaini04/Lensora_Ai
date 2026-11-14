package com.lensora.ai.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

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
        if (!isInitialized) {
            initialize()
        }

        var detectedLabel = "\u2728 AI: Auto"
        val inferenceTimeMs = measureTimeMillis {
            try {
                // Downscale bitmap to 224x224 input size on background thread
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

                var totalBrightness = 0L
                var redSum = 0L
                var greenSum = 0L
                var blueSum = 0L
                val pixelCount = 224 * 224

                val pixels = IntArray(pixelCount)
                scaledBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224)

                for (p in pixels) {
                    val r = Color.red(p)
                    val g = Color.green(p)
                    val b = Color.blue(p)
                    val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                    totalBrightness += luminance
                    redSum += r
                    greenSum += g
                    blueSum += b
                }

                val avgLuminance = totalBrightness / pixelCount
                val avgGreen = greenSum / pixelCount
                val avgBlue = blueSum / pixelCount
                val avgRed = redSum / pixelCount

                detectedLabel = when {
                    avgLuminance < 45 -> "\u2728 AI: Night"
                    avgGreen > avgRed && avgGreen > avgBlue && avgGreen > 80 -> "\u2728 AI: Landscape"
                    avgRed > 110 && avgGreen > 90 && avgBlue < 120 -> "\u2728 AI: Portrait"
                    avgBlue > avgRed && avgBlue > 110 -> "\u2728 AI: Sky"
                    else -> "\u2728 AI: Auto"
                }
            } catch (e: Exception) {
                Log.e(tag, "Error during scene classification inference", e)
                detectedLabel = "\u2728 AI: Auto"
            }
        }

        Log.d(tag, "Inference label: $detectedLabel in ${inferenceTimeMs}ms")
        return@withContext detectedLabel
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
