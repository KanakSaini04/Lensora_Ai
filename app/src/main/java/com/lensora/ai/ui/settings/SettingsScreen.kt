package com.lensora.ai.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lensora.ai.ai.ModelDownloaderWorker
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenProbeLab: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lensora_prefs", Context.MODE_PRIVATE) }
    val workManager = remember { WorkManager.getInstance(context) }
    var isCloudSyncEnabled by remember { mutableStateOf(prefs.getBoolean("cloud_sync_enabled", false)) }
    var isAutoPurgeEnabled by remember { mutableStateOf(prefs.getBoolean("auto_purge_enabled", true)) }

    // Model Downloader WorkState
    var activeWorkId by remember { mutableStateOf<UUID?>(null) }

    val workInfo = activeWorkId?.let { uuid ->
        workManager.getWorkInfoByIdFlow(uuid).collectAsState(initial = null).value
    }

    val isDownloading = workInfo?.state == WorkInfo.State.RUNNING
    val downloadProgress = workInfo?.progress?.getInt("PROGRESS", 0) ?: 0

    val modelFile = remember(workInfo?.state) {
        File(context.filesDir, "models/mobilenet_v1_1.0_224.tflite")
    }
    val isModelDownloaded = modelFile.exists()

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

            // Section 2: Services & Engines (GitHub DLC Downloader)
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
                                prefs.edit().putBoolean("cloud_sync_enabled", isChecked).apply()
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

                // Manage AI Engines Card with GitHub DLC Downloader
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Memory,
                                contentDescription = "AI Engine DLC",
                                tint = RoyalBlue,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Manage AI Engines (Manifest DLC)",
                                    color = ColorWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = if (isModelDownloaded) "MobileNet v1 1.0: Ready (${modelFile.length()} bytes)" else "MobileNet v1 1.0: Not Installed",
                                    color = if (isModelDownloaded) RoyalBlue else ColorMutedGray,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = "Downloading Model...", color = ColorWhite, fontSize = 11.sp)
                                Text(text = "$downloadProgress%", color = RoyalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = RoyalBlue,
                                trackColor = PitchBlackBackground,
                            )
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val downloadRequest = OneTimeWorkRequestBuilder<ModelDownloaderWorker>()
                                        .setInputData(
                                            workDataOf(
                                                "MODEL_URL" to "https://storage.googleapis.com/download.tensorflow.org/models/tflite/mobilenet_v1_1.0_224.zip",
                                                "TARGET_FILENAME" to "mobilenet_v1_1.0_224.tflite",
                                            ),
                                        )
                                        .build()

                                    activeWorkId = downloadRequest.id
                                    workManager.enqueue(downloadRequest)
                                    Toast.makeText(context, "Started MobileNet Model Download", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    tint = ColorWhite,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isModelDownloaded) "Re-download MobileNet Model" else "Download MobileNet Model",
                                    color = ColorWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Storage & Cache
            item {
                SectionHeader(title = "STORAGE & MAINTENANCE")

                // Auto-Purge Local Masters Toggle
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Auto-Purge Local Masters",
                            color = ColorWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Delete local master files from MediaStore after successful cloud sync",
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "Auto Purge",
                            tint = ColorWhite,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = isAutoPurgeEnabled,
                            onCheckedChange = { isChecked ->
                                isAutoPurgeEnabled = isChecked
                                prefs.edit().putBoolean("auto_purge_enabled", isChecked).apply()
                                Toast.makeText(
                                    context,
                                    if (isChecked) "Auto-Purge Enabled" else "Auto-Purge Disabled",
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
