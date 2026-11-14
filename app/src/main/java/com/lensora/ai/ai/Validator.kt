package com.lensora.ai.ai

import android.graphics.Bitmap
import android.graphics.Color

class Validator {
    fun isValid(bitmap: Bitmap?): Boolean {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return false

        var totalLuminance = 0L
        val stepX = (bitmap.width / 10).coerceAtLeast(1)
        val stepY = (bitmap.height / 10).coerceAtLeast(1)

        var count = 0
        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                totalLuminance += lum
                count++
            }
        }

        val avgLum = if (count > 0) totalLuminance / count else 0
        return avgLum > 2
    }
}
