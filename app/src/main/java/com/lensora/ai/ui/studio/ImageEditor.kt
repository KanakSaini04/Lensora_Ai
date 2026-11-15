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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageEditor {

    fun createColorMatrix(
        exposure: Float,
        contrast: Float,
    ): ComposeColorMatrix {
        // Exposure gain scaling (-2.0 EV to +2.0 EV)
        val evScale = 1.0f + (exposure * 0.25f)

        // Contrast scaling (-50 to +50 -> 0.5x to 1.5x)
        val contrastScale = 1.0f + (contrast / 50.0f) * 0.5f

        val combinedScale = contrastScale * evScale
        val translate = 128f * (1f - contrastScale) + (exposure * 20f)

        return ComposeColorMatrix(
            floatArrayOf(
                combinedScale, 0f, 0f, 0f, translate,
                0f, combinedScale, 0f, 0f, translate,
                0f, 0f, combinedScale, 0f, translate,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }

    suspend fun exportImage(
        context: Context,
        inputUri: Uri,
        exposure: Float,
        contrast: Float,
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

            // 3. Construct android.graphics.ColorMatrix matching Compose ColorMatrix
            val evScale = 1.0f + (exposure * 0.25f)
            val contrastScale = 1.0f + (contrast / 50.0f) * 0.5f
            val combinedScale = contrastScale * evScale
            val translate = 128f * (1f - contrastScale) + (exposure * 20f)

            val colorMatrix = ColorMatrix(
                floatArrayOf(
                    combinedScale, 0f, 0f, 0f, translate,
                    0f, combinedScale, 0f, 0f, translate,
                    0f, 0f, combinedScale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )

            // 4. Render ColorMatrix onto software Canvas
            val canvas = Canvas(editedBitmap)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
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
