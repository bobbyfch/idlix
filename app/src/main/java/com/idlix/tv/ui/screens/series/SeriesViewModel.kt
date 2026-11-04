package com.idlix.tv.ui.screens.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idlix.tv.data.model.MovieItem
import com.idlix.tv.data.model.SeriesItem
import com.idlix.tv.data.repository.IdlixRepository
import com.idlix.tv.ui.screens.browse.distinctCatalogShelves
import com.idlix.tv.ui.screens.browse.CatalogShelf
import com.idlix.tv.ui.screens.browse.shelfItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException

data class SeriesUiState(
    val isLoading: Boolean = true,
    val heroSeries: MovieItem? = null,
    val popularSeries: List<MovieItem> = emptyList(),
    val latestSeries: List<MovieItem> = emptyList(),
    val dramaSeries: List<MovieItem> = emptyList(),
    val actionSeries: List<MovieItem> = emptyList(),
    val comedySeries: List<MovieItem> = emptyList(),
    val extraShelves: List<CatalogShelf> = emptyList(),
    val error: String? = null
)

class SeriesViewModel(
    private val repository: IdlixRepository = IdlixRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    init {
        loadSeriesData()
    }

    private var catalogLoadJob: Job? = null

    fun loadSeriesData() {
        catalogLoadJob?.cancel()
        catalogLoadJob = viewModelScope.launch {
            // Never append editorial rails from an older refresh.
            _uiState.value = _uiState.value.copy(
                isLoading = true, error = null, extraShelves = emptyList()
            )
            try {
                coroutineScope {
                    val popularDef = async { repository.getPopularSeries().map { it.toMovieItem() } }
                    val latestDef = async { repository.getSeriesBySort("createdAt").map { it.toMovieItem() } }
                    val dramaDef = async { repository.getSeriesByGenre("drama").map { it.toMovieItem() } }
                    val actionDef = async { repository.getSeriesByGenre("action").map { it.toMovieItem() } }
                    val comedyDef = async { repository.getSeriesByGenre("comedy").map { it.toMovieItem() } }

                    val popular = popularDef.await()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        heroSeries = popular.firstOrNull(),
                        popularSeries = shelfItems(popular)
                    )
                    val latestRaw = latestDef.await()
                    val latest = latestRaw
                    val drama = dramaDef.await()
                    val action = actionDef.await()
                    val comedy = comedyDef.await()

                    val rails = distinctCatalogShelves(
                        listOf(popular, latest, drama, action, comedy)
                    )
                    _uiState.value = SeriesUiState(
                        isLoading = false,
                        heroSeries = popular.firstOrNull(),
                        popularSeries = rails[0],
                        latestSeries = rails[1],
                        dramaSeries = rails[2],
                        actionSeries = rails[3],
                        comedySeries = rails[4]
                    )
                }

                // Defer extra editorial shelves until the first screen is rendered.
                // Only two genre requests at a time to avoid a burst of heavy
                // poster/network work on mobile and low-powered TV devices.
                delay(650)
                val editorial = listOf(
                    Triple("Misteri", "series", "mystery"),
                    Triple("Kriminal", "series", "crime"),
                    Triple("Animasi & Anime", "series", "animation"),
                    Triple("Fantasi", "series", "fantasy"),
                    Triple("Sci-Fi", "series", "sci-fi")
                )
                for (batch in editorial.chunked(2)) {
                    val shelves = coroutineScope {
                        batch.map { (title, type, genre) ->
                            async {
                                val items = if (type == "series") {
                                    repository.getSeriesByGenre(genre).map { it.toMovieItem() }
                                } else repository.getMoviesByGenre(genre)
                                CatalogShelf(title, shelfItems(items))
                            }
                        }.map { it.await() }
                    }
                    _uiState.value = _uiState.value.copy(
                        extraShelves = _uiState.value.extraShelves + shelves.filter { it.items.isNotEmpty() }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Gagal memuat katalog serial TV"
                )
            }
        }
    }

    fun setHeroMovie(movie: MovieItem) {
        _uiState.value = _uiState.value.copy(heroSeries = movie)
    }
}
