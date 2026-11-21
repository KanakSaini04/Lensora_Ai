package com.lensora.ai.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.lensora.ai.ai.SceneClassifier
import com.lensora.ai.camera.CameraController
import com.lensora.ai.camera.CameraMode
import com.lensora.ai.data.GalleryRepository
import com.lensora.ai.ui.components.CameraSwipeDecision
import com.lensora.ai.ui.components.ZoomTransformMath
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue
import com.lensora.ai.ui.vibes.VibePreset
import com.lensora.ai.ui.vibes.VibeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onOpenQuickView: () -> Unit = {},
    onNavigateToStudio: () -> Unit = {},
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

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                ),
            )
        }
    }

    if (hasCameraPermission) {
        val cameraController = remember { CameraController(context) }
        val galleryRepository = remember { GalleryRepository(context) }
        val sceneClassifier = remember { SceneClassifier(context) }
        val cameraPrefs = remember {
            try {
                context.getSharedPreferences("lensora_camera_prefs", Context.MODE_PRIVATE)
            } catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.createDeviceProtectedStorageContext().getSharedPreferences("lensora_camera_prefs", Context.MODE_PRIVATE)
                } else {
                    context.getSharedPreferences("lensora_camera_prefs", Context.MODE_PRIVATE)
                }
            }
        }

        var recentPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }

        // Measured UI Exclusion Bounds for Swipe Gesture Evaluator
        var topBarBounds by remember { mutableStateOf<Rect?>(null) }
        var aiBadgeBounds by remember { mutableStateOf<Rect?>(null) }
        var zoomPillsBounds by remember { mutableStateOf<Rect?>(null) }
        var carouselBounds by remember { mutableStateOf<Rect?>(null) }
        var shutterRowBounds by remember { mutableStateOf<Rect?>(null) }

        // Persisted UI preferences restoration
        var selectedMode by remember {
            val modeName = cameraPrefs.getString("camera_mode", CameraMode.PHOTO.name)
            mutableStateOf(try { CameraMode.valueOf(modeName ?: "PHOTO") } catch (e: Exception) { CameraMode.PHOTO })
        }
        var customModeName by remember { mutableStateOf<String?>(null) }
        var isMoreSheetVisible by remember { mutableStateOf(false) }

        var selectedZoom by remember { mutableStateOf("1x") }
        var currentZoomRatio by remember { mutableFloatStateOf(1.0f) }

        // Pro Mode Manual Exposure State
        var proIsoValue by remember { mutableIntStateOf(400) }
        var proShutterNs by remember { mutableStateOf(4_000_000L) } // 1/250s
        var proEvValue by remember { mutableFloatStateOf(0.0f) }
        var isRawEnabled by remember { mutableStateOf(cameraPrefs.getBoolean("raw_dng_enabled", false)) }
        var isZebrasEnabled by remember { mutableStateOf(false) }
        var isPeakingEnabled by remember { mutableStateOf(false) }

        // Timer State & Countdown
        var timerMode by remember { mutableStateOf(TimerMode.OFF) }
        var countdownRemaining by remember { mutableIntStateOf(0) }

        // Video Recording State
        var isRecordingVideo by remember { mutableStateOf(false) }
        var recordingSeconds by remember { mutableIntStateOf(0) }

        var focusTapOffset by remember { mutableStateOf<Offset?>(null) }
        var isGridVisible by remember { mutableStateOf(cameraPrefs.getBoolean("grid_enabled", false)) }
        var flashMode by remember {
            val flashName = cameraPrefs.getString("flash_mode", CameraController.FlashMode.AUTO.name)
            mutableStateOf(try { CameraController.FlashMode.valueOf(flashName ?: "AUTO") } catch (e: Exception) { CameraController.FlashMode.AUTO })
        }
        var aiBadgeText by remember { mutableStateOf("\u2728 AI: Auto") }
        var coachingHintText by remember { mutableStateOf<String?>(null) }
        var detectedBoundingBox by remember { mutableStateOf<RectF?>(null) }
        var textureViewRef by remember { mutableStateOf<TextureView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                if (isRecordingVideo) {
                    cameraController.stopVideoRecording(onVideoSaved = { _, _ -> }, onError = {})
                }
                cameraController.closeCamera()
                sceneClassifier.close()
            }
        }

        LaunchedEffect(isRecordingVideo) {
            if (isRecordingVideo) {
                recordingSeconds = 0
                while (isActive && isRecordingVideo) {
                    delay(1000)
                    recordingSeconds++
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

        // Shutter Timer Countdown Tick Effect
        LaunchedEffect(countdownRemaining) {
            if (countdownRemaining > 0) {
                delay(1000)
                val next = countdownRemaining - 1
                countdownRemaining = next
                if (next == 0) {
                    cameraController.takePicture(
                        displayRotationDegrees = deviceRotationDegrees,
                        isRawEnabled = isRawEnabled && selectedMode == CameraMode.PRO,
                        vibePreset = VibeState.selectedVibe.name,
                        onPhotoSaved = { uri, msg ->
                            recentPhotos = (listOf(uri) + recentPhotos).distinct().take(10)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        },
                    )
                    timerMode = TimerMode.OFF
                }
            }
        }

        LaunchedEffect(hasCameraPermission) {
            if (hasCameraPermission) {
                recentPhotos = galleryRepository.getRecentPhotos(limit = 10)
                // Apply restored persistent settings to cameraController
                cameraController.setFlashMode(flashMode)
                cameraController.setCameraMode(selectedMode)
                cameraController.setRawCaptureEnabled(isRawEnabled)
                if (selectedMode == CameraMode.PRO) {
                    cameraController.setManualExposure(proIsoValue, proShutterNs)
                }
            }
        }

        // Periodic 500ms AI Scene Analysis & Execution Planning Loop (2 FPS on Dispatchers.Default)
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
                                val detectionResult = sceneClassifier.analyzeWithDetection(bitmap)
                                withContext(Dispatchers.Main) {
                                    aiBadgeText = detectionResult.label
                                    coachingHintText = detectionResult.coachingHint
                                    detectedBoundingBox = detectionResult.boundingBox
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Error during live AI director preview sampling", e)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlackBackground),
        ) {
            // Pure Raw Camera2 Viewfinder (Mirrored horizontally for front camera)
            CameraPreview(
                cameraController = cameraController,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = if (cameraController.isFrontCamera) -1f else 1f
                    },
                onTextureViewCreated = { view ->
                    textureViewRef = view
                },
            )

            // 2.39:1 Cinematic Aspect Ratio Pure Black Letterboxing Overlay (Top 12% & Bottom 12% bars, 76% center viewfinder)
            if (selectedMode == CameraMode.CINEMATIC && customModeName == null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val screenWidth = size.width
                    val screenHeight = size.height
                    val barHeight = screenHeight * 0.12f // Top 12% and Bottom 12% pure black cinematic bars

                    // Top Pure Black Letterbox Bar (Top 12%)
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(0f, 0f),
                        size = Size(screenWidth, barHeight),
                    )
                    // Bottom Pure Black Letterbox Bar (Bottom 12%)
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(0f, screenHeight - barHeight),
                        size = Size(screenWidth, barHeight),
                    )
                }
            }

            // 3x3 Canvas Grid Overlay
            if (isGridVisible) {
                GridOverlay(modifier = Modifier.fillMaxSize())
            }

            // Large Center Shutter Timer Countdown Overlay (3... 2... 1...)
            if (countdownRemaining > 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$countdownRemaining",
                        color = ColorWhite,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Animated Focus Ring Overlay
            FocusRingOverlay(tapOffset = focusTapOffset)

            // Transparent Gesture Layer for Pinch-to-Zoom, Tap-to-Focus & Horizontal Tab Swipe Gesture
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var dragStartPos = down.position
                            var totalDx = 0f
                            var totalDy = 0f
                            var maxPointerCount = 1

                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.size > maxPointerCount) {
                                    maxPointerCount = event.changes.size
                                }

                                val change = event.changes.firstOrNull()
                                if (change != null && change.pressed) {
                                    totalDx += (change.position.x - change.previousPosition.x)
                                    totalDy += (change.position.y - change.previousPosition.y)
                                    if (kotlin.math.abs(totalDx) > (2.0f * kotlin.math.abs(totalDy)) && totalDx < -80f && maxPointerCount == 1) {
                                        change.consume()
                                    }
                                }

                                if (event.changes.all { !it.pressed }) {
                                    val excludedRects = listOfNotNull(topBarBounds, aiBadgeBounds, zoomPillsBounds, carouselBounds, shutterRowBounds)
                                    val decision = ZoomTransformMath.evaluateCameraSwipeGesture(
                                        startX = dragStartPos.x,
                                        startY = dragStartPos.y,
                                        totalDx = totalDx,
                                        totalDy = totalDy,
                                        pointerCount = maxPointerCount,
                                        screenWidth = size.width.toFloat(),
                                        screenHeight = size.height.toFloat(),
                                        excludedRects = excludedRects,
                                    )
                                    if (decision == CameraSwipeDecision.NAVIGATE_NEXT_TAB) {
                                        onNavigateToStudio()
                                    }
                                    break
                                }
                            }
                        }
                    }
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

            // Top Overlays Column: Top Bar -> AI Badge (Stacked cleanly to clear status bar / cutout and eliminate overlap at all font scales)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        topBarBounds = coordinates.boundsInRoot()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CameraTopBar(
                    flashMode = flashMode,
                    isGridVisible = isGridVisible,
                    timerMode = timerMode,
                    onFlashClick = {
                        val nextFlash = when (flashMode) {
                            CameraController.FlashMode.OFF -> CameraController.FlashMode.ON
                            CameraController.FlashMode.ON -> CameraController.FlashMode.AUTO
                            CameraController.FlashMode.AUTO -> CameraController.FlashMode.OFF
                        }
                        flashMode = nextFlash
                        cameraController.setFlashMode(nextFlash)
                        cameraPrefs.edit().putString("flash_mode", nextFlash.name).apply()
                        Toast.makeText(context, "Flash: ${nextFlash.name}", Toast.LENGTH_SHORT).show()
                    },
                    onTimerClick = {
                        timerMode = timerMode.next()
                        Toast.makeText(context, "Timer: ${timerMode.displayName}", Toast.LENGTH_SHORT).show()
                    },
                    onVibeClick = {
                        val nextVibe = when (VibeState.selectedVibe) {
                            VibePreset.NONE -> VibePreset.CINEMATIC
                            VibePreset.CINEMATIC -> VibePreset.LEICA
                            VibePreset.LEICA -> VibePreset.ROYAL_GOLD
                            else -> VibePreset.NONE
                        }
                        VibeState.selectedVibe = nextVibe
                        Toast.makeText(context, "Vibe LUT: ${nextVibe.displayName}", Toast.LENGTH_SHORT).show()
                    },
                    onGridClick = {
                        isGridVisible = !isGridVisible
                        cameraPrefs.edit().putBoolean("grid_enabled", isGridVisible).apply()
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

                val badgeText = if (isRecordingVideo) {
                    val mins = recordingSeconds / 60
                    val secs = recordingSeconds % 60
                    String.format(Locale.US, "\uD83D\uDD34 REC %02d:%02d", mins, secs)
                } else {
                    when {
                        VibeState.selectedVibe != VibePreset.NONE -> "\uD83C\uDFAC Vibe: ${VibeState.selectedVibe.displayName}"
                        customModeName != null -> "\u2728 Mode: $customModeName"
                        selectedMode == CameraMode.CINEMATIC -> "\uD83C\uDFAC 24fps 2.39:1 Cinematic"
                        else -> aiBadgeText
                    }
                }

                AiBadge(
                    statusText = badgeText,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        aiBadgeBounds = coordinates.boundsInRoot()
                    },
                )
            }

            // Bottom Overlays: Coaching Hint + Pro Controls + Zoom Pills + Mode Carousel + Shutter & Thumbnail Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Live AI Director Composition Coaching Hint Pill (Only visible when a real bounding box is detected and hint is non-null)
                val currentHint = coachingHintText
                if (!currentHint.isNullOrEmpty() && detectedBoundingBox != null && !isRecordingVideo) {
                    Surface(
                        color = CharcoalSurface.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentHint,
                                color = ColorWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                // Pro Mode HUD Controls (Only visible when PRO mode is selected)
                if (selectedMode == CameraMode.PRO && customModeName == null && !isRecordingVideo) {
                    ProModeControls(
                        isoValue = proIsoValue,
                        shutterSpeedNs = proShutterNs,
                        evValue = proEvValue,
                        isRawEnabled = isRawEnabled,
                        isZebrasEnabled = isZebrasEnabled,
                        isPeakingEnabled = isPeakingEnabled,
                        onIsoChanged = { iso ->
                            proIsoValue = iso
                            cameraController.setManualExposure(iso, proShutterNs)
                        },
                        onShutterSpeedChanged = { shutterNs ->
                            proShutterNs = shutterNs
                            cameraController.setManualExposure(proIsoValue, shutterNs)
                        },
                        onEvChanged = { ev ->
                            proEvValue = ev
                            cameraController.setExposureCompensation(ev)
                        },
                        onRawToggled = { raw ->
                            isRawEnabled = raw
                            cameraPrefs.edit().putBoolean("raw_dng_enabled", raw).apply()
                            cameraController.setRawCaptureEnabled(raw)
                            Toast.makeText(context, if (raw) "RAW DNG Enabled" else "RAW DNG Disabled", Toast.LENGTH_SHORT).show()
                        },
                        onZebrasToggled = { zebras ->
                            isZebrasEnabled = zebras
                            if (zebras) isPeakingEnabled = false
                            Toast.makeText(context, if (zebras) "Zebras Overlay Enabled" else "Zebras Overlay Disabled", Toast.LENGTH_SHORT).show()
                        },
                        onPeakingToggled = { peaking ->
                            isPeakingEnabled = peaking
                            if (peaking) isZebrasEnabled = false
                            Toast.makeText(context, if (peaking) "Focus Peaking Enabled" else "Focus Peaking Disabled", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }

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
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .onGloballyPositioned { coordinates ->
                            zoomPillsBounds = coordinates.boundsInRoot()
                        },
                )

                // Horizontal Scrollable Camera Mode Carousel
                CameraModeCarousel(
                    selectedMode = selectedMode,
                    customModeName = customModeName,
                    onModeSelected = { mode ->
                        if (!isRecordingVideo) {
                            customModeName = null
                            selectedMode = mode
                            cameraController.setCameraMode(mode)
                            cameraPrefs.edit().putString("camera_mode", mode.name).apply()
                            if (mode == CameraMode.PRO) {
                                cameraController.setManualExposure(proIsoValue, proShutterNs)
                            }
                        }
                    },
                    onMoreClick = {
                        if (!isRecordingVideo) {
                            isMoreSheetVisible = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .onGloballyPositioned { coordinates ->
                            carouselBounds = coordinates.boundsInRoot()
                        },
                )

                // Shutter Controls Row (Gallery Thumbnail + Shutter Button + Camera Flip Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .onGloballyPositioned { coordinates ->
                            shutterRowBounds = coordinates.boundsInRoot()
                        },
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
                            .clickable { if (!isRecordingVideo) onOpenQuickView() },
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

                    // Refined Obsidian Royal Multi-Ring Camera Shutter Button
                    val isVideoRecordingMode = (selectedMode == CameraMode.VIDEO || selectedMode == CameraMode.CINEMATIC) && customModeName == null
                    val shutterColor = if (isVideoRecordingMode) Color(0xFFDC2626) else ColorWhite
                    val haptic = LocalHapticFeedback.current
                    var isShutterPressed by remember { mutableStateOf(false) }

                    val shutterScale by animateFloatAsState(
                        targetValue = if (isShutterPressed || isRecordingVideo) 0.88f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "ShutterSpringScale",
                    )

                    // Outer Tactile Ring (2dp stroke, 82dp size)
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .border(width = 2.dp, color = ColorWhite.copy(alpha = 0.7f), shape = CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Inner Tactile Shutter Core
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .graphicsLayer {
                                    scaleX = shutterScale
                                    scaleY = shutterScale
                                }
                                .border(width = 1.5.dp, color = ColorWhite.copy(alpha = 0.4f), shape = CircleShape)
                                .clip(CircleShape)
                                .background(shutterColor)
                                .clickable {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}

                                    if (isVideoRecordingMode) {
                                        if (isRecordingVideo) {
                                            cameraController.stopVideoRecording(
                                                onVideoSaved = { uri, msg ->
                                                    isRecordingVideo = false
                                                    recentPhotos = (listOf(uri) + recentPhotos).distinct().take(10)
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    isRecordingVideo = false
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                },
                                            )
                                        } else {
                                            cameraController.startVideoRecording(
                                                hasAudioPermission = hasAudioPermission,
                                                displayRotationDegrees = deviceRotationDegrees,
                                                onRecordingStarted = {
                                                    isRecordingVideo = true
                                                    Toast.makeText(context, "Recording started...", Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    isRecordingVideo = false
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                },
                                            )
                                        }
                                    } else {
                                        if (timerMode != TimerMode.OFF && countdownRemaining == 0) {
                                            countdownRemaining = timerMode.seconds
                                        } else if (countdownRemaining == 0) {
                                            cameraController.takePicture(
                                                displayRotationDegrees = deviceRotationDegrees,
                                                isRawEnabled = isRawEnabled && selectedMode == CameraMode.PRO,
                                                vibePreset = VibeState.selectedVibe.name,
                                                onPhotoSaved = { uri, msg ->
                                                    recentPhotos = (listOf(uri) + recentPhotos).distinct().take(10)
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                },
                                            )
                                        }
                                    }
                                },
                        )
                    }

                    // Camera Flip Button (Front / Back camera switch)
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
                            .clickable {
                                if (!isRecordingVideo) {
                                    val textureView = textureViewRef
                                    val surfaceTexture = textureView?.surfaceTexture
                                    if (textureView != null && surfaceTexture != null) {
                                        cameraController.switchCamera(surfaceTexture, textureView.width, textureView.height)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cameraswitch,
                            contentDescription = "Switch Camera Lens Facing",
                            tint = ColorWhite,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }

            // More Modes Bottom Sheet
            if (isMoreSheetVisible) {
                MoreModesSheet(
                    onDismissRequest = { isMoreSheetVisible = false },
                    onModeSelected = { modeName ->
                        customModeName = modeName
                        cameraController.setAdvancedMode(modeName) { feedback ->
                            Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
                        }
                    },
                )
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
                    text = "Lensora_Ai requires camera and microphone permissions to capture video.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorMutedGray,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO,
                            ),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "Grant Permissions",
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
            .background(CharcoalSurface.copy(alpha = 0.65f))
            .border(width = 1.dp, color = ColorWhite.copy(alpha = 0.25f), shape = CircleShape)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = statusText,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "AiBadgeTextAnimation",
        ) { text ->
            Text(
                text = text,
                color = ColorWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
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
