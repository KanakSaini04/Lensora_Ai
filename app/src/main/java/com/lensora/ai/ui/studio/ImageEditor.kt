package com.lensora.ai.ui.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import com.lensora.ai.ui.vibes.VibePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageEditor {

    private const val TAG = "ImageEditor"

    fun getLutStripBitmap(context: Context, vibe: VibePreset): Bitmap? {
        return when (vibe) {
            VibePreset.CINEMATIC -> LutParser.parseCubeAsset(context, "luts/teal_orange.cube")
            VibePreset.LEICA -> LutParser.generateLeicaBwStripBitmap(17)
            else -> null
        }
    }

    fun createAndroidColorMatrix(
        exposure: Float,
        contrast: Float,
    ): ColorMatrix {
        val evScale = 1.0f + (exposure * 0.25f)
        val contrastScale = 1.0f + (contrast / 50.0f) * 0.5f

        val combinedScale = contrastScale * evScale
        val translate = 128f * (1f - contrastScale) + (exposure * 20f)

        return ColorMatrix(
            floatArrayOf(
                combinedScale, 0f, 0f, 0f, translate,
                0f, combinedScale, 0f, 0f, translate,
                0f, 0f, combinedScale, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    fun createVibeAndroidColorMatrix(vibe: VibePreset): ColorMatrix {
        return when (vibe) {
            VibePreset.NONE -> ColorMatrix()
            VibePreset.CINEMATIC -> ColorMatrix(
                floatArrayOf(
                    1.20f, 0f, 0f, 0f, 10f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 0.90f, 0f, -10f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            VibePreset.LEICA -> ColorMatrix(
                floatArrayOf(
                    0.35f, 0.55f, 0.10f, 0f, -15f,
                    0.35f, 0.55f, 0.10f, 0f, -15f,
                    0.35f, 0.55f, 0.10f, 0f, -15f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            VibePreset.VINTAGE -> ColorMatrix(
                floatArrayOf(
                    1.10f, 0f, 0f, 0f, 25f,
                    0f, 1.05f, 0f, 0f, 15f,
                    0f, 0f, 0.85f, 0f, 30f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
            VibePreset.ROYAL_GOLD -> ColorMatrix(
                floatArrayOf(
                    1.25f, 0f, 0f, 0f, 15f,
                    0f, 1.15f, 0f, 0f, 10f,
                    0f, 0f, 0.80f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }
    }

    fun createCombinedColorMatrix(
        exposure: Float,
        contrast: Float,
        vibe: VibePreset = VibePreset.NONE,
    ): ComposeColorMatrix {
        val baseMatrix = createAndroidColorMatrix(exposure, contrast)
        val vibeMatrix = createVibeAndroidColorMatrix(vibe)
        baseMatrix.postConcat(vibeMatrix)
        return ComposeColorMatrix(baseMatrix.array)
    }

    suspend fun exportImage(
        context: Context,
        inputUri: Uri,
        exposure: Float,
        contrast: Float,
        vibe: VibePreset = VibePreset.NONE,
        includeTimestamp: Boolean = false,
    ): Result<Uri> = withContext(Dispatchers.IO) {
        var editedBitmap: Bitmap? = null
        try {
            // 1. Memory Shield: Use BitmapRegionDecoder for 512x512 tiled processing (no full image decode into RAM)
            val decoder = context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                BitmapRegionDecoder.newInstance(inputStream)
            } ?: return@withContext Result.failure(Exception("Failed to initialize BitmapRegionDecoder for $inputUri"))

            val width = decoder.width
            val height = decoder.height

            // 2. Create target mutable bitmap
            editedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // 3. Construct combined stacked ColorMatrix (Base Exposure/Contrast + Vibe Preset)
            val baseMatrix = createAndroidColorMatrix(exposure, contrast)
            val vibeMatrix = createVibeAndroidColorMatrix(vibe)
            baseMatrix.postConcat(vibeMatrix)

            val canvas = Canvas(editedBitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(baseMatrix)
                isFilterBitmap = true
            }

            // 4. Memory Shield: 512x512 Tiled Pipeline
            val tileSize = 512
            val numCols = (width + tileSize - 1) / tileSize
            val numRows = (height + tileSize - 1) / tileSize
            val totalTiles = numCols * numRows

            Log.i(TAG, "Starting Studio Export 512x512 Tiled pass ($width x $height px, Total Tiles: $totalTiles)")

            var tileIndex = 0
            for (row in 0 until numRows) {
                for (col in 0 until numCols) {
                    val left = col * tileSize
                    val top = row * tileSize
                    val right = (left + tileSize).coerceAtMost(width)
                    val bottom = (top + tileSize).coerceAtMost(height)
                    val rect = Rect(left, top, right, bottom)

                    // Decode only 512x512 tile
                    val tileBitmap = decoder.decodeRegion(rect, null)
                    if (tileBitmap != null) {
                        canvas.drawBitmap(tileBitmap, left.toFloat(), top.toFloat(), paint)
                        tileBitmap.recycle() // Memory Shield: Recycle tile immediately!
                    }

                    tileIndex++
                    Log.d(TAG, "Export Tile $tileIndex of $totalTiles processed ($left,$top - $right,$bottom)")
                }
            }

            // 4b. Draw VN-Style Timestamp Overlay on exported Canvas if enabled
            if (includeTimestamp) {
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                val textToDraw = "LENSORA • $timeStr"

                val fontSize = (height * 0.035f).coerceAtLeast(36f)
                val textMarginLeft = width * 0.04f
                val textMarginBottom = height * 0.05f

                val textPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#00E5FF")
                    textSize = fontSize
                    isAntiAlias = true
                    isFakeBoldText = true
                    setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
                }

                val textBounds = Rect()
                textPaint.getTextBounds(textToDraw, 0, textToDraw.length, textBounds)

                val pillPaddingX = fontSize * 0.4f
                val pillPaddingY = fontSize * 0.25f

                val bgRect = RectF(
                    textMarginLeft - pillPaddingX,
                    (height - textMarginBottom) - textBounds.height() - pillPaddingY,
                    textMarginLeft + textBounds.width() + pillPaddingX,
                    (height - textMarginBottom) + pillPaddingY,
                )

                val bgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#99131313")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

                val borderPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#6600E5FF")
                    style = Paint.Style.STROKE
                    strokeWidth = fontSize * 0.06f
                    isAntiAlias = true
                }

                val cornerRadius = fontSize * 0.3f
                canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint)
                canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, borderPaint)

                canvas.drawText(
                    textToDraw,
                    textMarginLeft,
                    height - textMarginBottom,
                    textPaint,
                )
            }

            // 5. Save edited bitmap to MediaStore as LENSORA_EDITED_<timestamp>.jpg
            val timeStamp = System.currentTimeMillis()
            val fileName = "LENSORA_EDITED_$timeStamp.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Lensora_Ai")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Failed to insert row into MediaStore"))

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                editedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            } ?: return@withContext Result.failure(Exception("Failed to open MediaStore output stream"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }

            Result.success(uri)
        } catch (oom: OutOfMemoryError) {
            Log.e(TAG, "OOM Error during export", oom)
            Result.failure(Exception("Image too large to export"))
        } catch (e: Exception) {
            Log.e(TAG, "Error during exportImage tiled pass", e)
            Result.failure(e)
        } finally {
            editedBitmap?.recycle()
        }
    }
}
