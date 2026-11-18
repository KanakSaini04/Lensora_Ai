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
        val modelsDir = File(context.filesDir, "models")
        val localFile = File(modelsDir, modelFileName)
        if (ModelFileGuard.isUsable(localFile)) {
            return try {
                Interpreter(localFile).also {
                    interpreter = it
                    Log.i(tag, "Successfully loaded model from local storage: ${localFile.name} (${localFile.length()} bytes)")
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to instantiate TFLite Interpreter for local file ${localFile.absolutePath}", e)
                null
            }
        }

        return try {
            val assetFd = context.assets.openFd(modelFileName)
            val inputStream = FileInputStream(assetFd.fileDescriptor)
            val fileChannel = inputStream.channel
            val modelBuffer: ByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFd.startOffset,
                assetFd.declaredLength,
            )
            Interpreter(modelBuffer).also {
                interpreter = it
                Log.i(tag, "Successfully loaded asset model: $modelFileName")
            }
        } catch (e: Exception) {
            Log.w(tag, "TFLite model $modelFileName unavailable (missing locally and in assets): ${e.localizedMessage}. Falling back to heuristic AI engine.")
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
            if (ModelFileGuard.isUsable(file)) {
                Log.i(tag, "Found Image-to-Image AI Enhancement model: ${file.name} (${file.length()} bytes)")
                return file
            }
        }

        val otherModels = modelsDir.listFiles { _, name ->
            name.endsWith(".tflite", ignoreCase = true) && !name.contains("mobilenet", ignoreCase = true)
        }
        return otherModels?.firstOrNull { ModelFileGuard.isUsable(it) }
    }

    fun getLamaModelFile(): File? {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) return null

        val candidates = listOf(
            "lama_dilated.tflite",
            "lama_inpainting.tflite",
            "object_eraser.tflite",
        )

        for (candidate in candidates) {
            val file = File(modelsDir, candidate)
            if (ModelFileGuard.isUsable(file)) {
                Log.i(tag, "Found LaMa-Dilated Inpainting model: ${file.name} (${file.length()} bytes)")
                return file
            }
        }
        return null
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
