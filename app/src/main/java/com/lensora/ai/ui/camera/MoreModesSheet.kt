package com.lensora.ai.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Panorama
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue

data class ModeItem(
    val name: String,
    val icon: ImageVector,
    val isEnabled: Boolean = true,
)

val proToolsList = listOf(
    ModeItem("Astro", Icons.Outlined.Stars, isEnabled = true),
    ModeItem("Panorama", Icons.Outlined.Panorama, isEnabled = false),
    ModeItem("Tele-Cine", Icons.Outlined.Movie, isEnabled = false),
    ModeItem("Pro Macro", Icons.Outlined.Camera, isEnabled = true),
    ModeItem("Time-Lapse", Icons.Outlined.Timelapse, isEnabled = false),
    ModeItem("3D Spatial", Icons.Outlined.AutoAwesome, isEnabled = false),
    ModeItem("360 Sphere", Icons.Outlined.PhotoCamera, isEnabled = false),
    ModeItem("Stop Motion", Icons.Outlined.Videocam, isEnabled = false),
)

val signatureBrandsList = listOf(
    ModeItem("Leica", Icons.Outlined.FilterVintage, isEnabled = true),
    ModeItem("ZEISS", Icons.Outlined.AutoAwesome, isEnabled = true),
    ModeItem("Hasselblad", Icons.Outlined.Camera, isEnabled = true),
    ModeItem("XMAGE", Icons.Outlined.Stars, isEnabled = true),
    ModeItem("Harcourt", Icons.Outlined.FilterVintage, isEnabled = true),
    ModeItem("Sony G", Icons.Outlined.PhotoCamera, isEnabled = false),
    ModeItem("Samsung", Icons.Outlined.AutoAwesome, isEnabled = false),
    ModeItem("Apple", Icons.Outlined.Camera, isEnabled = false),
    ModeItem("Pixel", Icons.Outlined.Stars, isEnabled = false),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreModesSheet(
    onDismissRequest: () -> Unit,
    onModeSelected: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = CharcoalSurface,
        scrimColor = PitchBlackBackground.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // Header Title
            Text(
                text = "MORE CAMERA MODES",
                color = RoyalBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // Section 1: PRO TOOLS
            Text(
                text = "PRO TOOLS",
                color = ColorMutedGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
            ) {
                items(proToolsList) { item ->
                    ModeCard(item = item, onClick = {
                        if (item.isEnabled) {
                            onModeSelected(item.name)
                            onDismissRequest()
                        }
                    })
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: SIGNATURE BRANDS
            Text(
                text = "SIGNATURE BRANDS",
                color = ColorMutedGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
            ) {
                items(signatureBrandsList) { item ->
                    ModeCard(item = item, onClick = {
                        if (item.isEnabled) {
                            onModeSelected(item.name)
                            onDismissRequest()
                        }
                    })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeCard(
    item: ModeItem,
    onClick: () -> Unit,
) {
    val isEnabled = item.isEnabled
    val cardModifier = if (isEnabled) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PitchBlackBackground)
            .border(
                width = 1.dp,
                color = if (isEnabled) ColorWhite.copy(alpha = 0.12f) else Color(0xFF1E2330),
                shape = RoundedCornerShape(10.dp),
            )
            .then(cardModifier)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!isEnabled) {
            // Small "SOON" badge with Royal Blue outline and muted gray text #9CA3AF
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 2.dp)
                    .border(width = 1.dp, color = RoyalBlue, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    text = "SOON",
                    color = Color(0xFF9CA3AF),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = if (isEnabled) RoyalBlue else Color(0xFF9CA3AF),
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.name,
                color = if (isEnabled) ColorWhite else Color(0xFF9CA3AF),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
