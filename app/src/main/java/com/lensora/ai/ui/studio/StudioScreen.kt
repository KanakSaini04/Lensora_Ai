package com.lensora.ai.ui.studio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lensora.ai.data.GalleryRepository
import com.lensora.ai.ui.components.ZoomTransformMath
import com.lensora.ai.ui.components.ZoomableBox
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    onNavigateBackToCamera: () -> Unit = {},
) {
    var studioTabMode by rememberSaveable { mutableStateOf("PHOTO") }

    if (studioTabMode == "VIDEO") {
        Column(modifier = Modifier.fillMaxSize()) {
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
            onNavigateBackToCamera = onNavigateBackToCamera,
            onSwitchToVideoTab = { studioTabMode = "VIDEO" },
            onSwitchToPhotoTab = { studioTabMode = "PHOTO" },
            studioTabMode = studioTabMode,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioPhotoContent(
    onNavigateBackToCamera: () -> Unit,
    onSwitchToVideoTab: () -> Unit,
    onSwitchToPhotoTab: () -> Unit,
    studioTabMode: String,
    viewModel: StudioViewModel = viewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val galleryRepository = remember { GalleryRepository(context) }
    val objectEraser = remember { ObjectEraser(context) }
    val density = LocalDensity.current.density

    var photoUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var selectedPhotoIndex by rememberSaveable { mutableIntStateOf(0) }

    // Small primitive state variables restored via rememberSaveable
    var selectedToolCategory by rememberSaveable { mutableIntStateOf(0) }
    var exposureValue by rememberSaveable { mutableFloatStateOf(0.0f) }
    var contrastValue by rememberSaveable { mutableFloatStateOf(0.0f) }
    var brushSizeDp by rememberSaveable { mutableFloatStateOf(30.0f) }
    var opacityFloat by rememberSaveable { mutableFloatStateOf(0.8f) }
    var isEraseModeActiveState by rememberSaveable { mutableStateOf(true) }

    var isIncludeTimestampEnabled by rememberSaveable { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isErasingWithAi by remember { mutableStateOf(false) }
    var inpaintedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isFilmstripExpanded by rememberSaveable { mutableStateOf(false) }
    var isComparePressed by remember { mutableStateOf(false) }

    // Brush state variables
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var isAdjustingBrushSize by remember { mutableStateOf(false) }
    var currentStrokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val strokesState by viewModel.strokesState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val isEraserModeActive = selectedToolCategory == 2

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
        currentStrokePoints = emptyList()
        viewModel.clearMaskAndHistory()
    }

    Scaffold(
        topBar = {
            Column {
                // Main Top Row
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onNavigateBackToCamera) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back to Camera",
                                tint = ColorWhite,
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(
                                onClick = { viewModel.undo() },
                                enabled = canUndo,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Undo,
                                    contentDescription = "Undo",
                                    tint = if (canUndo) ColorWhite else ColorMutedGray.copy(alpha = 0.4f),
                                )
                            }
                            IconButton(
                                onClick = { viewModel.redo() },
                                enabled = canRedo,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Redo,
                                    contentDescription = "Redo",
                                    tint = if (canRedo) ColorWhite else ColorMutedGray.copy(alpha = 0.4f),
                                )
                            }
                        }
                    },
                    actions = {
                        // Hold to Compare Icon Button
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isComparePressed = true
                                            tryAwaitRelease()
                                            isComparePressed = false
                                        },
                                    )
                                }
                                .padding(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RemoveRedEye,
                                contentDescription = "Hold to Compare",
                                tint = if (isComparePressed) RoyalBlue else ColorWhite,
                            )
                        }

                        // Export Button
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalSurface),
                )

                // Secondary Utility Sub-Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CharcoalSurface)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // PHOTO / VIDEO Segmented Toggle
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
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("PHOTO", color = ColorWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (studioTabMode == "VIDEO") RoyalBlue else Color.Transparent)
                                .clickable { onSwitchToVideoTab() }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("VIDEO", color = ColorWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Time Stamp Toggle Switch
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Time", color = ColorMutedGray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Switch(
                            checked = isIncludeTimestampEnabled,
                            onCheckedChange = { isIncludeTimestampEnabled = it },
                            modifier = Modifier.scale(0.65f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ColorWhite,
                                checkedTrackColor = RoyalBlue,
                            ),
                        )
                    }

                    // Reset All Edits Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PitchBlackBackground)
                            .clickable {
                                selectedToolCategory = 0
                                exposureValue = 0.0f
                                contrastValue = 0.0f
                                brushSizeDp = 30.0f
                                opacityFloat = 0.8f
                                isEraseModeActiveState = true
                                inpaintedBitmap = null
                                currentStrokePoints = emptyList()
                                VibeState.selectedVibe = VibePreset.NONE
                                viewModel.clearMaskAndHistory()
                                Toast.makeText(context, "Reset All Edits", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("Reset", color = ColorWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = PitchBlackBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .background(PitchBlackBackground),
        ) {
            // 1. TOP SECTION (~65% Height): Main Photo Canvas Area
            var canvasSize by remember { mutableStateOf(Size.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.68f)
                    .background(Color(0xFF0A0A0A))
                    .onSizeChanged { canvasSize = it.toSize() },
                contentAlignment = Alignment.Center,
            ) {
                ZoomableBox(
                    maxScale = 8.0f,
                    isStudio = true,
                    isDrawToolActive = isEraserModeActive,
                    resetKey = selectedPhotoIndex,
                ) { scale, pan ->
                    val currentInpainted = inpaintedBitmap
                    val displayBitmap = currentInpainted

                    if (isComparePressed) {
                        // Hold-to-Compare active: Render RAW original photo without filters/shaders/masks
                        if (photoUris.isNotEmpty() && selectedPhotoIndex in photoUris.indices) {
                            AsyncImage(
                                model = photoUris[selectedPhotoIndex],
                                contentDescription = "Raw Original Image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else if (displayBitmap != null) {
                        Image(
                            bitmap = displayBitmap.asImageBitmap(),
                            contentDescription = "Inpainted Image",
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

                    // Interactive Mask Canvas Layer (Only active when Eraser is selected and Hold-to-Compare is released)
                    if (isEraserModeActive && !isComparePressed) {
                        val currentBrushSizeDp by rememberUpdatedState(brushSizeDp)
                        val currentScale by rememberUpdatedState(scale)
                        val currentDensity by rememberUpdatedState(density)
                        val currentIsEraseMode by rememberUpdatedState(isEraseModeActiveState)

                        val localCanvasStrokeWidth = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(
                            screenDp = brushSizeDp,
                            density = density,
                            scale = scale,
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown()
                                        var activePoints = listOf(down.position)
                                        currentStrokePoints = activePoints
                                        currentTouchPoint = down.position

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val pointerCount = event.changes.size

                                            if (pointerCount >= 2) {
                                                // End stroke immediately when 2+ pointers touch down
                                                if (activePoints.isNotEmpty()) {
                                                    val localRadiusPx = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(
                                                        screenDp = currentBrushSizeDp,
                                                        density = currentDensity,
                                                        scale = currentScale,
                                                    ) / 2f
                                                    viewModel.addStroke(
                                                        StudioStroke(
                                                            points = activePoints,
                                                            isEraseMode = currentIsEraseMode,
                                                            radiusCanvasPx = localRadiusPx,
                                                        ),
                                                    )
                                                    currentStrokePoints = emptyList()
                                                    activePoints = emptyList()
                                                }
                                                currentTouchPoint = null
                                                break
                                            }

                                            val change = event.changes.firstOrNull()
                                            if (change != null && change.pressed) {
                                                change.consume()
                                                activePoints = activePoints + change.position
                                                currentStrokePoints = activePoints
                                                currentTouchPoint = change.position
                                            }

                                            if (event.changes.all { !it.pressed }) {
                                                if (activePoints.isNotEmpty()) {
                                                    val localRadiusPx = ZoomTransformMath.calculateLocalCanvasBrushWidthPx(
                                                        screenDp = currentBrushSizeDp,
                                                        density = currentDensity,
                                                        scale = currentScale,
                                                    ) / 2f
                                                    viewModel.addStroke(
                                                        StudioStroke(
                                                            points = activePoints,
                                                            isEraseMode = currentIsEraseMode,
                                                            radiusCanvasPx = localRadiusPx,
                                                        ),
                                                    )
                                                    currentStrokePoints = emptyList()
                                                    activePoints = emptyList()
                                                }
                                                currentTouchPoint = null
                                                break
                                            }
                                        }
                                    }
                                },
                        ) {
                            val eraseColor = Color(0x80FF0055).copy(alpha = opacityFloat)

                            // Render completed history strokes
                            strokesState.forEach { stroke ->
                                val strokeWidth = stroke.radiusCanvasPx * 2f
                                if (stroke.points.size > 1) {
                                    for (i in 0 until stroke.points.size - 1) {
                                        drawLine(
                                            color = if (stroke.isEraseMode) eraseColor else Color.Transparent,
                                            start = stroke.points[i],
                                            end = stroke.points[i + 1],
                                            strokeWidth = strokeWidth,
                                            cap = StrokeCap.Round,
                                            blendMode = if (stroke.isEraseMode) BlendMode.SrcOver else BlendMode.Clear,
                                        )
                                    }
                                } else if (stroke.points.size == 1) {
                                    drawCircle(
                                        color = if (stroke.isEraseMode) eraseColor else Color.Transparent,
                                        center = stroke.points[0],
                                        radius = stroke.radiusCanvasPx,
                                        blendMode = if (stroke.isEraseMode) BlendMode.SrcOver else BlendMode.Clear,
                                    )
                                }
                            }

                            // Render active in-progress stroke
                            if (currentStrokePoints.size > 1) {
                                for (i in 0 until currentStrokePoints.size - 1) {
                                    drawLine(
                                        color = if (isEraseModeActiveState) eraseColor else Color.Transparent,
                                        start = currentStrokePoints[i],
                                        end = currentStrokePoints[i + 1],
                                        strokeWidth = localCanvasStrokeWidth,
                                        cap = StrokeCap.Round,
                                        blendMode = if (isEraseModeActiveState) BlendMode.SrcOver else BlendMode.Clear,
                                    )
                                }
                            }

                            // Live Circle Brush Preview under finger while drawing
                            val touchPoint = currentTouchPoint
                            if (touchPoint != null) {
                                drawCircle(
                                    color = ColorWhite,
                                    center = touchPoint,
                                    radius = localCanvasStrokeWidth / 2f,
                                    style = Stroke(width = 2.dp.toPx()),
                                )
                            }
                        }
                    }
                }

                // Live Circle Brush Preview Centered on Screen while adjusting size slider
                if (isEraserModeActive && isAdjustingBrushSize) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radiusPx = (brushSizeDp * density) / 2f
                        drawCircle(
                            color = ColorWhite,
                            center = center,
                            radius = radiusPx,
                            style = Stroke(width = 2.5.dp.toPx()),
                        )
                        drawCircle(
                            color = RoyalBlue.copy(alpha = 0.4f),
                            center = center,
                            radius = radiusPx,
                        )
                    }
                }

                // LaMa AI Inpainting Progress Loading Overlay
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
            }

            // 2. BOTTOM SECTION: Filmstrip Queue & Editing Tool Panels
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.32f)
                    .background(PitchBlackBackground)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                // Collapsible Filmstrip Queue Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isFilmstripExpanded = !isFilmstripExpanded }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "FILMSTRIP QUEUE",
                        color = RoyalBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                    Icon(
                        imageVector = if (isFilmstripExpanded) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                        contentDescription = "Toggle Filmstrip",
                        tint = ColorMutedGray,
                        modifier = Modifier.size(16.dp),
                    )
                }

                AnimatedVisibility(visible = isFilmstripExpanded) {
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (photoUris.isNotEmpty()) {
                            itemsIndexed(photoUris) { index, uri ->
                                val isSelected = selectedPhotoIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) RoyalBlue else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .background(PitchBlackBackground)
                                        .clickable {
                                            selectedPhotoIndex = index
                                            inpaintedBitmap = null
                                            currentStrokePoints = emptyList()
                                            viewModel.clearMaskAndHistory()
                                        },
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Filmstrip $index",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                // Bottom Tool Category Tabs (Light, Color, Eraser, Crop)
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
                        },
                    )
                    ToolCategoryButton(
                        icon = Icons.Outlined.Palette,
                        label = "Color",
                        isSelected = selectedToolCategory == 1,
                        onClick = {
                            selectedToolCategory = 1
                        },
                    )
                    ToolCategoryButton(
                        icon = Icons.Outlined.AutoFixHigh,
                        label = "Eraser",
                        isSelected = selectedToolCategory == 2,
                        onClick = {
                            selectedToolCategory = 2
                        },
                    )
                    ToolCategoryButton(
                        icon = Icons.Outlined.Crop,
                        label = "Crop",
                        isSelected = selectedToolCategory == 3,
                        onClick = {
                            selectedToolCategory = 3
                        },
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Tool Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (selectedToolCategory) {
                        0 -> {
                            // Light Panel (Exposure & Contrast Sliders)
                            Text("Light Adjustments", color = ColorWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Exposure EV", color = ColorMutedGray, fontSize = 11.sp)
                                Text(String.format(Locale.US, "%.2f", exposureValue), color = RoyalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = exposureValue,
                                onValueChange = { exposureValue = it },
                                valueRange = -2.0f..2.0f,
                                colors = SliderDefaults.colors(thumbColor = RoyalBlue, activeTrackColor = RoyalBlue),
                                modifier = Modifier.height(24.dp),
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Contrast", color = ColorMutedGray, fontSize = 11.sp)
                                Text(String.format(Locale.US, "%.0f", contrastValue), color = RoyalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = contrastValue,
                                onValueChange = { contrastValue = it },
                                valueRange = -50.0f..50.0f,
                                colors = SliderDefaults.colors(thumbColor = RoyalBlue, activeTrackColor = RoyalBlue),
                                modifier = Modifier.height(24.dp),
                            )
                        }

                        1 -> {
                            // Color Panel (Vibe Presets & 3-Way Color Wheels SOON)
                            Text("Color Science Presets", color = ColorWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                VibeChip("NONE", "Original", VibeState.selectedVibe == VibePreset.NONE) { VibeState.selectedVibe = VibePreset.NONE }
                                VibeChip("CINEMATIC", "Teal/Orange", VibeState.selectedVibe == VibePreset.CINEMATIC) { VibeState.selectedVibe = VibePreset.CINEMATIC }
                                VibeChip("LEICA", "Leica B&W", VibeState.selectedVibe == VibePreset.LEICA) { VibeState.selectedVibe = VibePreset.LEICA }
                                VibeChip("ROYAL_GOLD", "Royal Gold", VibeState.selectedVibe == VibePreset.ROYAL_GOLD) { VibeState.selectedVibe = VibePreset.ROYAL_GOLD }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("3-WAY COLOR WHEELS (SOON)", color = RoyalBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Interactive shadow, midtone, and highlight color wheels coming in a future DLC update.", color = ColorMutedGray, fontSize = 10.sp)
                                }
                            }
                        }

                        2 -> {
                            // Eraser Panel (Size, Opacity, Erase/Restore Toggle, Clear & Process Buttons)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Brush Size: ${brushSizeDp.toInt()} dp", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Opacity: ${(opacityFloat * 100).toInt()}%", color = ColorMutedGray, fontSize = 11.sp)
                            }

                            Slider(
                                value = brushSizeDp,
                                onValueChange = {
                                    isAdjustingBrushSize = true
                                    brushSizeDp = it
                                },
                                onValueChangeFinished = { isAdjustingBrushSize = false },
                                valueRange = 10.0f..100.0f,
                                colors = SliderDefaults.colors(thumbColor = RoyalBlue, activeTrackColor = RoyalBlue),
                                modifier = Modifier.height(24.dp),
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Slider(
                                value = opacityFloat,
                                onValueChange = { opacityFloat = it },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = RoyalBlue, activeTrackColor = RoyalBlue),
                                modifier = Modifier.height(24.dp),
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Erase / Restore Mode Segmented Toggle
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CharcoalSurface)
                                        .padding(2.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isEraseModeActiveState) RoyalBlue else Color.Transparent)
                                            .clickable { isEraseModeActiveState = true }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text("Erase", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (!isEraseModeActiveState) RoyalBlue else Color.Transparent)
                                            .clickable { isEraseModeActiveState = false }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text("Restore", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            viewModel.clearMaskAndHistory()
                                            currentStrokePoints = emptyList()
                                            Toast.makeText(context, "Mask Cleared", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CharcoalSurface),
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text("Clear", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                                        val maskBitmap = MaskComposer.buildMaskBitmap(
                                                            imageWidth = sourceBitmap.width,
                                                            imageHeight = sourceBitmap.height,
                                                            canvasSize = canvasSize,
                                                            strokes = strokesState,
                                                        )

                                                        val inpainted = objectEraser.eraseObject(sourceBitmap, maskBitmap)
                                                        inpaintedBitmap = inpainted
                                                        viewModel.clearMaskAndHistory()
                                                        currentStrokePoints = emptyList()
                                                        Toast.makeText(context, "Object Erased with LaMa AI!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to load source image", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isErasingWithAi = false
                                                }
                                            }
                                        },
                                        enabled = !isErasingWithAi && strokesState.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text("Process", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Crop Panel (Informational Card)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("CROP & ASPECT RATIO (SOON)", color = RoyalBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Freehand cropping and aspect ratio presets (1:1, 4:5, 16:9) coming in a future DLC update.", color = ColorMutedGray, fontSize = 11.sp)
                                }
                            }
                        }
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
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) ColorWhite else ColorMutedGray,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
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
private fun VibeChip(
    id: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) RoyalBlue else CharcoalSurface)
            .border(width = 1.dp, color = if (isSelected) RoyalBlue else ColorWhite.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text = label, color = ColorWhite, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
    }
}
