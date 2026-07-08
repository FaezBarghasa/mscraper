package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.MmDlpApi
import com.example.core.MmDlpApiImpl
import com.example.core.ffi.TrackMetadata
import com.example.model.SearchSource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val api: MmDlpApi = MmDlpApiImpl()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSource = MutableStateFlow(SearchSource.YOUTUBE)
    val selectedSource: StateFlow<SearchSource> = _selectedSource.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TrackMetadata>>(emptyList())
    val searchResults: StateFlow<List<TrackMetadata>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    init {
        combine(_searchQuery, _selectedSource) { query, source ->
            query to source
        }
        .debounce(300L)
        .distinctUntilChanged()
        .onEach { (query, source) ->
            if (query.isNotBlank()) {
                performSearch(query, source)
            } else {
                _searchResults.value = emptyList()
            }
        }
        .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedSource(source: SearchSource) {
        _selectedSource.value = source
    }

    private fun performSearch(query: String, source: SearchSource) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = api.search(query, source)
                _searchResults.value = results
            } catch (e: Exception) {
                _errorMessage.emit(e.localizedMessage ?: "Unknown error occurred")
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
