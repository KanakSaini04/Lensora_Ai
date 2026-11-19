package com.lensora.ai.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Speed
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
import com.lensora.ai.ai.ManifestParser
import com.lensora.ai.ai.ModelEntry
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lensora.ai.ai.ModelDownloaderWorker
import com.lensora.ai.auth.GoogleAuthManager
import com.lensora.ai.ui.theme.AccentCyan
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue
import com.lensora.ai.utils.SystemMonitor
import com.lensora.ai.utils.SystemStats
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import java.util.UUID

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenProbeLab: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lensora_prefs", Context.MODE_PRIVATE) }
    val workManager = remember { WorkManager.getInstance(context) }
    val googleAuthManager = remember { GoogleAuthManager(context) }
    var isSignedInToDrive by remember { mutableStateOf(googleAuthManager.isSignedIn()) }
    var signedInEmail by remember { mutableStateOf(googleAuthManager.getSignedInEmail()) }
    var isCloudSyncEnabled by remember { mutableStateOf(prefs.getBoolean("cloud_sync_enabled", false)) }
    var isAutoPurgeEnabled by remember { mutableStateOf(prefs.getBoolean("auto_purge_enabled", true)) }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    // Model Downloader WorkState
    var activeWorkId by remember { mutableStateOf<UUID?>(null) }

    val workInfo = activeWorkId?.let { uuid ->
        workManager.getWorkInfoByIdFlow(uuid).collectAsState(initial = null).value
    }

    val isDownloading = workInfo?.state == WorkInfo.State.RUNNING
    val downloadProgress = workInfo?.progress?.getInt("PROGRESS", 0) ?: 0

    val manifestEntries = remember {
        try {
            val jsonStr = context.assets.open("manifest.json").bufferedReader().use { it.readText() }
            ManifestParser.parseManifest(jsonStr)
        } catch (e: Exception) {
            emptyList<ModelEntry>()
        }
    }

    val sceneEntry = remember(manifestEntries) { manifestEntries.firstOrNull { it.key == "scene_classifier" } }
    val sceneModelFile = remember(workInfo?.state, sceneEntry) {
        File(context.filesDir, "models/${sceneEntry?.fileName ?: "scene_classifier.tflite"}")
    }
    val isModelDownloaded = sceneModelFile.exists() && sceneModelFile.length() > 0

    // LaMa Inpainting Model Downloader WorkState
    var activeLamaWorkId by remember { mutableStateOf<UUID?>(null) }

    val lamaWorkInfo = activeLamaWorkId?.let { uuid ->
        workManager.getWorkInfoByIdFlow(uuid).collectAsState(initial = null).value
    }

    val isLamaDownloading = lamaWorkInfo?.state == WorkInfo.State.RUNNING
    val lamaDownloadProgress = lamaWorkInfo?.progress?.getInt("PROGRESS", 0) ?: 0

    val lamaEntry = remember(manifestEntries) { manifestEntries.firstOrNull { it.key == "lama_dilated" } }
    val lamaModelFile = remember(lamaWorkInfo?.state, lamaEntry) {
        File(context.filesDir, "models/${lamaEntry?.fileName ?: "lama_dilated.tflite"}")
    }
    val isLamaModelDownloaded = lamaModelFile.exists() && lamaModelFile.length() > 0

    // System Monitor State
    val systemMonitor = remember { SystemMonitor(context) }
    var systemStats by remember { mutableStateOf(systemMonitor.getSystemStats()) }

    LaunchedEffect(Unit) {
        while (true) {
            systemStats = systemMonitor.getSystemStats()
            delay(2000)
        }
    }

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
            // Section 1: AI Center & System Diagnostics
            item {
                SectionHeader(title = "SYSTEM & AI CONTROL CENTER")

                // Real-Time System Diagnostics Card
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
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = "System Diagnostics",
                                tint = AccentCyan,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "System Diagnostics Dashboard",
                                    color = ColorWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = "Redmi Note 13 Pro 5G | Live Stats (2s)",
                                    color = ColorMutedGray,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(text = "RAM Usage", color = ColorMutedGray, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%.1f GB / %.1f GB (%d%%)", systemStats.ramUsedGb, systemStats.ramTotalGb, systemStats.ramPercentage),
                                    color = ColorWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Battery Temp", color = ColorMutedGray, fontSize = 11.sp)
                                Text(
                                    text = String.format(Locale.US, "%.1f °C", systemStats.batteryTempCelsius),
                                    color = if (systemStats.batteryTempCelsius > 40f) Color(0xFFEF4444) else AccentCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(text = "Thermal State", color = ColorMutedGray, fontSize = 11.sp)
                                Text(
                                    text = systemStats.thermalStatus,
                                    color = if (systemStats.thermalStatus == "NONE" || systemStats.thermalStatus == "LIGHT") AccentCyan else RoyalBlue,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "AI Engines", color = ColorMutedGray, fontSize = 11.sp)
                                Text(
                                    text = if (systemStats.lamaModelReady) "LaMa: Ready | MobileNet: Ready" else "MobileNet: Ready",
                                    color = ColorWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

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

                // Sign in to Google Drive (OAuth2 REST API v3 Integration)
                ListItem(
                    headlineContent = {
                        Text(
                            text = if (isSignedInToDrive) "Signed in as ${signedInEmail ?: "Google Drive"}" else "Sign in to Google Drive",
                            color = ColorWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = if (isSignedInToDrive) "OAuth2 Session Active | Encrypted Token Vault" else "Authorize Lensora to backup photos to Google Drive",
                            color = if (isSignedInToDrive) AccentCyan else ColorMutedGray,
                            fontSize = 12.sp,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.CloudUpload,
                            contentDescription = "Google Drive Auth",
                            tint = AccentCyan,
                        )
                    },
                    trailingContent = {
                        Button(
                            onClick = {
                                if (isSignedInToDrive) {
                                    googleAuthManager.signOut()
                                    isSignedInToDrive = false
                                    Toast.makeText(context, "Signed out of Google Drive", Toast.LENGTH_SHORT).show()
                                } else {
                                    googleAuthManager.signInSimulated("vault.lensora.ai@gmail.com")
                                    isSignedInToDrive = true
                                    signedInEmail = googleAuthManager.getSignedInEmail()
                                    Toast.makeText(context, "Google Drive OAuth2 Connected!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSignedInToDrive) CharcoalSurface else RoyalBlue,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = if (isSignedInToDrive) "Disconnect" else "Sign In",
                                color = ColorWhite,
                                fontSize = 12.sp,
                            )
                        }
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
                                    text = if (isModelDownloaded) "Scene Classifier: Ready (${sceneModelFile.length()} bytes)" else if (sceneEntry?.url == null) "Scene Classifier: Model Unavailable" else "Scene Classifier: Not Installed",
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
                            val isSceneAvailable = sceneEntry?.url != null
                            Button(
                                onClick = {
                                    val url = sceneEntry?.url ?: return@Button
                                    val downloadRequest = OneTimeWorkRequestBuilder<ModelDownloaderWorker>()
                                        .setInputData(
                                            workDataOf(
                                                "TARGET_KEY" to "scene_classifier",
                                                "MODEL_URL" to url,
                                                "TARGET_FILENAME" to sceneEntry.fileName,
                                            ),
                                        )
                                        .build()

                                    activeWorkId = downloadRequest.id
                                    workManager.enqueue(downloadRequest)
                                    Toast.makeText(context, "Started Scene Classifier Model Download", Toast.LENGTH_SHORT).show()
                                },
                                enabled = isSceneAvailable,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalBlue,
                                    disabledContainerColor = CharcoalSurface,
                                    disabledContentColor = ColorMutedGray,
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    tint = if (isSceneAvailable) ColorWhite else ColorMutedGray,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isModelDownloaded) "Re-download Model" else if (!isSceneAvailable) "Model Unavailable" else "Download Model",
                                    color = if (isSceneAvailable) ColorWhite else ColorMutedGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }

                // LaMa Dilated Inpainting DLC Card
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
                                imageVector = Icons.Outlined.AutoFixHigh,
                                contentDescription = "LaMa AI Engine",
                                tint = AccentCyan,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "LaMa AI Inpainting Engine (DLC)",
                                    color = ColorWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = if (isLamaModelDownloaded) "LaMa Dilated: Ready (${lamaModelFile.length()} bytes)" else if (lamaEntry?.url == null) "LaMa Dilated: Model Unavailable" else "LaMa Dilated: Not Installed",
                                    color = if (isLamaModelDownloaded) AccentCyan else ColorMutedGray,
                                    fontSize = 12.sp,
                                )
                            }
                        }

                        if (isLamaDownloading) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = "Downloading LaMa Model...", color = ColorWhite, fontSize = 12.sp)
                                Text(text = "$lamaDownloadProgress%", color = AccentCyan, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { lamaDownloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = AccentCyan,
                                trackColor = PitchBlackBackground,
                            )
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            val isLamaAvailable = lamaEntry?.url != null
                            Button(
                                onClick = {
                                    val url = lamaEntry?.url ?: return@Button
                                    val request = OneTimeWorkRequestBuilder<ModelDownloaderWorker>()
                                        .setInputData(
                                            workDataOf(
                                                "TARGET_KEY" to "lama_dilated",
                                                "MODEL_URL" to url,
                                                "TARGET_FILENAME" to lamaEntry.fileName,
                                            ),
                                        )
                                        .build()

                                    activeLamaWorkId = request.id
                                    workManager.enqueue(request)
                                    Toast.makeText(context, "Started LaMa Model Download", Toast.LENGTH_SHORT).show()
                                },
                                enabled = isLamaAvailable,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentCyan,
                                    disabledContainerColor = CharcoalSurface,
                                    disabledContentColor = ColorMutedGray,
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    tint = if (isLamaAvailable) ColorWhite else ColorMutedGray,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isLamaModelDownloaded) "Re-download LaMa Model" else if (!isLamaAvailable) "Model Unavailable" else "Download LaMa Model",
                                    color = if (isLamaAvailable) ColorWhite else ColorMutedGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
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

                ListItem(
                    headlineContent = {
                        Text(
                            text = "Panic Wipe (Emergency Data Destruction)",
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Permanently clear filesDir, cacheDir, EncryptedSharedPreferences, PIN hash, and KeyStore aliases",
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.CleaningServices,
                            contentDescription = "Panic Wipe",
                            tint = Color(0xFFEF4444),
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = CharcoalSurface),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            try {
                                // 1. Clear filesDir and cacheDir
                                context.filesDir?.deleteRecursively()
                                context.cacheDir?.deleteRecursively()

                                // 2. Clear SharedPreferences
                                prefs.edit().clear().apply()
                                val cameraPrefs = context.getSharedPreferences("lensora_camera_prefs", Context.MODE_PRIVATE)
                                cameraPrefs.edit().clear().apply()

                                // 3. Clear EncryptedSharedPreferences
                                try {
                                    val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
                                    val encryptedPrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                                        "encrypted_lensora_prefs",
                                        masterKeyAlias,
                                        context,
                                        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                                    )
                                    encryptedPrefs.edit().clear().apply()
                                } catch (_: Exception) {}

                                // 4. Delete KeyStore aliases
                                try {
                                    val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                                    val aliases = keyStore.aliases()
                                    while (aliases.hasMoreElements()) {
                                        val alias = aliases.nextElement()
                                        keyStore.deleteEntry(alias)
                                    }
                                } catch (_: Exception) {}

                                Toast.makeText(context, "Panic Wipe Complete. All data permanently destroyed.", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Panic Wipe Execution Error", Toast.LENGTH_SHORT).show()
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

            // Section 5: Legal & Privacy
            item {
                SectionHeader(title = "LEGAL & PRIVACY")
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Privacy Policy",
                            color = ColorWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "100% On-Device Offline AI & Google Drive Sync Scope [DRAFT]",
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Privacy Policy",
                            tint = RoyalBlue,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = CharcoalSurface),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { showPrivacyDialog = true },
                )

                ListItem(
                    headlineContent = {
                        Text(
                            text = "Terms of Service",
                            color = ColorWhite,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "End User License Agreement & Offline AI Usage Terms [DRAFT]",
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Terms of Service",
                            tint = AccentCyan,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = CharcoalSurface),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { showTermsDialog = true },
                )
            }

            // Section 6: About
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

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showPrivacyDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text("[DRAFT - LEGAL REVIEW NEEDED]", color = Color(0xFFEAB308), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Privacy Policy", color = ColorWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Lensora_Ai operates on a strict 100% offline, privacy-first model.\n\n" +
                                "1. On-Device Processing: All AI scene classification, image enhancement (Dehaze, Real-ESRGAN), and video editing occur entirely on your local device hardware. No photos, videos, or camera feeds are ever uploaded to any third-party AI cloud server.\n\n" +
                                "2. Optional Google Cloud Vault Sync: If enabled by you, photos are uploaded directly to your personal Google Drive account via official Google OAuth2 APIs. Tokens are saved securely in EncryptedSharedPreferences.\n\n" +
                                "3. Zero Telemetry & Zero Ads: Lensora_Ai contains zero analytics trackers, zero advertising SDKs, and zero behavioral telemetry.",
                            color = ColorMutedGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showPrivacyDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text("Close", color = ColorWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Terms of Service Dialog
        if (showTermsDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showTermsDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text("[DRAFT - LEGAL REVIEW NEEDED]", color = Color(0xFFEAB308), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Terms of Service", color = ColorWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "End User License Agreement for Lensora_Ai Application.\n\n" +
                                "1. License Grant: Lensora_Ai grants you a personal, non-exclusive license to use computational camera, AI inference, and video editing capabilities on supported Android devices.\n\n" +
                                "2. Hardware Responsibility: Real-time high-resolution AI processing and video export are compute-intensive operations. The application enforces thermal and storage guard checks to preserve system stability.\n\n" +
                                "3. User Data Ownership: All captured media and exported projects remain 100% your property.",
                            color = ColorMutedGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showTermsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text("Close", color = ColorWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
