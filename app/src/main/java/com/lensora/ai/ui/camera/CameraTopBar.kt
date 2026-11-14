package com.lensora.ai.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lensora.ai.ui.theme.ColorWhite

@Composable
fun CameraTopBar(
    modifier: Modifier = Modifier,
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
        IconButton(onClick = onFlashClick) {
            Icon(
                imageVector = Icons.Outlined.FlashAuto,
                contentDescription = "Flash Toggle",
                tint = ColorWhite,
            )
        }

        IconButton(onClick = onTimerClick) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = "Timer Toggle",
                tint = ColorWhite,
            )
        }

        IconButton(onClick = onGridClick) {
            Icon(
                imageVector = Icons.Outlined.GridOn,
                contentDescription = "Grid Toggle",
                tint = ColorWhite,
            )
        }

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Camera Settings",
                tint = ColorWhite,
            )
        }
    }
}
