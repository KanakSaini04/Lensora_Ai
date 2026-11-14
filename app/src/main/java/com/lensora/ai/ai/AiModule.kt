package com.lensora.ai.ai

import android.graphics.Bitmap

interface AiModule {
    suspend fun initialize(): Boolean
    suspend fun analyze(bitmap: Bitmap): String
    fun close()
}
