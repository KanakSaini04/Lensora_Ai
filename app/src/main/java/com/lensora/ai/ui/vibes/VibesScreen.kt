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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
    val title: String,
    val subtitle: String,
    val previewColor: Color,
    val isTrending: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibesScreen() {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("LUTs", "Presets", "Templates")

    val trendingVibes = remember {
        listOf(
            VibeItem("Leica Authentic", "Classic Natural Contrast", Color(0xFF2C3E50), true),
            VibeItem("Cinematic City", "Cool Shadows & Warm Lights", Color(0xFF1F2421), true),
            VibeItem("Muted Film", "Soft Kodak Portra Grain", Color(0xFF3D3A45), true),
            VibeItem("Moody Teal", "Orange & Teal Accent", Color(0xFF1A3636), true),
        )
    }

    val allVibes = remember {
        listOf(
            VibeItem("Leica Authentic", "Classic Natural Contrast", Color(0xFF2C3E50)),
            VibeItem("Cinematic City", "Cool Shadows & Warm Lights", Color(0xFF1F2421)),
            VibeItem("Muted Film", "Soft Kodak Portra Grain", Color(0xFF3D3A45)),
            VibeItem("Moody Teal", "Orange & Teal Accent", Color(0xFF1A3636)),
            VibeItem("Monochrome High", "Deep B&W Shadow Contrast", Color(0xFF222222)),
            VibeItem("Warm Vintage", "Golden Hour Sun Glow", Color(0xFF4A3B32)),
            VibeItem("Cyber Neon", "Vibrant Cyan & Purple Tone", Color(0xFF2E1A47)),
            VibeItem("Nordic Clean", "Bright Crisp Architectural", Color(0xFF2B3A42)),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vibes & Presets",
                        color = ColorWhite,
                        fontWeight = FontWeight.Bold,
                    )
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

            // Main Content Grid with Trending Row
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // "🔥 Trending This Week" Section Header & LazyRow
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Text(
                            text = "🔥 TRENDING THIS WEEK",
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
                            items(trendingVibes) { vibe ->
                                TrendingVibeCard(
                                    vibe = vibe,
                                    onClick = {
                                        Toast.makeText(context, "Applied ${vibe.title}", Toast.LENGTH_SHORT).show()
                                    },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "ALL COLOR PRESETS (${tabTitles[selectedTabIndex]})",
                            color = ColorMutedGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                    }
                }

                // Grid of Vibe Cards
                items(allVibes) { vibe ->
                    VibeGridCard(
                        vibe = vibe,
                        onClick = {
                            Toast.makeText(context, "Selected ${vibe.title}", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingVibeCard(
    vibe: VibeItem,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = Modifier
            .width(160.dp)
            .height(120.dp)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(vibe.previewColor)
                .padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(PitchBlackBackground.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "HOT",
                    color = RoyalBlue,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                modifier = Modifier.align(Alignment.BottomStart),
            ) {
                Text(
                    text = vibe.title,
                    color = ColorWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = vibe.subtitle,
                    color = ColorMutedGray,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun VibeGridCard(
    vibe: VibeItem,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(vibe.previewColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = ColorWhite.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            ) {
                Text(
                    text = vibe.title,
                    color = ColorWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = vibe.subtitle,
                    color = ColorMutedGray,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
