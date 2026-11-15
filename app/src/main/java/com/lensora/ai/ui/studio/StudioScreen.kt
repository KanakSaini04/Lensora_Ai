package com.lensora.ai.ui.studio

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lensora.ai.data.GalleryRepository
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen() {
    val context = LocalContext.current
    val galleryRepository = remember { GalleryRepository(context) }
    var photoUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }

    // Interactive editing tool states
    var selectedToolCategory by remember { mutableIntStateOf(0) } // 0: Light, 1: Color, 2: Effects, 3: Crop
    var exposureValue by remember { mutableFloatStateOf(0.0f) }
    var contrastValue by remember { mutableFloatStateOf(0.0f) }

    // Real-time GPU ColorFilter matrix derived from Exposure & Contrast sliders
    val realTimeColorFilter = remember(exposureValue, contrastValue) {
        ColorFilter.colorMatrix(ImageEditor.createColorMatrix(exposureValue, contrastValue))
    }

    LaunchedEffect(Unit) {
        photoUris = galleryRepository.getAllPhotos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Studio Editor",
                        color = ColorWhite,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = {
                        exposureValue = 0.0f
                        contrastValue = 0.0f
                        Toast.makeText(context, "Reset Edit Pass", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.Undo, contentDescription = "Undo", tint = ColorWhite)
                    }
                    IconButton(onClick = { Toast.makeText(context, "Redo Pass", Toast.LENGTH_SHORT).show() }) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.Redo, contentDescription = "Redo", tint = ColorWhite)
                    }
                    Button(
                        onClick = { Toast.makeText(context, "Exporting Photo...", Toast.LENGTH_SHORT).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(imageVector = Icons.Outlined.Download, contentDescription = null, tint = ColorWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Export", color = ColorWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalSurface,
                ),
            )
        },
        containerColor = PitchBlackBackground,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PitchBlackBackground),
        ) {
            // 1. TOP SECTION (~50% Height): Photo Preview Canvas Area with Real-Time GPU ColorFilter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.50f)
                    .background(Color(0xFF0A0A0A)),
                contentAlignment = Alignment.Center,
            ) {
                if (photoUris.isNotEmpty() && selectedPhotoIndex in photoUris.indices) {
                    AsyncImage(
                        model = photoUris[selectedPhotoIndex],
                        contentDescription = "Studio Active Image",
                        contentScale = ContentScale.Fit,
                        colorFilter = realTimeColorFilter,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .background(CharcoalSurface, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No Photo Loaded\nCapture a photo to start editing",
                            color = ColorMutedGray,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                    }
                }

                // Info metadata badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PitchBlackBackground.copy(alpha = 0.75f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "12.5 MP • ISO 100 • 1/250s • f/1.8",
                        color = ColorWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // 2. MIDDLE SECTION (~15% Height): Filmstrip Thumbnails
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f)
                    .background(CharcoalSurface)
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "FILMSTRIP QUEUE",
                    color = RoyalBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp),
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (photoUris.isNotEmpty()) {
                        itemsIndexed(photoUris) { index, uri ->
                            val isSelected = selectedPhotoIndex == index
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) RoyalBlue else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .background(PitchBlackBackground)
                                    .clickable { selectedPhotoIndex = index },
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Filmstrip $index",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    } else {
                        items(6) { index ->
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PitchBlackBackground),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = "#${index + 1}", color = ColorMutedGray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // 3. BOTTOM SECTION (~35% Height): Professional Editing Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .background(PitchBlackBackground)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Tool category bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CharcoalSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    ToolCategoryButton(
                        icon = Icons.Outlined.WbSunny,
                        label = "Light",
                        isSelected = selectedToolCategory == 0,
                        onClick = { selectedToolCategory = 0 },
                    )
                    ToolCategoryButton(
                        icon = Icons.Outlined.Palette,
                        label = "Color",
                        isSelected = selectedToolCategory == 1,
                        onClick = { selectedToolCategory = 1 },
                    )
                    ToolCategoryButton(
                        icon = Icons.Outlined.AutoFixHigh,
                        label = "Effects",
                        isSelected = selectedToolCategory == 2,
                        onClick = { selectedToolCategory = 2 },
                    )
                    ToolCategoryButton(
                        icon = Icons.Outlined.Crop,
                        label = "Crop",
                        isSelected = selectedToolCategory == 3,
                        onClick = { selectedToolCategory = 3 },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Sliders (Exposure & Contrast) with 60fps GPU preview updates
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Exposure Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Exposure EV", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = String.format("%.2f", exposureValue),
                            color = RoyalBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Slider(
                        value = exposureValue,
                        onValueChange = { exposureValue = it },
                        valueRange = -2.0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = RoyalBlue,
                            activeTrackColor = RoyalBlue,
                            inactiveTrackColor = CharcoalSurface,
                        ),
                        modifier = Modifier.height(24.dp),
                    )

                    // Contrast Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Contrast", color = ColorWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = String.format("%.0f", contrastValue),
                            color = RoyalBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Slider(
                        value = contrastValue,
                        onValueChange = { contrastValue = it },
                        valueRange = -50.0f..50.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = RoyalBlue,
                            activeTrackColor = RoyalBlue,
                            inactiveTrackColor = CharcoalSurface,
                        ),
                        modifier = Modifier.height(24.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3-Way Color Wheels Placeholder
                Text(
                    text = "3-WAY COLOR WHEELS (L3 INTERCEPT)",
                    color = ColorMutedGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorWheelCircle(label = "Shadows", accentColor = Color(0xFF2563EB))
                    ColorWheelCircle(label = "Midtones", accentColor = Color(0xFF10B981))
                    ColorWheelCircle(label = "Highlights", accentColor = Color(0xFFF59E0B))
                }
            }
        }
    }
}

@Composable
private fun ToolCategoryButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) RoyalBlue else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) ColorWhite else ColorMutedGray,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = if (isSelected) ColorWhite else ColorMutedGray,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ColorWheelCircle(
    label: String,
    accentColor: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(CharcoalSurface)
                .border(1.5.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = ColorMutedGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
