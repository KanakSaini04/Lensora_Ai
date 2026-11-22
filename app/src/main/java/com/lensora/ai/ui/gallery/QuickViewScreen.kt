package com.lensora.ai.ui.gallery

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lensora.ai.R
import com.lensora.ai.data.GalleryFilter
import com.lensora.ai.data.GalleryRepository
import com.lensora.ai.data.MediaItemInfo
import com.lensora.ai.ui.components.ZoomTransformMath
import com.lensora.ai.ui.components.ZoomableBox
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickViewScreen(
    initialUri: String? = null,
    filterName: String? = null,
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val galleryRepository = remember { GalleryRepository(context) }
    var mediaItems by remember { mutableStateOf<List<MediaItemInfo>>(emptyList()) }
    var isLoaded by remember { mutableStateOf(false) }
    var isTopBarVisible by remember { mutableStateOf(true) }

    val decodedInitialUri = remember(initialUri) {
        if (!initialUri.isNullOrBlank()) Uri.decode(initialUri) else null
    }

    val filter = remember(filterName) {
        try {
            if (filterName != null) GalleryFilter.valueOf(filterName) else GalleryFilter.ALL
        } catch (_: Exception) {
            GalleryFilter.ALL
        }
    }

    LaunchedEffect(filterName, initialUri) {
        val items = galleryRepository.getFilteredMediaItems(filter)
        mediaItems = items
        isLoaded = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlackBackground),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoaded && mediaItems.isNotEmpty()) {
            val initialIndex = remember(mediaItems, decodedInitialUri) {
                ZoomTransformMath.resolveInitialPageIndex(
                    decodedInitialUri,
                    mediaItems.map { it.uri.toString() },
                )
            }

            val pagerState = rememberPagerState(
                initialPage = initialIndex,
                pageCount = { mediaItems.size },
            )

            var currentZoomScale by remember { mutableFloatStateOf(1.0f) }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = currentZoomScale <= 1.05f,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        isTopBarVisible = !isTopBarVisible
                    },
            ) { page ->
                val item = mediaItems[page]

                if (item.isVideo) {
                    QuickViewVideoPlayer(
                        uri = item.uri,
                        isCurrentPage = (pagerState.currentPage == page),
                        onTap = { isTopBarVisible = !isTopBarVisible },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    ZoomableBox(
                        maxScale = 8.0f,
                        resetKey = page,
                        onScaleChanged = { scale ->
                            if (page == pagerState.currentPage) {
                                currentZoomScale = scale
                            }
                        },
                    ) { _, _ ->
                        AsyncImage(
                            model = item.uri,
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        } else if (isLoaded) {
            val emptyMessage = if (filter == GalleryFilter.ALL) {
                stringResource(R.string.no_photos_found)
            } else {
                stringResource(R.string.no_media_for_filter)
            }
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.titleLarge,
                color = ColorMutedGray,
            )
        }

        // Slim Translucent Overlay Top Bar (Toggled on single tap)
        AnimatedVisibility(
            visible = isTopBarVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CharcoalSurface.copy(alpha = 0.8f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ColorWhite,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val subtitle = if (mediaItems.isNotEmpty()) " (${mediaItems.size})" else ""
                Text(
                    text = "Quick View$subtitle",
                    color = ColorWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
        }
    }
}
