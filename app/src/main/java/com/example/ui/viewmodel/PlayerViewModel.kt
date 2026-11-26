package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.example.data.local.UserSettings
import com.example.data.repository.AriaRepository
import com.example.media.PlaybackController
import com.example.media.PlaybackSnapshot
import com.example.model.CoverPaletteExtractor
import com.example.model.DynamicPaletteState
import com.example.model.LrcParser
import com.example.model.LyricLine
import com.example.model.MediaKind
import com.example.model.MediaSourceType
import com.example.model.MediaTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PlayerPanelTab {
    COVER,
    LYRICS
}

data class PlayerExtraState(
    val isFullPlayerOpen: Boolean = false,
    val activeTab: PlayerPanelTab = PlayerPanelTab.COVER,
    val isVideoFullscreen: Boolean = false,
    val lyrics: List<LyricLine> = emptyList(),
    val isLyricsLoading: Boolean = false,
    val palette: DynamicPaletteState = DynamicPaletteState(),
    val lastAudioTrack: MediaTrack? = null,
    val lastAudioQueue: List<MediaTrack> = emptyList(),
    val isResolvingMv: Boolean = false,
    val snackbarMessage: String? = null
)

data class PlayerUiState(
    val playback: PlaybackSnapshot = PlaybackSnapshot(),
    val extra: PlayerExtraState = PlayerExtraState(),
    val settings: UserSettings = UserSettings(),
    val isCurrentFavorite: Boolean = false,
    val activeLyricIndex: Int = -1
)

class PlayerViewModel(
    application: Application,
    private val repository: AriaRepository,
    private val playbackController: PlaybackController
) : AndroidViewModel(application) {

    private val extraState = MutableStateFlow(PlayerExtraState())

    val playerFlow: StateFlow<Player?> = playbackController.playerFlow
    val playHistoryFlow: StateFlow<List<MediaTrack>> = repository.playHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val localMediaFlow: StateFlow<List<MediaTrack>> = repository.localMediaFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoritesFlow: StateFlow<List<MediaTrack>> = repository.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<PlayerUiState> = combine(
        playbackController.snapshot,
        extraState,
        repository.settingsFlow,
        favoritesFlow
    ) { snapshot, extra, settings, favorites ->
        val currentId = snapshot.currentTrack?.id
        val isFav = currentId != null && favorites.any { it.id == currentId }
        val lyricIdx = LrcParser.findActiveIndex(extra.lyrics, snapshot.positionMs)
        PlayerUiState(
            playback = snapshot,
            extra = extra,
            settings = settings,
            isCurrentFavorite = isFav,
            activeLyricIndex = lyricIdx
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerUiState())

    init {
        playbackController.connect()

        // Extract dynamic cover palette whenever coverUrl changes
        viewModelScope.launch {
            playbackController.snapshot
                .map { it.currentTrack?.id to it.currentTrack?.coverUrl }
                .distinctUntilChanged()
                .collect { (trackId, coverUrl) ->
                    val palette = CoverPaletteExtractor.extract(getApplication(), coverUrl)
                    extraState.update { it.copy(palette = palette) }
                    if (trackId != null) {
                        val track = playbackController.snapshot.value.currentTrack
                        if (track != null && !track.isVideo) {
                            loadLyricsFor(track)
                        } else {
                            extraState.update { it.copy(lyrics = emptyList(), isLyricsLoading = false) }
                        }
                    }
                }
        }
    }

    private fun loadLyricsFor(track: MediaTrack) {
        viewModelScope.launch {
            extraState.update { it.copy(isLyricsLoading = true, lyrics = emptyList()) }
            val parsed = repository.getLyrics(track)
            if (playbackController.snapshot.value.currentTrack?.id == track.id) {
                extraState.update { it.copy(lyrics = parsed, isLyricsLoading = false) }
            }
        }
    }

    fun playTrack(track: MediaTrack, queue: List<MediaTrack> = listOf(track), openFull: Boolean = false) {
        if (!track.isVideo) {
            extraState.update {
                it.copy(
                    lastAudioTrack = track,
                    lastAudioQueue = queue,
                    isFullPlayerOpen = if (openFull) true else it.isFullPlayerOpen
                )
            }
        } else if (openFull) {
            extraState.update { it.copy(isFullPlayerOpen = true) }
        }
        playbackController.playTrack(track, queue)
    }

    fun playMv(
        mvId: Long,
        title: String = "",
        artist: String = "",
        coverUrl: String? = null,
        openFull: Boolean = true
    ) {
        if (mvId <= 0L) return
        val current = playbackController.snapshot.value.currentTrack
        val currentQueue = playbackController.snapshot.value.queue
        if (current != null && !current.isVideo) {
            extraState.update {
                it.copy(lastAudioTrack = current, lastAudioQueue = currentQueue)
            }
        }
        viewModelScope.launch {
            extraState.update { it.copy(isResolvingMv = true) }
            repository.resolveMvPlaybackTrack(mvId, title, artist, coverUrl)
                .onSuccess { mvTrack ->
                    extraState.update {
                        it.copy(
                            isResolvingMv = false,
                            isFullPlayerOpen = if (openFull) true else it.isFullPlayerOpen
                        )
                    }
                    playbackController.playTrack(mvTrack, listOf(mvTrack))
                }
                .onFailure { err ->
                    extraState.update {
                        it.copy(
                            isResolvingMv = false,
                            snackbarMessage = err.message ?: "无法加载 MV 视频流"
                        )
                    }
                }
        }
    }

    fun switchBetweenAudioAndMv() {
        val current = playbackController.snapshot.value.currentTrack ?: return
        if (current.isVideo) {
            val prevAudio = extraState.value.lastAudioTrack
            if (prevAudio != null) {
                playbackController.playTrack(prevAudio, extraState.value.lastAudioQueue.ifEmpty { listOf(prevAudio) })
            } else {
                extraState.update { it.copy(snackbarMessage = "暂无可切回的关联音频曲目") }
            }
        } else if (current.hasMv) {
            playMv(current.mvId, current.title, current.artist, current.coverUrl, openFull = true)
        }
    }

    fun openFullPlayer() {
        extraState.update { it.copy(isFullPlayerOpen = true) }
    }

    fun closeFullPlayer() {
        extraState.update { it.copy(isFullPlayerOpen = false, isVideoFullscreen = false) }
    }

    fun selectPlayerTab(tab: PlayerPanelTab) {
        extraState.update { it.copy(activeTab = tab) }
    }

    fun toggleVideoFullscreen() {
        extraState.update { it.copy(isVideoFullscreen = !it.isVideoFullscreen) }
    }

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)

    fun skipToNext() = playbackController.skipToNext()

    fun skipToPrevious() = playbackController.skipToPrevious()

    fun skipToQueueIndex(index: Int) = playbackController.skipToQueueIndex(index)

    fun cycleRepeatMode() = playbackController.cycleRepeatMode()

    fun toggleShuffle() = playbackController.toggleShuffle()

    fun toggleFavorite(track: MediaTrack) {
        viewModelScope.launch {
            val isNowFav = repository.toggleFavorite(track)
            extraState.update {
                it.copy(snackbarMessage = if (isNowFav) "已添加到我喜欢" else "已从我喜欢移除")
            }
        }
    }

    fun importLocalUri(uri: Uri, forceVideo: Boolean = false) {
        viewModelScope.launch {
            repository.importLocalUri(uri, forceVideo)
                .onSuccess { track ->
                    playTrack(track, listOf(track), openFull = true)
                    extraState.update {
                        it.copy(snackbarMessage = "已导入并开始播放：${track.title}")
                    }
                }
                .onFailure { err ->
                    extraState.update {
                        it.copy(snackbarMessage = err.message ?: "导入本地文件失败")
                    }
                }
        }
    }

    fun playDirectStreamUrl(url: String, title: String, isVideo: Boolean) {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) {
            extraState.update { it.copy(snackbarMessage = "请输入有效的音视频流 URL") }
            return
        }
        val track = MediaTrack(
            id = "url_${trimmedUrl.hashCode()}",
            title = title.ifBlank { trimmedUrl.substringAfterLast('/').substringBefore('?').ifBlank { "网络流媒体" } },
            artist = "网络直链",
            album = if (isVideo) "在线视频" else "在线音频",
            coverUrl = null,
            streamUrl = trimmedUrl,
            durationMs = 0L,
            sourceType = MediaSourceType.DIRECT_URL,
            mediaKind = if (isVideo) MediaKind.VIDEO else MediaKind.AUDIO
        )
        playTrack(track, listOf(track), openFull = true)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearPlayHistory()
            extraState.update { it.copy(snackbarMessage = "已清空播放历史") }
        }
    }

    fun consumeSnackbar() {
        extraState.update { it.copy(snackbarMessage = null) }
    }

    fun clearPlaybackError() {
        playbackController.clearError()
    }
}
