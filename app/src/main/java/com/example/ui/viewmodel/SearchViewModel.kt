package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AriaRepository
import com.example.data.repository.SearchBundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchCategory(val apiType: Int, val label: String) {
    SONGS(1, "单曲"),
    PLAYLISTS(1000, "歌单"),
    MVS(1004, "MV 视频")
}

data class SearchUiState(
    val query: String = "",
    val category: SearchCategory = SearchCategory.SONGS,
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val results: SearchBundle = SearchBundle(),
    val errorMessage: String? = null
)

class SearchViewModel(
    private val repository: AriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val searchHistoryFlow: StateFlow<List<String>> = repository.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun selectCategory(category: SearchCategory) {
        _uiState.update { it.copy(category = category) }
        if (_uiState.value.query.isNotBlank()) {
            submitSearch()
        }
    }

    fun submitSearch(overrideQuery: String? = null) {
        val q = (overrideQuery ?: _uiState.value.query).trim()
        if (q.isEmpty()) return
        _uiState.update { it.copy(query = q, isLoading = true, errorMessage = null, hasSearched = true) }
        val category = _uiState.value.category
        viewModelScope.launch {
            repository.search(q, category.apiType)
                .onSuccess { bundle ->
                    _uiState.update {
                        it.copy(isLoading = false, results = bundle, errorMessage = null)
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "搜索失败，请检查网络连接"
                        )
                    }
                }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearSearchHistory() }
    }
}
