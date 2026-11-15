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
)

val proToolsList = listOf(
    ModeItem("Astro", Icons.Outlined.Stars),
    ModeItem("Panorama", Icons.Outlined.Panorama),
    ModeItem("Tele-Cine", Icons.Outlined.Movie),
    ModeItem("Pro Macro", Icons.Outlined.Camera),
    ModeItem("Time-Lapse", Icons.Outlined.Timelapse),
    ModeItem("3D Spatial", Icons.Outlined.AutoAwesome),
    ModeItem("360 Sphere", Icons.Outlined.PhotoCamera),
    ModeItem("Stop Motion", Icons.Outlined.Videocam),
)

val signatureBrandsList = listOf(
    ModeItem("Leica", Icons.Outlined.FilterVintage),
    ModeItem("ZEISS", Icons.Outlined.AutoAwesome),
    ModeItem("Hasselblad", Icons.Outlined.Camera),
    ModeItem("XMAGE", Icons.Outlined.Stars),
    ModeItem("Harcourt", Icons.Outlined.FilterVintage),
    ModeItem("Sony G", Icons.Outlined.PhotoCamera),
    ModeItem("Samsung", Icons.Outlined.AutoAwesome),
    ModeItem("Apple", Icons.Outlined.Camera),
    ModeItem("Pixel", Icons.Outlined.Stars),
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
                        onModeSelected(item.name)
                        onDismissRequest()
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
                        onModeSelected(item.name)
                        onDismissRequest()
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PitchBlackBackground)
            .border(width = 1.dp, color = ColorWhite.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.name,
                tint = RoyalBlue,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.name,
                color = ColorWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}
