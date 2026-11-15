package com.lensora.ai.ui.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ColorMatrix as ComposeColorMatrix
import com.lensora.ai.ui.vibes.VibePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageEditor {

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
    ): Result<Uri> = withContext(Dispatchers.IO) {
        var srcBitmap: Bitmap? = null
        var editedBitmap: Bitmap? = null
        try {
            // 1. Decode full-res source image from URI
            val inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return@withContext Result.failure(Exception("Failed to open input stream for $inputUri"))

            srcBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (srcBitmap == null) {
                return@withContext Result.failure(Exception("Failed to decode bitmap from $inputUri"))
            }

            // 2. Create target mutable bitmap
            val width = srcBitmap.width
            val height = srcBitmap.height
            editedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // 3. Construct combined stacked ColorMatrix (Base Exposure/Contrast + Vibe Preset)
            val baseMatrix = createAndroidColorMatrix(exposure, contrast)
            val vibeMatrix = createVibeAndroidColorMatrix(vibe)
            baseMatrix.postConcat(vibeMatrix)

            // 4. Render ColorMatrix onto software Canvas
            val canvas = Canvas(editedBitmap)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(baseMatrix)
                isAntiAlias = true
                isFilterBitmap = true
            }
            canvas.drawBitmap(srcBitmap, 0f, 0f, paint)

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
            Result.failure(Exception("Image too large to export"))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            srcBitmap?.recycle()
            editedBitmap?.recycle()
        }
    }
}
