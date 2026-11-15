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
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.lensora.ai.ai.AiPlanner
import com.lensora.ai.ai.SceneClassifier
import com.lensora.ai.camera.CameraController
import com.lensora.ai.camera.CameraMode
import com.lensora.ai.data.GalleryRepository
import com.lensora.ai.ui.studio.ImageEditor
import com.lensora.ai.ui.studio.LutShader
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
        val aiPlanner = remember { AiPlanner() }
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

        // Dynamic VN-Style Timestamp State
        var currentTimeString by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
            while (isActive) {
                currentTimeString = sdf.format(Date())
                delay(1000)
            }
        }

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
                                val label = sceneClassifier.analyze(bitmap)
                                val sceneType = AiPlanner.fromLabel(label)
                                val processingPlan = aiPlanner.createPlan(sceneType)
                                withContext(Dispatchers.Main) {
                                    aiBadgeText = label
                                    if (selectedMode != CameraMode.PRO && !isRecordingVideo) {
                                        cameraController.applyProcessingPlan(processingPlan)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Error during AI preview analysis", e)
                    }
                }
            }
        }

        // Real-Time Viewfinder AGSL LUT Shader Pass (With strict safe try-catch fallback)
        val cameraLutBitmap = remember(VibeState.selectedVibe) {
            if (VibeState.selectedVibe == VibePreset.CINEMATIC) {
                ImageEditor.getLutStripBitmap(context, VibePreset.CINEMATIC)
            } else null
        }

        val cameraLutRenderEffect = remember(cameraLutBitmap) {
            if (cameraLutBitmap != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    LutShader.createLutRenderEffect(cameraLutBitmap, 17f)
                } catch (t: Throwable) {
                    Log.w("CameraScreen", "Failed to create LutRenderEffect: ${t.localizedMessage}")
                    null
                }
            } else null
        }

        val activeRenderEffect = remember(selectedMode, isZebrasEnabled, isPeakingEnabled, cameraLutRenderEffect) {
            try {
                when {
                    selectedMode == CameraMode.PRO && isZebrasEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> AgslShaders.createZebrasEffect()
                    selectedMode == CameraMode.PRO && isPeakingEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> AgslShaders.createFocusPeakingEffect()
                    cameraLutRenderEffect != null -> cameraLutRenderEffect
                    else -> null
                }
            } catch (t: Throwable) {
                Log.w("CameraScreen", "AGSL RenderEffect creation failed (${t.localizedMessage}). Safe fallback to raw preview.", t)
                null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PitchBlackBackground),
        ) {
            // Live Camera Viewfinder (Mirrored horizontally for front camera + Safe AGSL RenderEffect)
            CameraPreview(
                cameraController = cameraController,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = if (cameraController.isFrontCamera) -1f else 1f
                        if (activeRenderEffect != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            try {
                                renderEffect = activeRenderEffect.asComposeRenderEffect()
                            } catch (t: Throwable) {
                                Log.w("CameraScreen", "Failed to apply graphicsLayer renderEffect: ${t.localizedMessage}")
                                renderEffect = null
                            }
                        }
                    },
                onTextureViewCreated = { view ->
                    textureViewRef = view
                },
            )

            // PHOTO Mode Thin White Aesthetic Border Overlay
            if (selectedMode == CameraMode.PHOTO && customModeName == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .border(width = 1.dp, color = ColorWhite.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)),
                )
            }

            // 2.39:1 Cinematic Aspect Ratio Pure Black Letterboxing Overlay (Only visible in CINEMATIC mode)
            if (selectedMode == CameraMode.CINEMATIC && customModeName == null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val screenWidth = size.width
                    val screenHeight = size.height
                    val targetHeight = screenWidth / 2.39f
                    val barHeight = (screenHeight - targetHeight) / 2f

                    if (barHeight > 0) {
                        // Top Pure Black Letterbox Bar
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(0f, 0f),
                            size = Size(screenWidth, barHeight),
                        )
                        // Bottom Pure Black Letterbox Bar
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(0f, screenHeight - barHeight),
                            size = Size(screenWidth, barHeight),
                        )
                    }
                }
            }

            // 3x3 Canvas Grid Overlay
            if (isGridVisible) {
                GridOverlay(modifier = Modifier.fillMaxSize())
            }

            // Glowing Cyan VN-Style Timestamp Overlay at Bottom-Left Corner
            if (currentTimeString.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 180.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CharcoalSurface.copy(alpha = 0.65f))
                        .border(width = 1.dp, color = Color(0xFF00E5FF).copy(alpha = 0.4f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "LENSORA • $currentTimeString",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    )
                }
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

            // Frosted Glass AI Badge / Recording Timer Overlay at Top Center (Padding 104.dp to prevent Top Bar Icon Overlap)
            if (isRecordingVideo) {
                val mins = recordingSeconds / 60
                val secs = recordingSeconds % 60
                val timerText = String.format(Locale.US, "\uD83D\uDD34 REC %02d:%02d", mins, secs)
                AiBadge(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 104.dp),
                    statusText = timerText,
                )
            } else {
                val badgeText = when {
                    VibeState.selectedVibe != VibePreset.NONE -> "\uD83C\uDFAC Vibe: ${VibeState.selectedVibe.displayName}"
                    customModeName != null -> "\u2728 Mode: $customModeName"
                    selectedMode == CameraMode.CINEMATIC -> "\uD83C\uDFAC 24fps 2.39:1 Cinematic"
                    else -> aiBadgeText
                }
                AiBadge(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 104.dp),
                    statusText = badgeText,
                )
            }

            // Bottom Overlays: Pro Controls (if PRO) + Zoom Pills + Mode Carousel + Shutter & Thumbnail Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Pro Mode HUD Controls (Only visible when PRO mode is selected)
                if (selectedMode == CameraMode.PRO && customModeName == null && !isRecordingVideo) {
                    ProModeControls(
                        isoValue = proIsoValue,
                        shutterSpeedNs = proShutterNs,
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
                    modifier = Modifier.padding(bottom = 12.dp),
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
                        .padding(bottom = 12.dp),
                )

                // Shutter Controls Row (Gallery Thumbnail + Shutter Button + Camera Flip Button)
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

                    // Obsidian Royal Shutter Button (Red in VIDEO & CINEMATIC modes, White in Photo modes)
                    val isVideoRecordingMode = (selectedMode == CameraMode.VIDEO || selectedMode == CameraMode.CINEMATIC) && customModeName == null
                    val shutterColor = if (isVideoRecordingMode) Color(0xFFDC2626) else ColorWhite

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(width = 4.dp, color = ColorWhite, shape = CircleShape)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(shutterColor)
                            .clickable {
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
