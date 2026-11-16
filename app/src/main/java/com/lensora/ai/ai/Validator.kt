package com.lensora.ai.ai

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log

class Validator {

    private val tag = "L4Validator"

    fun isValid(bitmap: Bitmap?): Boolean {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
            Log.w(tag, "L4 Validation FAIL: Bitmap is null or zero-dimensioned")
            return false
        }

        var totalLuminance = 0L
        val stepX = (bitmap.width / 20).coerceAtLeast(1)
        val stepY = (bitmap.height / 20).coerceAtLeast(1)

        val luminances = mutableListOf<Int>()
        var count = 0

        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                totalLuminance += lum
                luminances.add(lum)
                count++
            }
        }

        val avgLum = if (count > 0) (totalLuminance / count).toInt() else 0

        // L4 Anti-AI Rulebook: Check extreme luminance bounds (not pitch black & not blown out white)
        if (avgLum <= 2) {
            Log.w(tag, "L4 Validation FAIL: Pitch black image detected (avgLum: $avgLum <= 2)")
            return false
        }

        if (avgLum >= 253) {
            Log.w(tag, "L4 Validation FAIL: Blown out white image detected (avgLum: $avgLum >= 253)")
            return false
        }

        Log.i(tag, "L4 Validation PASS: Image luminance check valid (avgLum: $avgLum, samples: $count)")
        return true
    }

    fun isTileValid(tileBitmap: Bitmap?): Boolean {
        if (tileBitmap == null || tileBitmap.width <= 0 || tileBitmap.height <= 0) return false

        var totalLuminance = 0L
        val step = (tileBitmap.width / 10).coerceAtLeast(1)
        var count = 0

        for (x in 0 until tileBitmap.width step step) {
            for (y in 0 until tileBitmap.height step step) {
                val pixel = tileBitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                totalLuminance += lum
                count++
            }
        }

        val avgLum = if (count > 0) (totalLuminance / count).toInt() else 0
        val isValid = avgLum in 2..253
        if (!isValid) {
            Log.w(tag, "L4 Anti-AI Tile Validation FAIL: Extreme luminance shift in tile (avgLum: $avgLum)")
        }
        return isValid
    }
}
