package com.lensora.ai.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lensora.ai.domain.DocumentSealer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class ScanState {
    CAMERA,
    REVIEW,
    MASK
}

class ScanViewModel : ViewModel() {

    private val _scanState = MutableStateFlow(ScanState.CAMERA)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _scannedPages = MutableStateFlow<List<Bitmap>>(emptyList())
    val scannedPages: StateFlow<List<Bitmap>> = _scannedPages.asStateFlow()

    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _manualRedactRects = MutableStateFlow<List<RectF>>(emptyList())
    val manualRedactRects: StateFlow<List<RectF>> = _manualRedactRects.asStateFlow()

    private val _isDrawCustomBoxActive = MutableStateFlow(false)
    val isDrawCustomBoxActive: StateFlow<Boolean> = _isDrawCustomBoxActive.asStateFlow()

    fun onPictureCaptured(bitmap: Bitmap) {
        _currentBitmap.value = bitmap
        _scanState.value = ScanState.REVIEW
    }

    fun addPageAndScanNext() {
        val current = _currentBitmap.value
        if (current != null) {
            _scannedPages.value = _scannedPages.value + current
        }
        _currentBitmap.value = null
        _scanState.value = ScanState.CAMERA
    }

    fun proceedToMaskState() {
        val current = _currentBitmap.value
        if (current != null) {
            _scannedPages.value = _scannedPages.value + current
        }
        _scanState.value = ScanState.MASK
    }

    fun toggleDrawCustomBox() {
        _isDrawCustomBoxActive.value = !_isDrawCustomBoxActive.value
    }

    fun addManualRect(rect: RectF) {
        _manualRedactRects.value = _manualRedactRects.value + rect
    }

    fun applyFinalRedactionAndGeneratePdf(context: Context, onComplete: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val pages = _scannedPages.value
            val rects = _manualRedactRects.value

            val redactedPages = pages.map { original ->
                val mutable = original.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)
                val blackPaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }

                for (rect in rects) {
                    canvas.drawRect(rect, blackPaint)
                }
                mutable
            }

            val outputFile = File(context.cacheDir, "multi_page_doc_${System.currentTimeMillis()}.pdf")
            DocumentSealer.generateMultiPagePdf(redactedPages, outputFile)
            _scanState.value = ScanState.CAMERA
            _scannedPages.value = emptyList()
            _manualRedactRects.value = emptyList()

            onComplete(outputFile)
        }
    }
}
