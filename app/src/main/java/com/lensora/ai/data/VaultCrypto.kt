package com.lensora.ai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.ByteArrayOutputStream
import java.io.File

object VaultCrypto {

    private fun getMasterKeyAlias(): String {
        return MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    fun saveDocumentToVault(
        context: Context,
        docId: String,
        bitmap: Bitmap,
    ): Pair<File, File> {
        val masterKeyAlias = getMasterKeyAlias()
        val vaultDir = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }

        // 1. Generate 200x200 thumbnail
        val thumbnail = Bitmap.createScaledBitmap(bitmap, 200, 200, true)

        // 2. Encrypt & save thumbnail separately
        val thumbFile = File(vaultDir, "thumb_${docId}.enc")
        if (thumbFile.exists()) thumbFile.delete()
        val encryptedThumbFile = EncryptedFile.Builder(
            thumbFile,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()

        ByteArrayOutputStream().use { baos ->
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            encryptedThumbFile.openFileOutput().use { out ->
                out.write(baos.toByteArray())
            }
        }

        // 3. Encrypt & save full-res document
        val docFile = File(vaultDir, "doc_${docId}.enc")
        if (docFile.exists()) docFile.delete()
        val encryptedDocFile = EncryptedFile.Builder(
            docFile,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()

        ByteArrayOutputStream().use { baos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            encryptedDocFile.openFileOutput().use { out ->
                out.write(baos.toByteArray())
            }
        }

        return Pair(thumbFile, docFile)
    }

    fun loadThumbnail(context: Context, thumbFile: File): Bitmap? {
        return try {
            val masterKeyAlias = getMasterKeyAlias()
            val encryptedFile = EncryptedFile.Builder(
                thumbFile,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
            ).build()

            encryptedFile.openFileInput().use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun loadFullDocument(context: Context, docFile: File): Bitmap? {
        return try {
            val masterKeyAlias = getMasterKeyAlias()
            val encryptedFile = EncryptedFile.Builder(
                docFile,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
            ).build()

            encryptedFile.openFileInput().use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            null
        }
    }
}
