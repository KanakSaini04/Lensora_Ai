package com.lensora.ai.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenProbeLab: () -> Unit = {},
) {
    val context = LocalContext.current
    var isCloudSyncEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PitchBlackBackground),
        ) {
            // Section 1: AI Center
            item {
                SectionHeader(title = "AI CONTROL CENTER")
                ListItem(
                    headlineContent = {
                        Text(
                            text = "AI Center Status",
                            color = ColorWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Scene Classifier: Active (50ms) | Model: TFLite / MobileNet",
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "AI Center",
                            tint = RoyalBlue,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = CharcoalSurface),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            Toast.makeText(context, "AI Center: Active", Toast.LENGTH_SHORT).show()
                        },
                )
            }

            // Section 2: Cloud Sync & Plugins
            item {
                SectionHeader(title = "SERVICES & ENGINES")
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Cloud Sync",
                            color = ColorWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Sync captured photos to Google Drive automatically",
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.CloudSync,
                            contentDescription = "Cloud Sync",
                            tint = ColorWhite,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isCloudSyncEnabled,
                            onCheckedChange = { isChecked ->
                                isCloudSyncEnabled = isChecked
                                Toast.makeText(
                                    context,
                                    if (isChecked) "Cloud Sync Enabled" else "Cloud Sync Disabled",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ColorWhite,
                                checkedTrackColor = RoyalBlue,
                                uncheckedThumbColor = ColorMutedGray,
                                uncheckedTrackColor = PitchBlackBackground,
                            ),
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = CharcoalSurface),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                ListItem(
                    headlineContent = {
                        Text(
                            text = "Manage AI Engines",
                            color = ColorWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Configure downloaded TFLite plugins and weights",
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Memory,
                            contentDescription = "Manage AI Engines",
                            tint = ColorWhite,
                        )
                    },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = ColorMutedGray,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = CharcoalSurface),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            Toast.makeText(context, "AI Engines: Up to date", Toast.LENGTH_SHORT).show()
                        },
                )
            }

            // Section 3: Storage & Cache
            item {
                SectionHeader(title = "STORAGE & MAINTENANCE")
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Clear Cache & Temporary Files",
                            color = ColorWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Delete temporary .tmp capture journals and cached bitmaps",
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.CleaningServices,
                            contentDescription = "Clear Cache",
                            tint = ColorWhite,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = CharcoalSurface),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            try {
                                context.cacheDir.deleteRecursively()
                                Toast.makeText(context, "Cache Cleared Successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cache Clear Failed", Toast.LENGTH_SHORT).show()
                            }
                        },
                )
            }

            // Section 4: Hardware Diagnostics (Probe Lab Launcher)
            item {
                SectionHeader(title = "HARDWARE DIAGNOSTICS")
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onOpenProbeLab() },
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Hardware Probe Lab",
                                color = ColorWhite,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Audit Camera2 sensor capabilities (P1: 200MP, P2: RAW, P3: 24fps) safely",
                                color = ColorMutedGray,
                                fontSize = 12.sp,
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Science,
                                contentDescription = "Probe Lab",
                                tint = RoyalBlue,
                            )
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = ColorMutedGray,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = CharcoalSurface),
                    )
                }
            }

            // Section 5: About
            item {
                SectionHeader(title = "ABOUT")
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Lensora_Ai",
                            color = ColorWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Version 1.0.0 (Android 16 API 36) | Pure Camera2 L0 Safe Core",
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "About",
                            tint = ColorWhite,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = CharcoalSurface),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Text(
            text = title,
            color = RoyalBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        )
    }
}
