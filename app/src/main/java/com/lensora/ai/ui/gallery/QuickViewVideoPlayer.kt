package com.lensora.ai.ui.gallery

import android.net.Uri
import android.util.Log
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun QuickViewVideoPlayer(
    uri: Uri,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val tag = "QuickViewVideoPlayer"

    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isControlsVisible by remember { mutableStateOf(true) }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isDraggingSeekBar by remember { mutableStateOf(false) }
    var seekSliderPositionMs by remember { mutableFloatStateOf(0f) }

    val player = remember(context, uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.height > 0) {
                    val width = videoSize.width.toFloat()
                    val height = videoSize.height.toFloat()
                    val par = if (videoSize.pixelWidthHeightRatio > 0f) videoSize.pixelWidthHeightRatio else 1.0f
                    videoAspect = (width * par) / height
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(tag, "Playback error for $uri: ${error.localizedMessage}", error)
                hasError = true
                errorMessage = error.localizedMessage ?: "Video Playback Error"
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
            Log.i(tag, "Released ExoPlayer for $uri")
        }
    }

    // Auto-pause when page scrolls away
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && player.isPlaying) {
            player.pause()
        }
    }

    // 200ms position polling loop (only runs while playing and user is not dragging seek bar)
    LaunchedEffect(isPlaying, isDraggingSeekBar) {
        while (isActive && isPlaying && !isDraggingSeekBar) {
            currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.coerceAtLeast(0L)
            delay(200)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                isControlsVisible = !isControlsVisible
            },
        contentAlignment = Alignment.Center,
    ) {
        if (hasError) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CharcoalSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Video Unavailable",
                        color = ColorWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Unable to play video file",
                        color = ColorMutedGray,
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            // Video TextureView Container with Dynamic Aspect Ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(videoAspect),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            player.setVideoTextureView(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Controls Overlay
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PitchBlackBackground.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    // Center Play / Pause Button (48dp Touch Target)
                    IconButton(
                        onClick = {
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                if (player.playbackState == Player.STATE_ENDED) {
                                    player.seekTo(0)
                                }
                                player.play()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(CharcoalSurface.copy(alpha = 0.85f)),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = ColorWhite,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    // Bottom Seek Bar & Player Controls Bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .background(CharcoalSurface.copy(alpha = 0.85f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        val displayPosMs = if (isDraggingSeekBar) seekSliderPositionMs.toLong() else currentPositionMs
                        val totalDurationMs = if (durationMs > 0) durationMs else player.duration.coerceAtLeast(0L)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${formatDurationMs(displayPosMs)} / ${formatDurationMs(totalDurationMs)}",
                                color = ColorWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )

                            // Mute / Unmute Button (48dp Touch Target)
                            IconButton(
                                onClick = {
                                    val nextMuted = !isMuted
                                    isMuted = nextMuted
                                    player.volume = if (nextMuted) 0f else 1f
                                },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp,
                                    contentDescription = if (isMuted) "Unmute" else "Mute",
                                    tint = ColorWhite,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        // Seek Bar Slider
                        Slider(
                            value = displayPosMs.toFloat().coerceIn(0f, totalDurationMs.toFloat().coerceAtLeast(1f)),
                            onValueChange = { pos ->
                                isDraggingSeekBar = true
                                seekSliderPositionMs = pos
                            },
                            onValueChangeFinished = {
                                isDraggingSeekBar = false
                                player.seekTo(seekSliderPositionMs.toLong())
                                currentPositionMs = seekSliderPositionMs.toLong()
                            },
                            valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = RoyalBlue,
                                activeTrackColor = RoyalBlue,
                                inactiveTrackColor = ColorMutedGray,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                        )
                    }
                }
            }
        }
    }
}
