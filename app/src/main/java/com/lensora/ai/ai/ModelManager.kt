package com.lensora.ai.ai

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class ModelManager(
    private val context: Context,
) {
    private val tag = "ModelManager"
    private var interpreter: Interpreter? = null

    fun loadModel(modelFileName: String): Interpreter? {
        return try {
            val assetFileDescriptor = context.assets.openFd(modelFileName)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer: ByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength,
            )
            Interpreter(modelBuffer).also {
                interpreter = it
                Log.i(tag, "Successfully loaded asset model: $modelFileName")
            }
        } catch (e: Exception) {
            Log.w(tag, "TFLite model $modelFileName not found in assets. Falling back to heuristic AI engine: ${e.localizedMessage}")
            null
        }
    }

    fun getEnhancementModelFile(): File? {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) return null

        val priorityCandidates = listOf(
            "real_esrgan_v2.tflite",
            "dehaze.tflite",
            "image_enhancer.tflite",
        )

        for (candidate in priorityCandidates) {
            val file = File(modelsDir, candidate)
            if (file.exists() && file.length() > 0) {
                Log.i(tag, "Found Image-to-Image AI Enhancement model: ${file.name} (${file.length()} bytes)")
                return file
            }
        }

        // Fallback: Check any .tflite file in models/ that is NOT MobileNet
        val otherModels = modelsDir.listFiles { _, name ->
            name.endsWith(".tflite", ignoreCase = true) && !name.contains("mobilenet", ignoreCase = true)
        }
        return otherModels?.firstOrNull()
    }

    fun close() {
        try {
            interpreter?.close()
            interpreter = null
        } catch (e: Exception) {
            Log.e(tag, "Error closing model manager", e)
        }
    }
}
