package com.lensora.ai.ui.scan

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lensora.ai.ui.theme.AccentCyan
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val scanState by viewModel.scanState.collectAsState()
    val currentBitmap by viewModel.currentBitmap.collectAsState()
    val scannedPages by viewModel.scannedPages.collectAsState()
    val manualRects by viewModel.manualRedactRects.collectAsState()
    val isDrawCustomBoxActive by viewModel.isDrawCustomBoxActive.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isPermanentlyDenied by remember {
        mutableStateOf(
            activity != null &&
                    !hasCameraPermission &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (scanState) {
                            ScanState.CAMERA -> "Scan Document (Page ${scannedPages.size + 1})"
                            ScanState.REVIEW -> "Review Page (${scannedPages.size + 1})"
                            ScanState.MASK -> "Redact & Mask Document"
                        },
                        color = ColorWhite,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalSurface),
            )
        },
        containerColor = PitchBlackBackground,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PitchBlackBackground),
        ) {
            if (!hasCameraPermission) {
                // Permanent Camera Permission Denial Handling
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                ) {
                    Text(
                        text = if (isPermanentlyDenied)
                            "Camera permission is permanently denied. Please enable it in Settings."
                        else
                            "Camera permission is required to scan documents.",
                        color = ColorWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (isPermanentlyDenied) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } else {
                                activity?.let {
                                    ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.CAMERA), 101)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = if (isPermanentlyDenied) "Open Settings" else "Grant Permission",
                            color = ColorWhite,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                when (scanState) {
                    ScanState.CAMERA -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text("Camera Ready for Page ${scannedPages.size + 1}", color = ColorMutedGray)
                        }
                    }
                    ScanState.REVIEW -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            currentBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Scanned Page Preview",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(16.dp),
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Button(
                                    onClick = { viewModel.addPageAndScanNext() },
                                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurface),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("Add Page", color = AccentCyan, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.proceedToMaskState() },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("Crop & Continue", color = ColorWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    ScanState.MASK -> {
                        var dragStart by remember { mutableStateOf<Offset?>(null) }
                        var dragCurrent by remember { mutableStateOf<Offset?>(null) }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(16.dp)
                                    .pointerInput(isDrawCustomBoxActive) {
                                        if (isDrawCustomBoxActive) {
                                            detectDragGestures(
                                                onDragStart = { start ->
                                                    dragStart = start
                                                    dragCurrent = start
                                                },
                                                onDrag = { change, _ ->
                                                    dragCurrent = change.position
                                                },
                                                onDragEnd = {
                                                    val start = dragStart
                                                    val end = dragCurrent
                                                    if (start != null && end != null) {
                                                        val rect = RectF(
                                                            minOf(start.x, end.x),
                                                            minOf(start.y, end.y),
                                                            maxOf(start.x, end.x),
                                                            maxOf(start.y, end.y),
                                                        )
                                                        viewModel.addManualRect(rect)
                                                    }
                                                    dragStart = null
                                                    dragCurrent = null
                                                },
                                            )
                                        }
                                    },
                            ) {
                                currentBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Redaction Mask Layer",
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    for (rect in manualRects) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(rect.left, rect.top),
                                            size = Size(rect.width(), rect.height()),
                                        )
                                    }

                                    val start = dragStart
                                    val current = dragCurrent
                                    if (start != null && current != null) {
                                        val left = minOf(start.x, current.x)
                                        val top = minOf(start.y, current.y)
                                        val w = kotlin.math.abs(current.x - start.x)
                                        val h = kotlin.math.abs(current.y - start.y)
                                        drawRect(
                                            color = Color.Red,
                                            topLeft = Offset(left, top),
                                            size = Size(w, h),
                                            style = Stroke(width = 2.dp.toPx()),
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Button(
                                    onClick = { viewModel.toggleDrawCustomBox() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDrawCustomBoxActive) AccentCyan else CharcoalSurface,
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = if (isDrawCustomBoxActive) "Drawing Active" else "Draw Custom Box",
                                        color = if (isDrawCustomBoxActive) PitchBlackBackground else ColorWhite,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.applyFinalRedactionAndGeneratePdf(context) { pdfFile ->
                                            Toast.makeText(context, "Saved Multi-Page PDF: ${pdfFile.name}", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("Finalize & Export PDF", color = ColorWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
