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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
fun CameraScreen() {
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
        var persistentPhotos by remember { mutableStateOf<List<Uri>>(emptyList()) }

        DisposableEffect(Unit) {
            onDispose {
                cameraController.closeCamera()
            }
        }

        LaunchedEffect(hasCameraPermission) {
            if (hasCameraPermission) {
                persistentPhotos = galleryRepository.getRecentPhotos(limit = 4)
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
            CameraPreview(
                cameraController = cameraController,
                modifier = Modifier.fillMaxSize(),
            )

            // Bottom Controls Bar (Persistent Filmstrip + Shutter Button)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Persistent Filmstrip (Last 4 Photos)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (persistentPhotos.isNotEmpty()) {
                        persistentPhotos.forEach { photoUri ->
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Persistent Photo Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.5.dp,
                                        color = ColorWhite,
                                        shape = RoundedCornerShape(10.dp),
                                    ),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CharcoalSurface)
                                .border(
                                    width = 1.5.dp,
                                    color = ColorWhite,
                                    shape = RoundedCornerShape(10.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = "Gallery Placeholder",
                                tint = ColorMutedGray,
                                modifier = Modifier.size(24.dp),
                            )
                        }
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
                                    persistentPhotos = (listOf(uri) + persistentPhotos).distinct().take(4)
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
