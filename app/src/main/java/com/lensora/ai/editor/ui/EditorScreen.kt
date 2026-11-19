package com.lensora.ai.editor.ui

import android.graphics.SurfaceTexture
import android.net.Uri
import android.view.Surface
import android.view.TextureView
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.lensora.ai.editor.Clip
import com.lensora.ai.editor.ClipType
import com.lensora.ai.editor.Project
import com.lensora.ai.editor.TimelineEditEngine
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    project: Project,
    viewModel: ProjectViewModel,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    val totalDurationMs = remember(project.clips) { TimelineEditEngine.totalDurationMs(project) }

    val selectedClip = remember(project.clips, uiState.selectedClipId) {
        project.clips.firstOrNull { it.id == uiState.selectedClipId } ?: project.clips.firstOrNull()
    }

    val exoPlayer: ExoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(project.clips) {
        exoPlayer.clearMediaItems()
        val mediaItems = project.clips.map { clip ->
            val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clip.trimStartMs)
                .setEndPositionMs(clip.trimEndMs)
                .build()

            MediaItem.Builder()
                .setUri(Uri.parse(clip.sourceUri))
                .setClippingConfiguration(clippingConfig)
                .build()
        }
        if (mediaItems.isNotEmpty()) {
            exoPlayer.setMediaItems(mediaItems)
            exoPlayer.prepare()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (!exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_ENDED) {
                isPlaying = false
            }
            delay(100)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project.name,
                            color = ColorWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        val totalSecs = totalDurationMs / 1000
                        Text(
                            text = "Duration: ${totalSecs / 60}m ${totalSecs % 60}s | ${project.clips.size} clips",
                            color = ColorMutedGray,
                            fontSize = 11.sp,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ColorWhite,
                        )
                    }
                },
                actions = {
                    var showResolutionDialog by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { viewModel.undoEdit() },
                        enabled = uiState.canUndo,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) ColorWhite else ColorMutedGray,
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redoEdit() },
                        enabled = uiState.canRedo,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) ColorWhite else ColorMutedGray,
                        )
                    }
                    androidx.compose.material3.Button(
                        onClick = { showResolutionDialog = true },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("Export", color = ColorWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (showResolutionDialog) {
                        androidx.compose.ui.window.Dialog(onDismissRequest = { showResolutionDialog = false }) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text("EXPORT VIDEO", color = RoyalBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Select Resolution", color = ColorWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = {
                                                showResolutionDialog = false
                                                viewModel.startExport(project.id, 720)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("720p HD", color = ColorWhite, fontWeight = FontWeight.Bold)
                                        }

                                        androidx.compose.material3.Button(
                                            onClick = {
                                                showResolutionDialog = false
                                                viewModel.startExport(project.id, 1080)
                                            },
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("1080p FHD", color = ColorWhite, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CharcoalSurface),
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (selectedClip != null && selectedClip.type == ClipType.PHOTO) {
                    AsyncImage(
                        model = selectedClip.sourceUri,
                        contentDescription = "Photo Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                        (exoPlayer as Player).setVideoSurface(Surface(surface))
                                    }
                                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                        exoPlayer.setVideoSurface(null)
                                        return true
                                    }
                                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                                isPlaying = false
                            } else {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                    exoPlayer.seekTo(0, 0L)
                                }
                                exoPlayer.play()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RoyalBlue),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = ColorWhite,
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val maxSliderValue = totalDurationMs.toFloat().coerceAtLeast(1f)
                    val sliderValue = currentPositionMs.toFloat().coerceIn(0f, maxSliderValue)

                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            currentPositionMs = newValue.toLong()
                            exoPlayer.seekTo(currentPositionMs)
                        },
                        valueRange = 0f..maxSliderValue,
                        colors = SliderDefaults.colors(
                            thumbColor = RoyalBlue,
                            activeTrackColor = RoyalBlue,
                            inactiveTrackColor = ColorMutedGray.copy(alpha = 0.3f),
                        ),
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val curSecs = currentPositionMs / 1000
                    val totSecs = totalDurationMs / 1000
                    Text(
                        text = String.format(Locale.US, "%02d:%02d / %02d:%02d", curSecs / 60, curSecs % 60, totSecs / 60, totSecs % 60),
                        color = ColorMutedGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PitchBlackBackground)
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = "TIMELINE CLIPS",
                    color = RoyalBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(project.clips, key = { _, item -> item.id }) { index, clip ->
                        val isSelected = clip.id == selectedClip?.id
                        TimelineClipCard(
                            clip = clip,
                            isSelected = isSelected,
                            onClick = {
                                viewModel.selectClip(clip.id)
                                exoPlayer.seekTo(index, 0L)
                            },
                        )
                    }
                }
            }

            if (selectedClip != null) {
                val clipIndex = project.clips.indexOfFirst { it.id == selectedClip.id }
                ClipEditControlsPanel(
                    clip = selectedClip,
                    clipIndex = clipIndex,
                    totalClips = project.clips.size,
                    onTrimChange = { inMs, outMs ->
                        viewModel.trimClip(selectedClip.id, inMs, outMs)
                    },
                    onSplit = {
                        viewModel.splitClip(selectedClip.id, currentPositionMs)
                    },
                    onMoveLeft = {
                        if (clipIndex > 0) {
                            viewModel.reorderClip(clipIndex, clipIndex - 1)
                        }
                    },
                    onMoveRight = {
                        if (clipIndex < project.clips.size - 1) {
                            viewModel.reorderClip(clipIndex, clipIndex + 1)
                        }
                    },
                    onDelete = {
                        viewModel.deleteClip(selectedClip.id)
                        Toast.makeText(context, "Clip Removed", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }

        // Export Progress & Error Dialogs
        if (uiState.isExporting) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.cancelExport() }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("EXPORTING VIDEO", color = RoyalBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Rendering ${project.name}...", color = ColorWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        val progress = uiState.exportProgress / 100f
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            color = RoyalBlue,
                            trackColor = PitchBlackBackground,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${uiState.exportProgress}%", color = ColorMutedGray, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(20.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { viewModel.cancelExport() },
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Cancel Export", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (uiState.exportError != null) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { viewModel.dismissExportError() }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("EXPORT FAILED", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(uiState.exportError!!, color = ColorWhite, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        androidx.compose.material3.Button(
                            onClick = { viewModel.dismissExportError() },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("OK", color = ColorWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineClipCard(
    clip: Clip,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .width(110.dp)
            .height(90.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) RoyalBlue else ColorWhite.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
            )
            .clickable { onClick() },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = clip.sourceUri,
                contentDescription = "Clip Thumb",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                val spanSecs = (clip.trimEndMs - clip.trimStartMs) / 1000f
                Text(
                    text = String.format(Locale.US, "%.1fs", spanSecs),
                    color = ColorWhite,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (clip.type == ClipType.VIDEO) RoyalBlue else Color(0xFF00E5FF))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(
                    text = clip.type.name,
                    color = if (clip.type == ClipType.VIDEO) ColorWhite else PitchBlackBackground,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ClipEditControlsPanel(
    clip: Clip,
    clipIndex: Int,
    totalClips: Int,
    onTrimChange: (inMs: Long, outMs: Long) -> Unit,
    onSplit: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit,
) {
    var rangeStart by remember(clip) { mutableFloatStateOf(clip.trimStartMs.toFloat()) }
    var rangeEnd by remember(clip) { mutableFloatStateOf(clip.trimEndMs.toFloat()) }
    val maxSpan = remember(clip) {
        if (clip.durationMs <= 0L && clip.type == ClipType.PHOTO) 3000f else clip.durationMs.toFloat().coerceAtLeast(100f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TRIM & EDIT CLIP (${clipIndex + 1}/$totalClips)",
                    color = ColorWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onMoveLeft,
                        enabled = clipIndex > 0,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Move Left",
                            tint = if (clipIndex > 0) ColorWhite else ColorMutedGray,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(
                        onClick = onMoveRight,
                        enabled = clipIndex < totalClips - 1,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Move Right",
                            tint = if (clipIndex < totalClips - 1) ColorWhite else ColorMutedGray,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(onClick = onSplit) {
                        Icon(
                            imageVector = Icons.Filled.ContentCut,
                            contentDescription = "Split Clip",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Clip",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            RangeSlider(
                value = rangeStart..rangeEnd,
                onValueChange = { range ->
                    rangeStart = range.start
                    rangeEnd = range.endInclusive
                    onTrimChange(rangeStart.toLong(), rangeEnd.toLong())
                },
                valueRange = 0f..maxSpan,
                colors = SliderDefaults.colors(
                    thumbColor = RoyalBlue,
                    activeTrackColor = RoyalBlue,
                    inactiveTrackColor = ColorMutedGray.copy(alpha = 0.3f),
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = String.format(Locale.US, "In: %.1fs", rangeStart / 1000f),
                    color = ColorMutedGray,
                    fontSize = 10.sp,
                )
                Text(
                    text = String.format(Locale.US, "Out: %.1fs", rangeEnd / 1000f),
                    color = ColorMutedGray,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
