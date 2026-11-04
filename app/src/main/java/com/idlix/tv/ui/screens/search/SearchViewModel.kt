package com.idlix.tv.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idlix.tv.data.model.SearchResultItem
import com.idlix.tv.data.repository.SiteCollection
import com.idlix.tv.data.repository.IdlixRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<SearchResultItem> = emptyList(),
    val error: String? = null,
    val collections: List<SiteCollection> = emptyList(),
    val collectionsLoading: Boolean = false,
    val collectionsFromWebsite: Boolean = false
)

class SearchViewModel(
    private val repository: IdlixRepository = IdlixRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var collectionsJob: Job? = null
    private var collectionsLoaded = false

    /** TV and mobile share the same collection index; no N-title API enrichment. */
    fun loadCollections() {
        if (collectionsLoaded || collectionsJob?.isActive == true) return
        collectionsJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(collectionsLoading = true)
            try {
                val listing = repository.getCollectionTitles()
                _uiState.value = _uiState.value.copy(
                    collections = listing.collections,
                    collectionsFromWebsite = listing.fromWebsite,
                    collectionsLoading = false
                )
                collectionsLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                collectionsLoaded = false
                _uiState.value = _uiState.value.copy(collectionsLoading = false)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        searchJob?.cancel()
        val trimmed = newQuery.trim()
        // Avoid showing stale results for a new query during the debounce window.
        _uiState.value = _uiState.value.copy(
            query = newQuery,
            results = emptyList(),
            isLoading = trimmed.isNotEmpty(),
            error = null
        )

        if (trimmed.isEmpty()) return

        searchJob = viewModelScope.launch {
            delay(400) // debounce
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val results = repository.searchStrict(newQuery.trim())
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    results = results
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Pencarian gagal"
                )
            }
        }
    }
}
