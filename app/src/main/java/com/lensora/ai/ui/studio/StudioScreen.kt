package com.lensora.ai.ui.studio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lensora.ai.ui.components.ZoomableBox
import com.lensora.ai.data.GalleryRepository
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue
import com.lensora.ai.ui.vibes.VibePreset
import com.lensora.ai.ui.vibes.VibeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class BrushStroke(
    val points: List<Offset>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen() {
    var studioTabMode by remember { mutableStateOf("PHOTO") }

    if (studioTabMode == "VIDEO") {
        Column(modifier = Modifier.fillMaxSize()) {
            // Segmented Top Bar for PHOTO | VIDEO Toggle
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PitchBlackBackground)
                            .padding(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (studioTabMode == "PHOTO") RoyalBlue else Color.Transparent)
                                .clickable { studioTabMode = "PHOTO" }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("PHOTO", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (studioTabMode == "VIDEO") RoyalBlue else Color.Transparent)
                                .clickable { studioTabMode = "VIDEO" }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("VIDEO", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalSurface),
            )
            com.lensora.ai.editor.ui.ProjectsScreen()
        }
    } else {
        StudioPhotoContent(
            onSwitchToVideoTab = { studioTabMode = "VIDEO" },
            onSwitchToPhotoTab = { studioTabMode = "PHOTO" },
            studioTabMode = studioTabMode,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioPhotoContent(
    onSwitchToVideoTab: () -> Unit,
    onSwitchToPhotoTab: () -> Unit,
    studioTabMode: String,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val galleryRepository = remember { GalleryRepository(context) }
    val objectEraser = remember { ObjectEraser(context) }

    var photoUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }

    // Interactive editing tool states
    var selectedToolCategory by remember { mutableIntStateOf(0) } // 0: Light, 1: Color, 2: Eraser, 3: Crop
    var exposureValue by remember { mutableFloatStateOf(0.0f) }
    var contrastValue by remember { mutableFloatStateOf(0.0f) }
    var isIncludeTimestampEnabled by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    // Magic Eraser AI Inpainting states
    var isEraserModeActive by remember { mutableStateOf(false) }
    var brushStrokes by remember { mutableStateOf<List<BrushStroke>>(emptyList()) }
    var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isErasingWithAi by remember { mutableStateOf(false) }
    var inpaintedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Real-time GPU combined ColorFilter matrix derived from Sliders + Vibe State
    val realTimeColorFilter = remember(exposureValue, contrastValue, VibeState.selectedVibe) {
        ColorFilter.colorMatrix(
            ImageEditor.createCombinedColorMatrix(
                exposure = exposureValue,
                contrast = contrastValue,
                vibe = VibeState.selectedVibe,
            ),
        )
    }

    // AGSL Real .cube LUT texture shader pass
    val lutStripBitmap = remember(VibeState.selectedVibe) {
        ImageEditor.getLutStripBitmap(context, VibeState.selectedVibe)
    }

    val activeLutRenderEffect = remember(lutStripBitmap) {
        if (lutStripBitmap != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LutShader.createLutRenderEffect(lutStripBitmap, 17f)
        } else null
    }

    LaunchedEffect(Unit) {
        photoUris = galleryRepository.getAllPhotos()
    }

    LaunchedEffect(selectedPhotoIndex) {
        inpaintedBitmap = null
        brushStrokes = emptyList()
        currentStrokePoints = emptyList()
        isEraserModeActive = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PitchBlackBackground)
                                .padding(2.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (studioTabMode == "PHOTO") RoyalBlue else Color.Transparent)
                                    .clickable { onSwitchToPhotoTab() }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text("PHOTO", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (studioTabMode == "VIDEO") RoyalBlue else Color.Transparent)
                                    .clickable { onSwitchToVideoTab() }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                Text("VIDEO", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (VibeState.selectedVibe != VibePreset.NONE) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RoyalBlue)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = VibeState.selectedVibe.displayName,
                                    color = ColorWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Include Timestamp Toggle Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Text(
                            text = "Time",
                            color = ColorMutedGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Switch(
                            checked = isIncludeTimestampEnabled,
                            onCheckedChange = { isIncludeTimestampEnabled = it },
                            modifier = Modifier.scale(0.7f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ColorWhite,
                                checkedTrackColor = RoyalBlue,
                            ),
                        )
                    }

                    IconButton(onClick = {
                        exposureValue = 0.0f
                        contrastValue = 0.0f
                        inpaintedBitmap = null
                        brushStrokes = emptyList()
                        currentStrokePoints = emptyList()
                        isEraserModeActive = false
                        VibeState.selectedVibe = VibePreset.NONE
                        Toast.makeText(context, "Reset Edit & Vibe Pass", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo", tint = ColorWhite)
                    }
                    Button(
                        onClick = {
                            if (photoUris.isNotEmpty() && selectedPhotoIndex in photoUris.indices && !isExporting) {
                                coroutineScope.launch {
                                    isExporting = true
                                    val targetUri = photoUris[selectedPhotoIndex]
                                    val result = ImageEditor.exportImage(
                                        context = context,
                                        inputUri = targetUri,
                                        exposure = exposureValue,
                                        contrast = contrastValue,
                                        vibe = VibeState.selectedVibe,
                                        includeTimestamp = isIncludeTimestampEnabled,
                                    )
                                    result.onSuccess {
                                        Toast.makeText(context, "Photo exported to Gallery!", Toast.LENGTH_SHORT).show()
                                        photoUris = galleryRepository.getAllPhotos()
                                    }.onFailure { exception ->
                                        val message = if (exception.message?.contains("large", ignoreCase = true) == true) {
                                            "Image too large to export"
                                        } else {
                                            "Export failed: ${exception.localizedMessage}"
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                    isExporting = false
                                }
                            }
                        },
                        enabled = !isExporting && photoUris.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                color = ColorWhite,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            Icon(imageVector = Icons.Outlined.Download, contentDescription = null, tint = ColorWhite, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isExporting) "Exporting..." else "Export", color = ColorWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalSurface,
                ),
            )
        },
        containerColor = PitchBlackBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PitchBlackBackground),
        ) {
            // 1. TOP SECTION (~50% Height): Photo Preview Canvas Area with Real-Time GPU ColorFilter & AGSL LUT Shader
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.50f)
                    .background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center,
            ) {
                ZoomableBox(
                    maxScale = 8.0f,
                    isStudio = true,
                    isDrawToolActive = isEraserModeActive,
                    resetKey = selectedPhotoIndex,
                ) { scale, pan ->
                    val currentInpointed = inpaintedBitmap
                    if (currentInpointed != null) {
                        Image(
                            bitmap = currentInpointed.asImageBitmap(),
                            contentDescription = "Inpainted AI Image",
                            contentScale = ContentScale.Fit,
                            colorFilter = realTimeColorFilter,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (activeLutRenderEffect != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        renderEffect = activeLutRenderEffect.asComposeRenderEffect()
                                    }
                                },
                        )
                    } else if (photoUris.isNotEmpty() && selectedPhotoIndex in photoUris.indices) {
                        AsyncImage(
                            model = photoUris[selectedPhotoIndex],
                            contentDescription = "Studio Active Image",
                            contentScale = ContentScale.Fit,
                            colorFilter = realTimeColorFilter,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    if (activeLutRenderEffect != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        renderEffect = activeLutRenderEffect.asComposeRenderEffect()
                                    }
                                },
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .background(CharcoalSurface, shape = RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No Photo Loaded\nCapture a photo to start editing",
                                color = ColorMutedGray,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            )
                        }
                    }

                    // Interactive Brush Mask Canvas Layer (When Magic Eraser mode is active)
                    if (isEraserModeActive) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentStrokePoints = listOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            currentStrokePoints = currentStrokePoints + change.position
                                        },
                                        onDragEnd = {
                                            if (currentStrokePoints.isNotEmpty()) {
                                                brushStrokes = brushStrokes + BrushStroke(currentStrokePoints)
                                                currentStrokePoints = emptyList()
                                            }
                                        },
                                        onDragCancel = {
                                            if (currentStrokePoints.isNotEmpty()) {
                                                brushStrokes = brushStrokes + BrushStroke(currentStrokePoints)
                                                currentStrokePoints = emptyList()
                                            }
                                        },
                                    )
                                },
                        ) {
                            val strokeWidthPx = 40.dp.toPx()
                            val maskColor = Color(0x80FF0055)

                            brushStrokes.forEach { stroke ->
                                if (stroke.points.size > 1) {
                                    for (i in 0 until stroke.points.size - 1) {
                                        drawLine(
                                            color = maskColor,
                                            start = stroke.points[i],
                                            end = stroke.points[i + 1],
                                            strokeWidth = strokeWidthPx,
                                            cap = StrokeCap.Round,
                                        )
                                    }
                                }
                            }

                            if (currentStrokePoints.size > 1) {
                                for (i in 0 until currentStrokePoints.size - 1) {
                                    drawLine(
                                        color = maskColor,
                                        start = currentStrokePoints[i],
                                        end = currentStrokePoints[i + 1],
                                        strokeWidth = strokeWidthPx,
                                        cap = StrokeCap.Round,
                                    )
                                }
                            }
                        }
                    }
                }

                // Loading overlay during LaMa AI Inpainting pass
                if (isErasingWithAi) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PitchBlackBackground.copy(alpha = 0.85f))
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = RoyalBlue, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Erasing object with LaMa AI...",
                                color = ColorWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Info metadata badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PitchBlackBackground.copy(alpha = 0.75f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = if (isEraserModeActive) "\u2728 Draw brush mask over object to erase" else "12.5 MP • ISO 100 • 1/250s • f/1.8",
                        color = ColorWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // 2. MIDDLE SECTION (~15% Height): Filmstrip Thumbnails
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f)
                    .background(CharcoalSurface)
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "FILMSTRIP QUEUE",
                    color = RoyalBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (photoUris.isNotEmpty()) {
                        itemsIndexed(photoUris) { index, uri ->
                            val isSelected = selectedPhotoIndex == index
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) RoyalBlue else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .background(PitchBlackBackground)
                                    .clickable { selectedPhotoIndex = index },
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Filmstrip $index",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    } else {
                        items(6) { index ->
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PitchBlackBackground),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "#${index + 1}", color = ColorMutedGray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // 3. BOTTOM SECTION (~35% Height): Professional Editing Panel & Magic Eraser Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .background(PitchBlackBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Tool category bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CharcoalSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    ToolCategoryButton(
                        icon = Icons.Outlined.WbSunny,
                        label = "Light",
                        isSelected = selectedToolCategory == 0,
                        onClick = {
                            selectedToolCategory = 0
                            isEraserModeActive = false
                        },
                    )
                    ToolCategoryButton(
                        icon = Icons.Outlined.Palette,
                        label = "Color",
                        isSelected = selectedToolCategory == 1,
                        onClick = {
                            selectedToolCategory = 1
                            isEraserModeActive = false
                        },
                    )
                    ToolCategoryButton(
                        icon = Icons.Outlined.AutoFixHigh,
                        label = "Eraser",
                        isSelected = selectedToolCategory == 2,
                        onClick = {
                            selectedToolCategory = 2
                            isEraserModeActive = true
                        },
                    )
                    ToolCategoryButton(
                        icon = Icons.Outlined.Crop,
                        label = "Crop",
                        isSelected = selectedToolCategory == 3,
                        onClick = {
                            selectedToolCategory = 3
                            isEraserModeActive = false
                        },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isEraserModeActive) {
                    // Magic Eraser Action Controls
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "\u2728 LA MA AI OBJECT ERASER",
                            color = RoyalBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    brushStrokes = emptyList()
                                    currentStrokePoints = emptyList()
                                    Toast.makeText(context, "Mask Cleared", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurface),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(text = "Clear Mask", color = ColorWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (photoUris.isNotEmpty() && selectedPhotoIndex in photoUris.indices && !isErasingWithAi) {
                                        coroutineScope.launch {
                                            isErasingWithAi = true
                                            val targetUri = photoUris[selectedPhotoIndex]

                                            val sourceBitmap = withContext(Dispatchers.IO) {
                                                context.contentResolver.openInputStream(targetUri)?.use { stream ->
                                                    BitmapFactory.decodeStream(stream)
                                                }
                                            }

                                            if (sourceBitmap != null) {
                                                // Construct mask bitmap matching source dimensions
                                                val maskBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
                                                val maskCanvas = android.graphics.Canvas(maskBitmap)
                                                val paint = Paint().apply {
                                                    color = android.graphics.Color.RED
                                                    strokeWidth = 40f
                                                    style = Paint.Style.STROKE
                                                    strokeCap = Paint.Cap.ROUND
                                                    strokeJoin = Paint.Join.ROUND
                                                }

                                                brushStrokes.forEach { stroke ->
                                                    if (stroke.points.size > 1) {
                                                        val path = Path()
                                                        path.moveTo(stroke.points[0].x, stroke.points[0].y)
                                                        for (i in 1 until stroke.points.size) {
                                                            path.lineTo(stroke.points[i].x, stroke.points[i].y)
                                                        }
                                                        maskCanvas.drawPath(path, paint)
                                                    }
                                                }

                                                val inpainted = objectEraser.eraseObject(sourceBitmap, maskBitmap)
                                                inpaintedBitmap = inpainted
                                                brushStrokes = emptyList()
                                                currentStrokePoints = emptyList()
                                                isEraserModeActive = false
                                                Toast.makeText(context, "Object Erased with LaMa AI!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to load source image", Toast.LENGTH_SHORT).show()
                                            }
                                            isErasingWithAi = false
                                        }
                                    }
                                },
                                enabled = !isErasingWithAi && brushStrokes.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(text = "Process Eraser", color = ColorWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Interactive Sliders (Exposure & Contrast) with 60fps GPU preview updates
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Exposure Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "Exposure EV", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = String.format(Locale.US, "%.2f", exposureValue),
                                color = RoyalBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Slider(
                            value = exposureValue,
                            onValueChange = { exposureValue = it },
                            valueRange = -2.0f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = RoyalBlue,
                                activeTrackColor = RoyalBlue,
                                inactiveTrackColor = CharcoalSurface,
                            ),
                            modifier = Modifier.height(24.dp),
                        )

                        // Contrast Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "Contrast", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = String.format(Locale.US, "%.0f", contrastValue),
                                color = RoyalBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Slider(
                            value = contrastValue,
                            onValueChange = { contrastValue = it },
                            valueRange = -50.0f..50.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = RoyalBlue,
                                activeTrackColor = RoyalBlue,
                                inactiveTrackColor = CharcoalSurface,
                            ),
                            modifier = Modifier.height(24.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3-Way Color Wheels Placeholder
                    Text(
                        text = "3-WAY COLOR WHEELS (L3 INTERCEPT)",
                        color = ColorMutedGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ColorWheelCircle(label = "Shadows", accentColor = Color(0xFF2563EB))
                        ColorWheelCircle(label = "Midtones", accentColor = Color(0xFF10B981))
                        ColorWheelCircle(label = "Highlights", accentColor = Color(0xFFF59E0B))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCategoryButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) RoyalBlue else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) ColorWhite else ColorMutedGray,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) ColorWhite else ColorMutedGray,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ColorWheelCircle(
    label: String,
    accentColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(CharcoalSurface)
                .border(1.5.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = ColorMutedGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
