package com.lensora.ai.editor.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.lensora.ai.editor.Clip
import com.lensora.ai.editor.ClipType
import com.lensora.ai.editor.Project
import com.lensora.ai.editor.import.ImportState
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // System Photo Picker launcher for New Project (images and videos, max 30 items)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 30),
    ) { uris ->
        if (uris.isNotEmpty()) {
            val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date())
            val defaultProjectName = "Vlog Project ($dateStr)"
            viewModel.startImport(uris, defaultProjectName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.selectedProject != null) uiState.selectedProject!!.name else "VIDEO PROJECTS",
                        color = ColorWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                },
                navigationIcon = {
                    if (uiState.selectedProject != null) {
                        IconButton(onClick = { viewModel.selectProject(null) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Projects",
                                tint = ColorWhite,
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.selectedProject != null) {
                        IconButton(onClick = {
                            val idToDelete = uiState.selectedProject!!.id
                            viewModel.deleteProject(idToDelete)
                            Toast.makeText(context, "Deleted Project", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete Project",
                                tint = Color(0xFFEF4444),
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = ColorWhite,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Project", color = ColorWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalSurface),
            )
        },
        containerColor = PitchBlackBackground,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PitchBlackBackground),
        ) {
            val selectedProj = uiState.selectedProject
            if (selectedProj != null) {
                EditorScreen(
                    project = selectedProj,
                    viewModel = viewModel,
                    onBackClick = { viewModel.selectProject(null) },
                )
            } else {
                // Projects List View
                if (uiState.projects.isEmpty() && !uiState.isLoading) {
                    EmptyProjectsState(
                        onNewProjectClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                            )
                        },
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(uiState.projects, key = { it.id }) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { viewModel.selectProject(project) },
                                onDelete = { viewModel.deleteProject(project.id) },
                            )
                        }
                    }
                }
            }

            // Import Progress / Storage Warning Dialog
            val importState = uiState.importState
            if (importState !is ImportState.Idle) {
                ImportProgressDialog(
                    importState = importState,
                    onCancel = { viewModel.cancelImport() },
                    onDismiss = { viewModel.dismissImportState() },
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val totalDurationMs = project.clips.sumOf { it.durationMs }
    val totalSecs = totalDurationMs / 1000
    val durationFormatted = String.format(Locale.US, "%02d:%02d", totalSecs / 60, totalSecs % 60)
    val createdDateStr = remember(project.createdAt) {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(project.createdAt))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = project.name,
                        color = ColorWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(RoyalBlue.copy(alpha = 0.2f))
                            .border(1.dp, RoyalBlue, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = project.aspect.name.replace("_", ":"),
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${project.clips.size} clips • Duration: $durationFormatted • $createdDateStr",
                    color = ColorMutedGray,
                    fontSize = 12.sp,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete Project",
                    tint = Color(0xFFEF4444),
                )
            }
        }
    }
}

@Composable
private fun ReadOnlyProjectView(
    project: Project,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("PROJECT SUMMARY", color = RoyalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(project.name, color = ColorWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val totalSecs = project.clips.sumOf { it.durationMs } / 1000
                Text(
                    "Total Duration: ${totalSecs / 60}m ${totalSecs % 60}s | Aspect: ${project.aspect.name} | Clips: ${project.clips.size}",
                    color = ColorMutedGray,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("IMPORTED CLIPS", color = ColorWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(project.clips, key = { it.id }) { clip ->
                ClipThumbnailCard(clip = clip)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onDelete,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete Project & Media Folder", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ClipThumbnailCard(clip: Clip) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .width(130.dp)
            .height(170.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = clip.sourceUri,
                contentDescription = "Clip Thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // Duration Label Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                val secs = clip.durationMs / 1000f
                Text(
                    text = String.format(Locale.US, "%.1fs", secs),
                    color = ColorWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Clip Type Badge (PHOTO / VIDEO)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (clip.type == ClipType.VIDEO) RoyalBlue else Color(0xFF00E5FF))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = clip.type.name,
                    color = if (clip.type == ClipType.VIDEO) ColorWhite else PitchBlackBackground,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun EmptyProjectsState(onNewProjectClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.VideoLibrary,
            contentDescription = null,
            tint = ColorMutedGray,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Video Projects Yet",
            color = ColorWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Import photos and videos to start creating VN-style reels",
            color = ColorMutedGray,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNewProjectClick,
            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, tint = ColorWhite)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create New Project", color = ColorWhite, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ImportProgressDialog(
    importState: ImportState,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = {
        if (importState is ImportState.Progress) onCancel() else onDismiss()
    }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (importState) {
                    is ImportState.Progress -> {
                        Text("IMPORTING MEDIA", color = RoyalBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Clip ${importState.currentClip} of ${importState.totalClips}",
                            color = ColorWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            importState.currentClipName,
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val progress = importState.currentClip.toFloat() / importState.totalClips.toFloat()
                        LinearProgressIndicator(
                            progress = { progress },
                            color = RoyalBlue,
                            trackColor = PitchBlackBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(containerColor = PitchBlackBackground),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Cancel Import", color = Color(0xFFEF4444))
                        }
                    }

                    is ImportState.StorageBlocked -> {
                        Text("STORAGE GUARD ALERT", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        val reqMb = importState.requiredBytes / (1024 * 1024)
                        val availMb = importState.availableBytes / (1024 * 1024)
                        Text(
                            "Insufficient Storage Space",
                            color = ColorWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Required: $reqMb MB | Available: $availMb MB. Minimum 2GB free space required.",
                            color = ColorMutedGray,
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("OK", color = ColorWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    is ImportState.Error -> {
                        Text("IMPORT ERROR", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(importState.message, color = ColorWhite, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Close", color = ColorWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    is ImportState.Success -> {
                        if (importState.skippedCount > 0) {
                            Text("IMPORT WARNING", color = Color(0xFFEAB308), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "${importState.skippedCount} file(s) could not be imported",
                                color = ColorWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("OK", color = ColorWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun ProjectsScreenPreview() {
    val sampleClips = listOf(
        Clip("c1", "", ClipType.PHOTO, 3000L),
        Clip("c2", "", ClipType.VIDEO, 12500L),
    )
    val sampleProject = Project(
        id = "sample_01",
        name = "Summer Travel Vlog",
        aspect = com.lensora.ai.editor.AspectRatio.NINE_SIXTEEN,
        clips = sampleClips,
    )
    com.lensora.ai.ui.theme.Lensora_AiTheme {
        Box(modifier = Modifier.fillMaxSize().background(PitchBlackBackground).padding(16.dp)) {
            ProjectCard(project = sampleProject, onClick = {}, onDelete = {})
        }
    }
}
