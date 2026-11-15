package com.lensora.ai.ui.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object LutParser {
    private const val TAG = "LutParser"

    fun parseCubeAsset(context: Context, assetPath: String): Bitmap? {
        return try {
            context.assets.open(assetPath).use { stream ->
                parseCubeTo2DStripBitmap(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening LUT asset $assetPath: ${e.localizedMessage}")
            null
        }
    }

    fun parseCubeTo2DStripBitmap(inputStream: InputStream): Bitmap? {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            var lutSize = 0
            val rgbList = ArrayList<IntArray>()

            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.startsWith("LUT_3D_SIZE")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 2) {
                        lutSize = parts[1].toInt()
                    }
                } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("TITLE") && !trimmed.startsWith("DOMAIN_")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 3) {
                        val r = (parts[0].toFloat().coerceIn(0f, 1f) * 255f).toInt()
                        val g = (parts[1].toFloat().coerceIn(0f, 1f) * 255f).toInt()
                        val b = (parts[2].toFloat().coerceIn(0f, 1f) * 255f).toInt()
                        rgbList.add(intArrayOf(r, g, b))
                    }
                }
                line = reader.readLine()
            }

            if (lutSize <= 0 || rgbList.size < lutSize * lutSize * lutSize) {
                Log.e(TAG, "Invalid .cube LUT file format. Size: $lutSize, Entries: ${rgbList.size}")
                return null
            }

            // Create 2D strip Bitmap: Width = N * N, Height = N
            val width = lutSize * lutSize
            val height = lutSize
            val bitmapPixels = IntArray(width * height)

            var entryIndex = 0
            for (b in 0 until lutSize) { // Blue slice (tile index along X axis)
                val tileXOffset = b * lutSize
                for (g in 0 until lutSize) { // Green row
                    for (r in 0 until lutSize) { // Red column
                        val rgb = rgbList[entryIndex++]
                        val pixelColor = Color.rgb(rgb[0], rgb[1], rgb[2])
                        val targetX = tileXOffset + r
                        val targetY = g
                        bitmapPixels[targetY * width + targetX] = pixelColor
                    }
                }
            }

            val stripBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            stripBitmap.setPixels(bitmapPixels, 0, width, 0, 0, width, height)
            Log.i(TAG, "Parsed .cube LUT successfully into 2D Strip Bitmap (${width}x${height}, N=$lutSize)")
            return stripBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse .cube LUT file", e)
            return null
        }
    }
}
