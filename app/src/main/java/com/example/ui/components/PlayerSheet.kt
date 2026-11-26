package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.R
import com.example.model.DynamicPaletteState
import com.example.model.MediaTrack
import com.example.model.RepeatModeOption
import com.example.model.formatDuration
import com.example.ui.theme.AriaMonoTimeStyle
import com.example.ui.viewmodel.PlayerPanelTab
import com.example.ui.viewmodel.PlayerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSheet(
    uiState: PlayerUiState,
    player: Player?,
    onClose: () -> Unit,
    onSelectTab: (PlayerPanelTab) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipToQueueIndex: (Int) -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: (MediaTrack) -> Unit,
    onSwitchAudioMv: () -> Unit,
    onToggleVideoFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = uiState.playback.currentTrack ?: return
    val rawPalette = uiState.extra.palette
    var showQueueSheet by remember { mutableStateOf(false) }

    BackHandler {
        if (uiState.extra.isVideoFullscreen) {
            onToggleVideoFullscreen()
        } else {
            onClose()
        }
    }

    val containerColor by animateColorAsState(
        targetValue = rawPalette.containerColor,
        animationSpec = tween(550),
        label = "bgContainer"
    )
    val glowColor by animateColorAsState(
        targetValue = rawPalette.glowColor,
        animationSpec = tween(550),
        label = "bgGlow"
    )
    val secondaryGlow by animateColorAsState(
        targetValue = rawPalette.secondaryGlowColor,
        animationSpec = tween(550),
        label = "bgSecondaryGlow"
    )
    val accentColor by animateColorAsState(
        targetValue = rawPalette.accentColor,
        animationSpec = tween(550),
        label = "bgAccent"
    )

    val animatedPalette = rawPalette.copy(
        containerColor = containerColor,
        glowColor = glowColor,
        secondaryGlowColor = secondaryGlow,
        accentColor = accentColor
    )

    if (uiState.extra.isVideoFullscreen && track.isVideo) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayerView(
                player = player,
                isPlaying = uiState.playback.isPlaying,
                isBuffering = uiState.playback.isBuffering,
                positionMs = uiState.playback.positionMs,
                durationMs = uiState.playback.durationMs,
                isFullscreen = true,
                canSwitchToAudio = uiState.extra.lastAudioTrack != null,
                palette = animatedPalette,
                onTogglePlayPause = onTogglePlayPause,
                onSeekTo = onSeekTo,
                onToggleFullscreen = onToggleVideoFullscreen,
                onSwitchToAudio = onSwitchAudioMv,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color = containerColor)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.52f), Color.Transparent),
                        center = Offset(size.width * 0.25f, size.height * 0.22f),
                        radius = size.maxDimension * 0.68f
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(secondaryGlow.copy(alpha = 0.48f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.78f),
                        radius = size.maxDimension * 0.64f
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideLayout = maxWidth >= 700.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                PlayerTopBar(
                    track = track,
                    activeTab = uiState.extra.activeTab,
                    isWideLayout = isWideLayout,
                    isResolvingMv = uiState.extra.isResolvingMv,
                    canSwitchToAudio = uiState.extra.lastAudioTrack != null,
                    palette = animatedPalette,
                    onClose = onClose,
                    onSelectTab = onSelectTab,
                    onSwitchAudioMv = onSwitchAudioMv
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (track.isVideo) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 880.dp)
                            .align(Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            VideoPlayerView(
                                player = player,
                                isPlaying = uiState.playback.isPlaying,
                                isBuffering = uiState.playback.isBuffering,
                                positionMs = uiState.playback.positionMs,
                                durationMs = uiState.playback.durationMs,
                                isFullscreen = false,
                                canSwitchToAudio = uiState.extra.lastAudioTrack != null,
                                palette = animatedPalette,
                                onTogglePlayPause = onTogglePlayPause,
                                onSeekTo = onSeekTo,
                                onToggleFullscreen = onToggleVideoFullscreen,
                                onSwitchToAudio = onSwitchAudioMv
                            )
                            TrackMetadataHeader(
                                track = track,
                                isFavorite = uiState.isCurrentFavorite,
                                palette = animatedPalette,
                                onToggleFavorite = { onToggleFavorite(track) }
                            )
                        }
                        TransportControlsBlock(
                            uiState = uiState,
                            palette = animatedPalette,
                            onTogglePlayPause = onTogglePlayPause,
                            onSeekTo = onSeekTo,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrevious,
                            onCycleRepeat = onCycleRepeat,
                            onToggleShuffle = onToggleShuffle,
                            onOpenQueue = { showQueueSheet = true }
                        )
                    }
                } else if (isWideLayout) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            CoverArtHero(
                                track = track,
                                isPlaying = uiState.playback.isPlaying,
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TrackMetadataHeader(
                                track = track,
                                isFavorite = uiState.isCurrentFavorite,
                                palette = animatedPalette,
                                onToggleFavorite = { onToggleFavorite(track) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TransportControlsBlock(
                                uiState = uiState,
                                palette = animatedPalette,
                                onTogglePlayPause = onTogglePlayPause,
                                onSeekTo = onSeekTo,
                                onSkipNext = onSkipNext,
                                onSkipPrevious = onSkipPrevious,
                                onCycleRepeat = onCycleRepeat,
                                onToggleShuffle = onToggleShuffle,
                                onOpenQueue = { showQueueSheet = true }
                            )
                        }

                        LyricsView(
                            lyrics = uiState.extra.lyrics,
                            activeIndex = uiState.activeLyricIndex,
                            isLoading = uiState.extra.isLyricsLoading,
                            showTranslated = uiState.settings.showTranslatedLyrics,
                            fontScale = uiState.settings.lyricFontScale,
                            palette = animatedPalette,
                            onSeekToLine = onSeekTo,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = uiState.extra.activeTab,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        label = "playerTabContent"
                    ) { tab ->
                        when (tab) {
                            PlayerPanelTab.COVER -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CoverArtHero(
                                            track = track,
                                            isPlaying = uiState.playback.isPlaying
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Mini inline lyric preview on Cover tab
                                        val activeLyric = uiState.extra.lyrics.getOrNull(uiState.activeLyricIndex)
                                        if (activeLyric != null) {
                                            Text(
                                                text = activeLyric.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = animatedPalette.accentColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onSelectTab(PlayerPanelTab.LYRICS) }
                                                    .padding(vertical = 2.dp)
                                            )
                                        }
                                        TrackMetadataHeader(
                                            track = track,
                                            isFavorite = uiState.isCurrentFavorite,
                                            palette = animatedPalette,
                                            onToggleFavorite = { onToggleFavorite(track) }
                                        )
                                        TransportControlsBlock(
                                            uiState = uiState,
                                            palette = animatedPalette,
                                            onTogglePlayPause = onTogglePlayPause,
                                            onSeekTo = onSeekTo,
                                            onSkipNext = onSkipNext,
                                            onSkipPrevious = onSkipPrevious,
                                            onCycleRepeat = onCycleRepeat,
                                            onToggleShuffle = onToggleShuffle,
                                            onOpenQueue = { showQueueSheet = true }
                                        )
                                    }
                                }
                            }

                            PlayerPanelTab.LYRICS -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    LyricsView(
                                        lyrics = uiState.extra.lyrics,
                                        activeIndex = uiState.activeLyricIndex,
                                        isLoading = uiState.extra.isLyricsLoading,
                                        showTranslated = uiState.settings.showTranslatedLyrics,
                                        fontScale = uiState.settings.lyricFontScale,
                                        palette = animatedPalette,
                                        onSeekToLine = onSeekTo,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    )
                                    TransportControlsBlock(
                                        uiState = uiState,
                                        palette = animatedPalette,
                                        compactMode = true,
                                        onTogglePlayPause = onTogglePlayPause,
                                        onSeekTo = onSeekTo,
                                        onSkipNext = onSkipNext,
                                        onSkipPrevious = onSkipPrevious,
                                        onCycleRepeat = onCycleRepeat,
                                        onToggleShuffle = onToggleShuffle,
                                        onOpenQueue = { showQueueSheet = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "播放队列 (${uiState.playback.queue.size})",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(360.dp)
                ) {
                    itemsIndexed(uiState.playback.queue) { idx, item ->
                        TrackRowItem(
                            track = item,
                            index = idx,
                            isCurrent = idx == uiState.playback.currentIndex,
                            onClick = {
                                onSkipToQueueIndex(idx)
                                showQueueSheet = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerTopBar(
    track: MediaTrack,
    activeTab: PlayerPanelTab,
    isWideLayout: Boolean,
    isResolvingMv: Boolean,
    canSwitchToAudio: Boolean,
    palette: DynamicPaletteState,
    onClose: () -> Unit,
    onSelectTab: (PlayerPanelTab) -> Unit,
    onSwitchAudioMv: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_collapse_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_collapse_player),
                tint = palette.onContainerColor,
                modifier = Modifier.size(30.dp)
            )
        }

        if (!track.isVideo && !isWideLayout) {
            Surface(
                color = palette.onContainerColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    PlayerPanelTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        Surface(
                            color = if (selected) palette.accentColor else Color.Transparent,
                            contentColor = if (selected) palette.onAccentColor else palette.onContainerColor,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onSelectTab(tab) }
                                .testTag("player_tab_${tab.name.lowercase()}")
                        ) {
                            Text(
                                text = if (tab == PlayerPanelTab.COVER) "歌曲" else "歌词",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = if (track.isVideo) "视频播放" else "正在播放",
                style = MaterialTheme.typography.labelLarge,
                color = palette.mutedOnContainerColor
            )
        }

        when {
            isResolvingMv -> {
                CircularProgressIndicator(
                    color = palette.accentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(22.dp)
                )
            }
            track.isVideo && canSwitchToAudio -> {
                FilledTonalButton(
                    onClick = onSwitchAudioMv,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = palette.onContainerColor.copy(alpha = 0.14f),
                        contentColor = palette.onContainerColor
                    ),
                    modifier = Modifier.testTag("player_switch_audio_button")
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("音频")
                }
            }
            !track.isVideo && track.hasMv -> {
                FilledTonalButton(
                    onClick = onSwitchAudioMv,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = palette.onContainerColor.copy(alpha = 0.14f),
                        contentColor = palette.onContainerColor
                    ),
                    modifier = Modifier.testTag("player_switch_mv_button")
                ) {
                    Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("MV")
                }
            }
            else -> {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun CoverArtHero(
    track: MediaTrack,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "coverScale"
    )

    CoverArtwork(
        coverUrl = track.coverUrl,
        contentDescription = track.title,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth(0.86f)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}

@Composable
private fun TrackMetadataHeader(
    track: MediaTrack,
    isFavorite: Boolean,
    palette: DynamicPaletteState,
    onToggleFavorite: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = palette.onContainerColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(track.artist)
                    if (track.album.isNotBlank()) {
                        append(" — ")
                        append(track.album)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = palette.mutedOnContainerColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_favorite_button")
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFavorite) "取消喜欢" else "喜欢",
                tint = if (isFavorite) palette.accentColor else palette.onContainerColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun TransportControlsBlock(
    uiState: PlayerUiState,
    palette: DynamicPaletteState,
    compactMode: Boolean = false,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenQueue: () -> Unit
) {
    val safeDuration = uiState.playback.durationMs.coerceAtLeast(1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val sliderFraction = if (isDragging) {
        dragFraction
    } else {
        (uiState.playback.positionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (compactMode) 4.dp else 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = sliderFraction,
                onValueChange = {
                    isDragging = true
                    dragFraction = it
                },
                onValueChangeFinished = {
                    onSeekTo((dragFraction * safeDuration).toLong())
                    isDragging = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = palette.accentColor,
                    activeTrackColor = palette.accentColor,
                    inactiveTrackColor = palette.onContainerColor.copy(alpha = 0.22f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_seekbar")
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatDuration(
                        if (isDragging) (dragFraction * safeDuration).toLong() else uiState.playback.positionMs
                    ),
                    style = AriaMonoTimeStyle,
                    color = palette.mutedOnContainerColor
                )
                Text(
                    text = formatDuration(uiState.playback.durationMs),
                    style = AriaMonoTimeStyle,
                    color = palette.mutedOnContainerColor
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("player_shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "随机播放",
                    tint = if (uiState.playback.shuffleEnabled) palette.accentColor else palette.mutedOnContainerColor
                )
            }

            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("player_previous_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.action_previous),
                    tint = palette.onContainerColor,
                    modifier = Modifier.size(34.dp)
                )
            }

            FilledIconButton(
                onClick = onTogglePlayPause,
                shape = RoundedCornerShape(24.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = palette.accentColor,
                    contentColor = palette.onAccentColor
                ),
                modifier = Modifier
                    .size(if (compactMode) 60.dp else 72.dp)
                    .testTag("player_play_pause_button")
            ) {
                if (uiState.playback.isBuffering) {
                    CircularProgressIndicator(
                        color = palette.onAccentColor,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (uiState.playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (uiState.playback.isPlaying) {
                            stringResource(R.string.action_pause)
                        } else {
                            stringResource(R.string.action_play)
                        },
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            IconButton(
                onClick = onSkipNext,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("player_next_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.action_next),
                    tint = palette.onContainerColor,
                    modifier = Modifier.size(34.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_repeat_button")
                ) {
                    val icon = when (uiState.playback.repeatMode) {
                        RepeatModeOption.ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    }
                    val tint = if (uiState.playback.repeatMode == RepeatModeOption.OFF) {
                        palette.mutedOnContainerColor
                    } else {
                        palette.accentColor
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "循环模式",
                        tint = tint
                    )
                }

                if (!compactMode) {
                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_queue_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = stringResource(R.string.action_open_queue),
                            tint = palette.onContainerColor
                        )
                    }
                }
            }
        }
    }
}
