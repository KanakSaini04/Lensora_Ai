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
import androidx.compose.material.icons.outlined.Timer10
import androidx.compose.material.icons.outlined.Timer3
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

enum class TimerMode(val seconds: Int, val displayName: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_10(10, "10s");

    fun next(): TimerMode = when (this) {
        OFF -> SEC_3
        SEC_3 -> SEC_10
        SEC_10 -> OFF
    }
}

@Composable
fun CameraTopBar(
    modifier: Modifier = Modifier,
    flashMode: CameraController.FlashMode = CameraController.FlashMode.AUTO,
    isGridVisible: Boolean = false,
    timerMode: TimerMode = TimerMode.OFF,
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
            val (timerIcon, timerTint) = when (timerMode) {
                TimerMode.OFF -> Icons.Outlined.Timer to ColorMutedGray
                TimerMode.SEC_3 -> Icons.Outlined.Timer3 to RoyalBlue
                TimerMode.SEC_10 -> Icons.Outlined.Timer10 to RoyalBlue
            }
            Icon(
                imageVector = timerIcon,
                contentDescription = "Timer Mode: ${timerMode.displayName}",
                tint = timerTint,
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
