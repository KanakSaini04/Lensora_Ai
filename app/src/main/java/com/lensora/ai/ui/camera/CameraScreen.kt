package com.lensora.ai.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.lensora.ai.camera.CameraController
import com.lensora.ai.data.GalleryRepository
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue

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
        var recentPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }
        var selectedMode by remember { mutableStateOf(CameraMode.PHOTO.displayName) }
        var selectedZoom by remember { mutableStateOf("1x") }

        DisposableEffect(Unit) {
            onDispose {
                cameraController.closeCamera()
            }
        }

        LaunchedEffect(hasCameraPermission) {
            if (hasCameraPermission) {
                recentPhotos = galleryRepository.getRecentPhotos(limit = 10)
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
            )

            // Transparent Camera Top Bar
            CameraTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                onFlashClick = {
                    Toast.makeText(context, "Flash: Auto", Toast.LENGTH_SHORT).show()
                },
                onTimerClick = {
                    Toast.makeText(context, "Timer: Off", Toast.LENGTH_SHORT).show()
                },
                onGridClick = {
                    Toast.makeText(context, "Grid toggled", Toast.LENGTH_SHORT).show()
                },
                onSettingsClick = {
                    Toast.makeText(context, "Camera Settings", Toast.LENGTH_SHORT).show()
                },
            )

            // Frosted Glass AI Badge at Top Center
            AiBadge(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp)
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
                            "0.6x" -> 0.6f
                            "2x" -> 2.0f
                            "3x" -> 3.0f
                            else -> 1.0f
                        }
                        cameraController.setZoomRatio(ratio)
                        Toast.makeText(context, "Zoom: $level", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Horizontal Scrollable Camera Mode Carousel
                CameraModeCarousel(
                    selectedMode = selectedMode,
                    onModeSelected = { selectedMode = it },
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
private fun AiBadge(
    modifier: Modifier = Modifier,
    statusText: String = "✨ AI: Auto"
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(CharcoalSurface.copy(alpha = 0.75f))
            .border(width = 1.dp, color = ColorWhite.copy(alpha = 0.25f), shape = CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusText,
            color = ColorWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ZoomPills(
    selectedZoom: String,
    onZoomSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    zoomLevels: List<String> = listOf("0.6x", "1x", "2x", "3x")
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(CharcoalSurface.copy(alpha = 0.8f))
            .border(width = 1.dp, color = ColorWhite.copy(alpha = 0.15f), shape = CircleShape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        zoomLevels.forEach { level ->
            val isSelected = level == selectedZoom
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) RoyalBlue else Color.Transparent)
                    .clickable { onZoomSelected(level) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level,
                    color = ColorWhite,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
