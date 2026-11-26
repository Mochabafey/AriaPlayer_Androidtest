package com.example.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.os.bundleOf
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.repository.AriaRepository
import com.example.model.MediaKind
import com.example.model.MediaTrack
import com.example.model.RepeatModeOption
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackSnapshot(
    val currentTrack: MediaTrack? = null,
    val queue: List<MediaTrack> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatModeOption = RepeatModeOption.ALL,
    val shuffleEnabled: Boolean = false,
    val hasVideoFrame: Boolean = false,
    val errorMessage: String? = null
)

class PlaybackController(
    private val context: Context,
    private val repository: AriaRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null

    private val _snapshot = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    private val _playerFlow = MutableStateFlow<Player?>(null)
    val playerFlow: StateFlow<Player?> = _playerFlow.asStateFlow()

    private val trackRegistry = linkedMapOf<String, MediaTrack>()
    private var currentQueue: List<MediaTrack> = emptyList()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _snapshot.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionTicker() else stopPositionTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            val duration = mediaController?.duration?.takeIf { it > 0L } ?: _snapshot.value.durationMs
            _snapshot.update {
                it.copy(
                    isBuffering = buffering,
                    durationMs = duration,
                    positionMs = mediaController?.currentPosition?.coerceAtLeast(0L) ?: it.positionMs
                )
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = mediaController?.currentMediaItemIndex ?: -1
            val track = mediaItem?.mediaId?.let { trackRegistry[it] }
                ?: currentQueue.getOrNull(index)
            val duration = mediaController?.duration?.takeIf { it > 0L } ?: track?.durationMs ?: 0L
            _snapshot.update {
                it.copy(
                    currentTrack = track,
                    currentIndex = index,
                    positionMs = 0L,
                    durationMs = duration,
                    hasVideoFrame = track?.isVideo == true,
                    errorMessage = null
                )
            }
            if (track != null) {
                scope.launch { repository.recordPlayHistory(track) }
                if (track.streamUrl.isNullOrBlank()) {
                    resolveAndPrepareCurrentIfNeeded(track, index)
                }
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                _snapshot.update { it.copy(hasVideoFrame = true) }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mapped = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatModeOption.ONE
                Player.REPEAT_MODE_ALL -> RepeatModeOption.ALL
                else -> RepeatModeOption.OFF
            }
            _snapshot.update { it.copy(repeatMode = mapped) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _snapshot.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _snapshot.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = error.localizedMessage ?: "媒体播放出错，请检查网络连接或音视频源"
                )
            }
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, AriaPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                val controller = future.get()
                mediaController = controller
                controller.addListener(playerListener)
                controller.repeatMode = Player.REPEAT_MODE_ALL
                _playerFlow.value = controller
                syncFullState(controller)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun syncFullState(controller: Player) {
        val currentId = controller.currentMediaItem?.mediaId
        val track = currentId?.let { trackRegistry[it] } ?: _snapshot.value.currentTrack
        _snapshot.update {
            it.copy(
                currentTrack = track,
                currentIndex = controller.currentMediaItemIndex,
                isPlaying = controller.isPlaying,
                isBuffering = controller.playbackState == Player.STATE_BUFFERING,
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = controller.duration.takeIf { d -> d > 0L } ?: track?.durationMs ?: 0L,
                shuffleEnabled = controller.shuffleModeEnabled
            )
        }
        if (controller.isPlaying) startPositionTicker()
    }

    fun playTrack(track: MediaTrack, queue: List<MediaTrack> = listOf(track)) {
        val effectiveQueue = if (queue.any { it.id == track.id }) queue else listOf(track) + queue
        val targetIndex = effectiveQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        currentQueue = effectiveQueue
        effectiveQueue.forEach { trackRegistry[it.id] = it }

        _snapshot.update {
            it.copy(
                currentTrack = track,
                queue = effectiveQueue,
                currentIndex = targetIndex,
                isBuffering = true,
                positionMs = 0L,
                durationMs = track.durationMs,
                hasVideoFrame = track.isVideo,
                errorMessage = null
            )
        }

        scope.launch {
            val resolvedUrl = if (!track.streamUrl.isNullOrBlank()) {
                Result.success(track.streamUrl)
            } else {
                repository.resolveAudioStreamUrl(track)
            }

            resolvedUrl.onSuccess { url ->
                val resolvedTrack = track.copy(streamUrl = url)
                trackRegistry[resolvedTrack.id] = resolvedTrack
                val updatedQueue = effectiveQueue.map {
                    if (it.id == resolvedTrack.id) resolvedTrack else it
                }
                currentQueue = updatedQueue
                _snapshot.update {
                    it.copy(currentTrack = resolvedTrack, queue = updatedQueue)
                }
                val controller = mediaController
                if (controller != null) {
                    val items = updatedQueue.map { it.toMediaItem() }
                    controller.setMediaItems(items, targetIndex, 0L)
                    controller.prepare()
                    controller.play()
                }
                repository.recordPlayHistory(resolvedTrack)
            }.onFailure { err ->
                _snapshot.update {
                    it.copy(
                        isBuffering = false,
                        errorMessage = err.message ?: "无法解析播放地址"
                    )
                }
            }
        }
    }

    private fun resolveAndPrepareCurrentIfNeeded(track: MediaTrack, index: Int) {
        scope.launch {
            repository.resolveAudioStreamUrl(track).onSuccess { url ->
                val resolvedTrack = track.copy(streamUrl = url)
                trackRegistry[resolvedTrack.id] = resolvedTrack
                currentQueue = currentQueue.map { if (it.id == resolvedTrack.id) resolvedTrack else it }
                _snapshot.update { state ->
                    if (state.currentTrack?.id == resolvedTrack.id) {
                        state.copy(currentTrack = resolvedTrack, queue = currentQueue)
                    } else state
                }
                val controller = mediaController ?: return@onSuccess
                if (controller.currentMediaItemIndex == index) {
                    val wasPlaying = controller.playWhenReady
                    controller.replaceMediaItem(index, resolvedTrack.toMediaItem())
                    controller.prepare()
                    if (wasPlaying) controller.play()
                }
            }.onFailure { err ->
                _snapshot.update {
                    it.copy(
                        isBuffering = false,
                        errorMessage = err.message ?: "无法获取当前歌曲播放地址"
                    )
                }
            }
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE && _snapshot.value.currentTrack != null) {
                controller.prepare()
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val clamped = positionMs.coerceAtLeast(0L)
        controller.seekTo(clamped)
        _snapshot.update { it.copy(positionMs = clamped) }
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        } else if (controller.mediaItemCount > 0) {
            controller.seekTo(0, 0L)
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3500L) {
            controller.seekTo(0L)
        } else if (controller.hasPreviousMediaItem()) {
            controller.seekToPreviousMediaItem()
        } else if (controller.mediaItemCount > 0) {
            controller.seekTo(controller.mediaItemCount - 1, 0L)
        }
    }

    fun skipToQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekTo(index, 0L)
            controller.play()
        } else {
            currentQueue.getOrNull(index)?.let { playTrack(it, currentQueue) }
        }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_ALL
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun clearError() {
        _snapshot.update { it.copy(errorMessage = null) }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0L } ?: _snapshot.value.durationMs
                    _snapshot.update {
                        it.copy(positionMs = pos, durationMs = dur)
                    }
                }
                delay(100L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    fun release() {
        stopPositionTicker()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        _playerFlow.value = null
    }

    private fun MediaTrack.toMediaItem(): MediaItem {
        val uri = streamUrl?.takeIf { it.isNotBlank() }
            ?: if (mediaKind == MediaKind.AUDIO && id.startsWith("ncm_")) {
                "https://music.163.com/song/media/outer/url?id=${id.removePrefix("ncm_")}.mp3"
            } else {
                ""
            }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .apply {
                coverUrl?.takeIf { it.isNotBlank() }?.let {
                    setArtworkUri(Uri.parse(it))
                }
            }
            .setExtras(bundleOf("mvId" to mvId, "isVideo" to isVideo))
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }
}
