package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserSettings
import com.example.data.repository.AriaRepository
import com.example.data.repository.PlaylistDetailData
import com.example.model.MediaTrack
import com.example.model.MvItem
import com.example.model.PlaylistSummary
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val personalizedPlaylists: List<PlaylistSummary> = emptyList(),
    val toplists: List<PlaylistSummary> = emptyList(),
    val dailyRecommendSongs: List<MediaTrack> = emptyList(),
    val featuredMvs: List<MvItem> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val errorMessage: String? = null
)

sealed interface PlaylistDetailUiState {
    data object Loading : PlaylistDetailUiState
    data class Success(val data: PlaylistDetailData) : PlaylistDetailUiState
    data class Error(val message: String) : PlaylistDetailUiState
}

class DiscoverViewModel(
    private val repository: AriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    private val _playlistDetailState = MutableStateFlow<PlaylistDetailUiState>(PlaylistDetailUiState.Loading)
    val playlistDetailState: StateFlow<PlaylistDetailUiState> = _playlistDetailState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            repository.settingsFlow
                .map { it.apiBaseUrl to it.cookie }
                .distinctUntilChanged()
                .collect {
                    refreshAll()
                }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val playlistsDeferred = async { repository.getPersonalizedPlaylists() }
            val toplistsDeferred = async { repository.getToplists() }
            val dailyDeferred = async { repository.getDailyRecommendSongs() }
            val mvsDeferred = async { repository.getFeaturedMvs() }

            val playlistsRes = playlistsDeferred.await()
            val toplistsRes = toplistsDeferred.await()
            val dailyRes = dailyDeferred.await()
            val mvsRes = mvsDeferred.await()

            val firstError = listOf(playlistsRes, toplistsRes, mvsRes)
                .firstOrNull { it.isFailure }
                ?.exceptionOrNull()
                ?.message

            _uiState.update {
                it.copy(
                    isLoading = false,
                    personalizedPlaylists = playlistsRes.getOrDefault(emptyList()),
                    toplists = toplistsRes.getOrDefault(emptyList()),
                    dailyRecommendSongs = dailyRes.getOrDefault(emptyList()),
                    featuredMvs = mvsRes.getOrDefault(emptyList()),
                    errorMessage = if (playlistsRes.isFailure && toplistsRes.isFailure) {
                        firstError ?: "加载音乐数据失败，请检查网络连接或服务端地址"
                    } else null
                )
            }
        }
    }

    fun loadPlaylistDetail(playlistId: Long) {
        viewModelScope.launch {
            _playlistDetailState.value = PlaylistDetailUiState.Loading
            repository.getPlaylistDetail(playlistId)
                .onSuccess { data ->
                    _playlistDetailState.value = PlaylistDetailUiState.Success(data)
                }
                .onFailure { err ->
                    _playlistDetailState.value = PlaylistDetailUiState.Error(
                        err.message ?: "无法加载歌单详情"
                    )
                }
        }
    }
}
