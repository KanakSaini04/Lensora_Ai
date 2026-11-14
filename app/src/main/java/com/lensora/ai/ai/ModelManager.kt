package com.lensora.ai.ai

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
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
                Log.i(tag, "Successfully loaded model: $modelFileName")
            }
        } catch (e: Exception) {
            Log.w(tag, "TFLite model $modelFileName not found in assets. Falling back to heuristic AI engine: ${e.localizedMessage}")
            null
        }
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
