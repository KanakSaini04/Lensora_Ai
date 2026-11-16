package com.lensora.ai.ui.vault

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lensora.ai.data.VaultCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class VaultItem(
    val id: String,
    val thumbFile: File,
    val docFile: File,
    val thumbnailBitmap: Bitmap?,
)

class VaultViewModel : ViewModel() {

    private val _vaultItems = MutableStateFlow<List<VaultItem>>(emptyList())
    val vaultItems: StateFlow<List<VaultItem>> = _vaultItems.asStateFlow()

    private val _selectedFullImage = MutableStateFlow<Bitmap?>(null)
    val selectedFullImage: StateFlow<Bitmap?> = _selectedFullImage.asStateFlow()

    fun loadVaultThumbnails(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val vaultDir = File(context.filesDir, "vault")
            if (!vaultDir.exists()) return@launch

            val thumbFiles = vaultDir.listFiles { _, name -> name.startsWith("thumb_") && name.endsWith(".enc") } ?: emptyArray()
            val items = thumbFiles.mapNotNull { thumbFile ->
                val id = thumbFile.name.removePrefix("thumb_").removeSuffix(".enc")
                val docFile = File(vaultDir, "doc_${id}.enc")
                val thumbBitmap = VaultCrypto.loadThumbnail(context, thumbFile)
                if (docFile.exists()) {
                    VaultItem(id = id, thumbFile = thumbFile, docFile = docFile, thumbnailBitmap = thumbBitmap)
                } else null
            }

            _vaultItems.value = items
        }
    }

    fun saveToVault(context: Context, docId: String, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            VaultCrypto.saveDocumentToVault(context, docId, bitmap)
            loadVaultThumbnails(context)
        }
    }

    fun loadFullDocument(context: Context, item: VaultItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val fullBitmap = VaultCrypto.loadFullDocument(context, item.docFile)
            _selectedFullImage.value = fullBitmap
        }
    }

    fun clearSelectedFullImage() {
        _selectedFullImage.value = null
    }
}
