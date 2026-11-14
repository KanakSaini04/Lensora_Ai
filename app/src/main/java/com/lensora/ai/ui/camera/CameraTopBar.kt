package com.lensora.ai.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lensora.ai.camera.CameraController
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.RoyalBlue

@Composable
fun CameraTopBar(
    modifier: Modifier = Modifier,
    flashMode: CameraController.FlashMode = CameraController.FlashMode.AUTO,
    isGridVisible: Boolean = false,
    onFlashClick: () -> Unit = {},
    onTimerClick: () -> Unit = {},
    onGridClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Flash Toggle Button
        IconButton(onClick = onFlashClick) {
            val (flashIcon, flashTint) = when (flashMode) {
                CameraController.FlashMode.OFF -> Icons.Outlined.FlashOff to ColorMutedGray
                CameraController.FlashMode.ON -> Icons.Outlined.FlashOn to RoyalBlue
                CameraController.FlashMode.AUTO -> Icons.Outlined.FlashAuto to ColorWhite
            }
            Icon(
                imageVector = flashIcon,
                contentDescription = "Flash Mode: ${flashMode.name}",
                tint = flashTint,
            )
        }

        // Timer Button
        IconButton(onClick = onTimerClick) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = "Timer Toggle",
                tint = ColorWhite,
            )
        }

        // Grid Toggle Button
        IconButton(onClick = onGridClick) {
            Icon(
                imageVector = if (isGridVisible) Icons.Outlined.GridOn else Icons.Outlined.GridOff,
                contentDescription = "Grid Toggle",
                tint = if (isGridVisible) RoyalBlue else ColorMutedGray,
            )
        }

        // Settings Button
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Camera Settings",
                tint = ColorWhite,
            )
        }
    }
}
