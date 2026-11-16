package com.lensora.ai.ui.vibes

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue

data class VibeItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val overlayText: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibesScreen() {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("LUTs", "Presets", "Templates")

    // Data for LUTs Tab (3D Color Math Swatches)
    val lutItems = remember {
        listOf(
            VibeItem("1", "Leica Authentic 3D", "Rec.709 Color Science Matrix", Color(0xFF0F2027), Color(0xFF203A43)),
            VibeItem("2", "Cinematic City LUT", "Teal & Orange Color Math", Color(0xFF1F1C2C), Color(0xFF928DAB)),
            VibeItem("3", "Kodak Portra 400", "Film Grain Emulation Cube", Color(0xFF2C3E50), Color(0xFFFD746C)),
            VibeItem("4", "Fuji Superia LUT", "Green Shadows & Red Tones", Color(0xFF134E5E), Color(0xFF71B280)),
            VibeItem("5", "B&W High Contrast", "Monochrome Luminance Curve", Color(0xFF000000), Color(0xFF434343)),
            VibeItem("6", "Golden Hour 3D", "Warm Sun Vector Offset", Color(0xFF3A1C71), Color(0xFFD76D77)),
        )
    }

    // Data for Presets Tab (Tone Curves & Before/After Shifts)
    val presetItems = remember {
        listOf(
            VibeItem("1", "Clean Portrait", "+0.3 EV, Soft Skin Curves", Color(0xFF1E1E1E), Color(0xFF3B3B3B)),
            VibeItem("2", "Moody Shadows", "-0.5 EV, Deep Shadow Roll", Color(0xFF111111), Color(0xFF2C2C2C)),
            VibeItem("3", "Vibrant Landscape", "+0.2 EV, High Dynamic Range", Color(0xFF14213D), Color(0xFF003049)),
            VibeItem("4", "Matte Urban", "Faded Blacks, Soft Contrast", Color(0xFF2B2D42), Color(0xFF8D99AE)),
            VibeItem("5", "Sunset Glow", "Warm Highlights, Red Lift", Color(0xFF3D0C02), Color(0xFF780000)),
            VibeItem("6", "Nordic Crisp", "Cool Temp, Sharpened Highlights", Color(0xFF001D3D), Color(0xFF003566)),
        )
    }

    // Data for Templates Tab (9:16 Vertical Story Overlays)
    val templateItems = remember {
        listOf(
            VibeItem("1", "Daily Journal", "9:16 Story Frame Layout", Color(0xFF111827), Color(0xFF1F2937), "Day 26/365"),
            VibeItem("2", "Film Strip Reel", "Vertical Cinema Border", Color(0xFF0F172A), Color(0xFF1E293B), "REELS 9:16"),
            VibeItem("3", "Minimalist Quote", "Clean Helvetica Overlay", Color(0xFF18181B), Color(0xFF27272A), "OCTOBER 2026"),
            VibeItem("4", "Polaroid Snap", "Vintage White Border Frame", Color(0xFF1F2022), Color(0xFF2D3033), "MEMORIES"),
            VibeItem("5", "Architectural", "Grid & Ratio Overlay", Color(0xFF0D1117), Color(0xFF161B22), "1080 x 1920"),
            VibeItem("6", "Vlog Intro Frame", "Title & Timestamp Template", Color(0xFF1C1917), Color(0xFF292524), "EPISODE 04"),
        )
    }

    val currentItems = when (selectedTabIndex) {
        0 -> lutItems
        1 -> presetItems
        else -> templateItems
    }

    val handleVibeSelection: (VibeItem) -> Unit = { vibe ->
        val preset = mapVibeToPreset(vibe)
        VibeState.selectedVibe = preset
        Toast.makeText(context, "Applied ${preset.displayName} to Studio!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Vibes & Presets",
                            color = ColorWhite,
                            fontWeight = FontWeight.Bold,
                        )
                        if (VibeState.selectedVibe != VibePreset.NONE) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RoyalBlue)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = VibeState.selectedVibe.displayName,
                                    color = ColorWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
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
            // Top Segmented Tab Row (LUTs | Presets | Templates)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = CharcoalSurface,
                contentColor = ColorWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = RoyalBlue,
                        height = 3.dp,
                    )
                },
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                color = if (isSelected) RoyalBlue else ColorMutedGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                            )
                        },
                    )
                }
            }

            // Main Content Grid with Dynamic Tab Layout
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Section Header & Trending Row
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = "🔥 TRENDING ${tabTitles[selectedTabIndex].uppercase()}",
                            color = RoyalBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 16.dp),
                        ) {
                            items(currentItems.take(4)) { vibe ->
                                TrendingCardByTab(
                                    tabIndex = selectedTabIndex,
                                    vibe = vibe,
                                    onClick = { handleVibeSelection(vibe) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ALL ${tabTitles[selectedTabIndex].uppercase()}",
                            color = ColorMutedGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }

                // Main Grid Items render differently based on active tab
                items(currentItems) { vibe ->
                    GridCardByTab(
                        tabIndex = selectedTabIndex,
                        vibe = vibe,
                        onClick = { handleVibeSelection(vibe) },
                    )
                }
            }
        }
    }
}

private fun mapVibeToPreset(vibe: VibeItem): VibePreset {
    return when {
        vibe.title.contains("Leica", ignoreCase = true) || vibe.title.contains("B&W", ignoreCase = true) -> VibePreset.LEICA
        vibe.title.contains("Cinematic", ignoreCase = true) || vibe.title.contains("Portra", ignoreCase = true) -> VibePreset.CINEMATIC
        vibe.title.contains("Golden", ignoreCase = true) || vibe.title.contains("Sunset", ignoreCase = true) -> VibePreset.ROYAL_GOLD
        vibe.title.contains("Vintage", ignoreCase = true) || vibe.title.contains("Fuji", ignoreCase = true) || vibe.title.contains("Matte", ignoreCase = true) -> VibePreset.VINTAGE
        else -> VibePreset.CINEMATIC
    }
}

// Renders distinct Trending Row Cards based on Tab
@Composable
private fun TrendingCardByTab(
    tabIndex: Int,
    vibe: VibeItem,
    onClick: () -> Unit,
) {
    when (tabIndex) {
        0 -> LutTrendingCard(vibe = vibe, onClick = onClick)
        1 -> PresetTrendingCard(vibe = vibe, onClick = onClick)
        else -> TemplateTrendingCard(vibe = vibe, onClick = onClick)
    }
}

// Renders distinct Grid Cards based on Tab
@Composable
private fun GridCardByTab(
    tabIndex: Int,
    vibe: VibeItem,
    onClick: () -> Unit,
) {
    when (tabIndex) {
        0 -> LutGridCard(vibe = vibe, onClick = onClick)
        1 -> PresetGridCard(vibe = vibe, onClick = onClick)
        else -> TemplateGridCard(vibe = vibe, onClick = onClick)
    }
}

// ---------------------------------------------------------------------------
// 1. LUTs Tab Components (1:1 Square Swatches, 3D Color Math)
// ---------------------------------------------------------------------------

@Composable
private fun LutTrendingCard(vibe: VibeItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = Modifier
            .width(160.dp)
            .height(130.dp)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(vibe.primaryColor, vibe.secondaryColor),
                    ),
                )
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PitchBlackBackground.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(text = "3D LUT", color = RoyalBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = ColorWhite.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp),
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(text = vibe.title, color = ColorWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(text = vibe.subtitle, color = ColorMutedGray, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun LutGridCard(vibe: VibeItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // 1:1 Square
                    .background(
                        Brush.linearGradient(
                            colors = listOf(vibe.primaryColor, vibe.secondaryColor),
                        ),
                    )
                    .padding(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(PitchBlackBackground.copy(alpha = 0.6f))
                        .padding(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PitchBlackBackground.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(text = "3D CUBE", color = ColorWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = vibe.title, color = ColorWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = vibe.subtitle, color = ColorMutedGray, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. Presets Tab Components (1:1 Split Before/After Exposure Curves)
// ---------------------------------------------------------------------------

@Composable
private fun PresetTrendingCard(vibe: VibeItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = Modifier
            .width(160.dp)
            .height(130.dp)
            .clickable { onClick() },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Split Before/After
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF222222)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "RAW", color = ColorMutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(ColorWhite),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(vibe.secondaryColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "EDITED", color = RoyalBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                Text(text = vibe.title, color = ColorWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = vibe.subtitle, color = ColorMutedGray, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PresetGridCard(vibe: VibeItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column {
            // Split Before / After 1:1 Aspect Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color(0xFF1E1E1E)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "BEFORE", color = ColorMutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(ColorWhite.copy(alpha = 0.8f)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(vibe.secondaryColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "AFTER", color = RoyalBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PitchBlackBackground.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(10.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "TONE CURVE", color = ColorWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = vibe.title, color = ColorWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = vibe.subtitle, color = ColorMutedGray, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Templates Tab Components (9:16 Tall Vertical Cards with Border & Text)
// ---------------------------------------------------------------------------

@Composable
private fun TemplateTrendingCard(vibe: VibeItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = Modifier
            .width(110.dp)
            .height(170.dp)
            .border(1.dp, RoyalBlue.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(vibe.primaryColor)
                .padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(4.dp))
                    .background(RoyalBlue)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(text = "9:16", color = ColorWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = vibe.overlayText,
                color = ColorWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(text = vibe.title, color = ColorWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TemplateGridCard(vibe: VibeItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, RoyalBlue, RoundedCornerShape(12.dp))
            .clickable { onClick() },
    ) {
        Column {
            // 9:16 Vertical Aspect Ratio Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f) // 9:16 Tall Story Aspect
                    .background(vibe.primaryColor)
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PitchBlackBackground.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AspectRatio,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(10.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "VN 9:16", color = ColorWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Overlay Text Representation
                Box(
                    modifier = Modifier
                        .border(1.dp, ColorWhite.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = vibe.overlayText,
                        color = ColorWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    text = "STORY FRAME OVERLAY",
                    color = ColorMutedGray,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp),
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = vibe.title, color = ColorWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = vibe.subtitle, color = ColorMutedGray, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}
