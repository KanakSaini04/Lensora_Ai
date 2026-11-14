package com.lensora.ai.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.lensora.ai.ai.SceneClassifier
import com.lensora.ai.camera.CameraController
import com.lensora.ai.camera.CameraMode
import com.lensora.ai.data.GalleryRepository
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun CameraScreen(
    onOpenQuickView: () -> Unit = {},
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        val cameraController = remember { CameraController(context) }
        val galleryRepository = remember { GalleryRepository(context) }
        val sceneClassifier = remember { SceneClassifier(context) }

        var recentPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var selectedMode by remember { mutableStateOf(CameraMode.PHOTO) }
        var selectedZoom by remember { mutableStateOf("1x") }
        var currentZoomRatio by remember { mutableFloatStateOf(1.0f) }

        var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
        var isGridVisible by remember { mutableStateOf(false) }
        var flashMode by remember { mutableStateOf(CameraController.FlashMode.AUTO) }
        var aiBadgeText by remember { mutableStateOf("\u2728 AI: Auto") }
        var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                cameraController.closeCamera()
                sceneClassifier.close()
            }
        }

        LaunchedEffect(hasCameraPermission) {
            if (hasCameraPermission) {
                recentPhotos = galleryRepository.getRecentPhotos(limit = 10)
            }
        }

        // Periodic 500ms AI Scene Analysis Loop (2 FPS on Dispatchers.Default)
        LaunchedEffect(textureViewRef) {
            withContext(Dispatchers.Default) {
                sceneClassifier.initialize()
                while (isActive) {
                    delay(500)
                    try {
                        val view = textureViewRef
                        if (view != null && view.isAvailable) {
                            val bitmap = withContext(Dispatchers.Main) {
                                view.getBitmap(224, 224)
                            }
                            if (bitmap != null) {
                                val label = sceneClassifier.analyze(bitmap)
                                withContext(Dispatchers.Main) {
                                    aiBadgeText = label
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Error during AI preview analysis", e)
                    }
                }
            }
        }

        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }

        val deviceRotationDegrees = when (display?.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlackBackground),
        ) {
            // Edge-to-edge Live Camera Viewfinder
            CameraPreview(
                cameraController = cameraController,
                modifier = Modifier.fillMaxSize(),
                onTextureViewCreated = { view ->
                    textureViewRef = view
                },
            )

            // 3x3 Canvas Grid Overlay
            if (isGridVisible) {
                GridOverlay(modifier = Modifier.fillMaxSize())
            }

            // Animated Focus Ring Overlay
            FocusRingOverlay(tapOffset = focusTapOffset)

            // Transparent Gesture Layer for Pinch-to-Zoom & Tap-to-Focus
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoomChange, _ ->
                            currentZoomRatio = (currentZoomRatio * zoomChange).coerceIn(1.0f, 10.0f)
                            cameraController.setZoomRatio(currentZoomRatio)
                            selectedZoom = if (currentZoomRatio % 1f == 0f) {
                                "${currentZoomRatio.toInt()}x"
                            } else {
                                String.format(Locale.US, "%.1fx", currentZoomRatio)
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            focusTapOffset = offset
                            cameraController.triggerTapToFocus(
                                x = offset.x,
                                y = offset.y,
                                viewWidth = size.width,
                                viewHeight = size.height,
                            )
                        }
                    },
            )

            // Transparent Camera Top Bar Controls
            CameraTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                flashMode = flashMode,
                isGridVisible = isGridVisible,
                onFlashClick = {
                    val nextFlash = when (flashMode) {
                        CameraController.FlashMode.OFF -> CameraController.FlashMode.ON
                        CameraController.FlashMode.ON -> CameraController.FlashMode.AUTO
                        CameraController.FlashMode.AUTO -> CameraController.FlashMode.OFF
                    }
                    flashMode = nextFlash
                    cameraController.setFlashMode(nextFlash)
                    Toast.makeText(context, "Flash: ${nextFlash.name}", Toast.LENGTH_SHORT).show()
                },
                onTimerClick = {
                    Toast.makeText(context, "Timer: Off", Toast.LENGTH_SHORT).show()
                },
                onGridClick = {
                    isGridVisible = !isGridVisible
                    Toast.makeText(
                        context,
                        if (isGridVisible) "Grid Enabled" else "Grid Disabled",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                onSettingsClick = {
                    Toast.makeText(context, "Camera Settings", Toast.LENGTH_SHORT).show()
                },
            )

            // Frosted Glass AI Badge at Top Center (Driven dynamically by SceneClassifier)
            AiBadge(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp),
                statusText = aiBadgeText,
            )

            // Bottom Overlays: Zoom Pills + Mode Carousel + Shutter & Thumbnail Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Zoom Pills Container (0.6x, 1x, 2x, 3x)
                ZoomPills(
                    selectedZoom = selectedZoom,
                    onZoomSelected = { level ->
                        selectedZoom = level
                        val ratio = when (level) {
                            "0.6x" -> 1.0f
                            "2x" -> 2.0f
                            "3x" -> 3.0f
                            else -> 1.0f
                        }
                        currentZoomRatio = ratio
                        cameraController.setZoomRatio(ratio)
                        Toast.makeText(context, "Zoom: $level", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                // Horizontal Scrollable Camera Mode Carousel
                CameraModeCarousel(
                    selectedMode = selectedMode,
                    onModeSelected = { mode ->
                        selectedMode = mode
                        cameraController.setCameraMode(mode)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )

                // Shutter Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Single Circular Gallery Thumbnail Shortcut
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CharcoalSurface)
                            .border(
                                width = 2.dp,
                                color = ColorWhite,
                                shape = CircleShape,
                            )
                            .clickable { onOpenQuickView() },
                        contentAlignment = Alignment.Center,
                    ) {
                        val latestPhotoUri = recentPhotos.firstOrNull()
                        if (latestPhotoUri != null) {
                            AsyncImage(
                                model = latestPhotoUri,
                                contentDescription = "Last Captured Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = "Gallery Placeholder",
                                tint = ColorMutedGray,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    // Obsidian Royal Shutter Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(width = 4.dp, color = ColorWhite, shape = CircleShape)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(ColorWhite)
                            .clickable {
                                cameraController.takePicture(
                                    displayRotationDegrees = deviceRotationDegrees,
                                    onPhotoSaved = { uri, msg ->
                                        recentPhotos = (listOf(uri) + recentPhotos).distinct().take(10)
                                        Toast
                                            .makeText(context, msg, Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                    onError = { err ->
                                        Toast
                                            .makeText(context, err, Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                )
                            },
                    )

                    // Placeholder Spacer for balanced layout
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlackBackground),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = ColorMutedGray,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = ColorWhite,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lensora_Ai requires camera permission to capture live preview and photos.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorMutedGray,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "Grant Permission",
                        color = ColorWhite,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GridOverlay(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val lineOffsetColor = ColorWhite.copy(alpha = 0.35f)
        val strokeWidthPx = 1.dp.toPx()

        // Vertical 3x3 grid lines
        drawLine(
            color = lineOffsetColor,
            start = Offset(width / 3f, 0f),
            end = Offset(width / 3f, height),
            strokeWidth = strokeWidthPx,
        )
        drawLine(
            color = lineOffsetColor,
            start = Offset(width * 2f / 3f, 0f),
            end = Offset(width * 2f / 3f, height),
            strokeWidth = strokeWidthPx,
        )

        // Horizontal 3x3 grid lines
        drawLine(
            color = lineOffsetColor,
            start = Offset(0f, height / 3f),
            end = Offset(width, height / 3f),
            strokeWidth = strokeWidthPx,
        )
        drawLine(
            color = lineOffsetColor,
            start = Offset(0f, height * 2f / 3f),
            end = Offset(width, height * 2f / 3f),
            strokeWidth = strokeWidthPx,
        )
    }
}

@Composable
private fun FocusRingOverlay(
    tapOffset: Offset?,
    modifier: Modifier = Modifier,
) {
    if (tapOffset == null) return

    var ringScale by remember(tapOffset) { mutableFloatStateOf(1.6f) }
    var ringAlpha by remember(tapOffset) { mutableFloatStateOf(1.0f) }

    val animatedScale by animateFloatAsState(
        targetValue = ringScale,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "ringScaleAnimation",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = ringAlpha,
        animationSpec = tween(durationMillis = 800, delayMillis = 600),
        label = "ringAlphaAnimation",
    )

    LaunchedEffect(tapOffset) {
        ringScale = 1.0f
        delay(1000)
        ringAlpha = 0.0f
    }

    if (animatedAlpha > 0f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val radius = 36.dp.toPx() * animatedScale
            drawCircle(
                color = RoyalBlue.copy(alpha = animatedAlpha),
                center = tapOffset,
                radius = radius,
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun AiBadge(
    modifier: Modifier = Modifier,
    statusText: String = "\u2728 AI: Auto",
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(CharcoalSurface.copy(alpha = 0.75f))
            .border(width = 1.dp, color = ColorWhite.copy(alpha = 0.25f), shape = CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = statusText,
            color = ColorWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ZoomPills(
    selectedZoom: String,
    onZoomSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    zoomLevels: List<String> = listOf("0.6x", "1x", "2x", "3x"),
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(CharcoalSurface.copy(alpha = 0.8f))
            .border(width = 1.dp, color = ColorWhite.copy(alpha = 0.15f), shape = CircleShape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        zoomLevels.forEach { level ->
            val isSelected = level == selectedZoom
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) RoyalBlue else Color.Transparent)
                    .clickable { onZoomSelected(level) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = level,
                    color = ColorWhite,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}
