package com.lensora.ai.ui.gallery

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lensora.ai.R
import com.lensora.ai.data.ExifMetadata
import com.lensora.ai.data.GalleryFilter
import com.lensora.ai.data.GalleryRepository
import com.lensora.ai.data.MediaItemInfo
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onPhotoClick: (Uri, String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val galleryRepository = remember { GalleryRepository(context) }
    val thumbnailLoader = remember { VideoThumbnailLoader(context) }
    var mediaItems by remember { mutableStateOf<List<MediaItemInfo>>(emptyList()) }
    var activeFilter by remember { mutableStateOf(GalleryFilter.ALL) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedExifUri by remember { mutableStateOf<Uri?>(null) }
    var exifData by remember { mutableStateOf<ExifMetadata?>(null) }

    LaunchedEffect(activeFilter) {
        isLoading = true
        mediaItems = galleryRepository.getFilteredMediaItems(activeFilter)
        isLoading = false
    }

    LaunchedEffect(selectedExifUri) {
        val uri = selectedExifUri
        if (uri != null) {
            exifData = galleryRepository.getExifMetadata(uri)
        } else {
            exifData = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = if (mediaItems.isNotEmpty()) {
                        stringResource(R.string.gallery_title_count, mediaItems.size)
                    } else {
                        stringResource(R.string.gallery_title)
                    }
                    Text(
                        text = titleText,
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
            // Category Filter Chips Bar (All | Photos | Videos | RAW DNG | Edited)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(GalleryFilter.entries) { filter ->
                    val isSelected = activeFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) RoyalBlue else PitchBlackBackground)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) RoyalBlue else ColorWhite.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = filter.displayName,
                            color = ColorWhite,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            color = RoyalBlue,
                        )
                    }
                    mediaItems.isNotEmpty() -> {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing = 10.dp,
                        ) {
                            itemsIndexed(mediaItems, key = { _, item -> item.uri.toString() }) { index, item ->
                                val itemHeight = remember(index) {
                                    when (index % 5) {
                                        0 -> 240.dp
                                        1 -> 180.dp
                                        2 -> 310.dp
                                        3 -> 200.dp
                                        else -> 260.dp
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(itemHeight)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CharcoalSurface)
                                        .clickable { onPhotoClick(item.uri, activeFilter.name) },
                                ) {
                                    if (item.isVideo) {
                                        var videoBitmap by remember(item.uri) { mutableStateOf<Bitmap?>(null) }
                                        LaunchedEffect(item.uri) {
                                            videoBitmap = thumbnailLoader.loadVideoThumbnail(item.uri, item.dateAddedMs, item.sizeBytes)
                                        }

                                        val currentBitmap = videoBitmap
                                        if (currentBitmap != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = currentBitmap.asImageBitmap(),
                                                contentDescription = item.displayName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            AsyncImage(
                                                model = item.uri,
                                                contentDescription = item.displayName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    } else {
                                        AsyncImage(
                                            model = item.uri,
                                            contentDescription = item.displayName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }

                                    // Type Badge (VIDEO, RAW, ULTRA-RES, EDITED)
                                    val isAiUpscaled = item.displayName.startsWith("LENSORA_AI_PROCESSED_", ignoreCase = true)
                                    val badgeText = when {
                                        item.isVideo -> "VIDEO"
                                        item.isRaw -> "RAW"
                                        isAiUpscaled -> "✨ ULTRA-RES"
                                        item.isEdited -> "EDITED"
                                        else -> null
                                    }

                                    if (badgeText != null) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    when (badgeText) {
                                                        "VIDEO" -> RoyalBlue
                                                        "RAW" -> Color(0xFFEAB308)
                                                        else -> Color(0xFF00E5FF)
                                                    }
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = badgeText,
                                                color = if (badgeText == "EDITED" || badgeText == "✨ ULTRA-RES") PitchBlackBackground else ColorWhite,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }

                                    // Video Duration Pill (Bottom Left)
                                    if (item.isVideo) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(PitchBlackBackground.copy(alpha = 0.75f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text(
                                                text = formatDurationMs(item.durationMs),
                                                color = ColorWhite,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }

                                    // EXIF Inspector Button (Bottom Right)
                                    IconButton(
                                        onClick = { selectedExifUri = item.uri },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(PitchBlackBackground.copy(alpha = 0.7f)),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = "EXIF Info",
                                            tint = ColorWhite,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = ColorMutedGray,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No media found for ${activeFilter.displayName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = ColorMutedGray,
                            )
                        }
                    }
                }
            }
        }
    }

    // EXIF Metadata Inspector Modal Bottom Sheet
    if (selectedExifUri != null && exifData != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedExifUri = null },
            containerColor = CharcoalSurface,
            scrimColor = PitchBlackBackground.copy(alpha = 0.75f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "EXIF METADATA INSPECTOR",
                    color = RoyalBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                val data = exifData!!
                Text(text = data.fileName, color = ColorWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Size", color = ColorMutedGray, fontSize = 11.sp)
                        Text(data.fileSizeFormatted, color = ColorWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Resolution", color = ColorMutedGray, fontSize = 11.sp)
                        Text(data.resolution, color = ColorWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Date", color = ColorMutedGray, fontSize = 11.sp)
                        Text(data.dateFormatted, color = ColorWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Format", color = ColorMutedGray, fontSize = 11.sp)
                        Text(data.mimeType, color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (data.iso != null || data.exposureTime != null || data.fNumber != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (data.iso != null) {
                            Column {
                                Text("ISO", color = ColorMutedGray, fontSize = 11.sp)
                                Text("ISO ${data.iso}", color = ColorWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (data.exposureTime != null) {
                            Column {
                                Text("Shutter", color = ColorMutedGray, fontSize = 11.sp)
                                Text("${data.exposureTime}s", color = ColorWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (data.fNumber != null) {
                            Column {
                                Text("Aperture", color = ColorMutedGray, fontSize = 11.sp)
                                Text("f/${data.fNumber}", color = ColorWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
