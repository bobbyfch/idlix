package com.idlix.tv.ui.screens.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idlix.tv.data.model.MovieItem
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

data class MoviesUiState(
    val isLoading: Boolean = true,
    val heroMovie: MovieItem? = null,
    val popularMovies: List<MovieItem> = emptyList(),
    val latestMovies: List<MovieItem> = emptyList(),
    val actionMovies: List<MovieItem> = emptyList(),
    val comedyMovies: List<MovieItem> = emptyList(),
    val horrorMovies: List<MovieItem> = emptyList(),
    val sciFiMovies: List<MovieItem> = emptyList(),
    val extraShelves: List<CatalogShelf> = emptyList(),
    val error: String? = null
)

class MoviesViewModel(
    private val repository: IdlixRepository = IdlixRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        loadMoviesData()
    }

    private var catalogLoadJob: Job? = null

    fun loadMoviesData() {
        catalogLoadJob?.cancel()
        catalogLoadJob = viewModelScope.launch {
            // Never append editorial rails from an older refresh.
            _uiState.value = _uiState.value.copy(
                isLoading = true, error = null, extraShelves = emptyList()
            )
            try {
                coroutineScope {
                    val popularDef = async { repository.getPopularMovies() }
                    val latestDef = async { repository.getMoviesBySort("createdAt") }
                    val actionDef = async { repository.getMoviesByGenre("action") }
                    val comedyDef = async { repository.getMoviesByGenre("comedy") }
                    val horrorDef = async { repository.getMoviesByGenre("horror") }
                    val sciFiDef = async { repository.getMoviesByGenre("science-fiction") }

                    val popular = popularDef.await()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        heroMovie = popular.firstOrNull(),
                        popularMovies = shelfItems(popular)
                    )
                    val latest = latestDef.await()
                    val action = actionDef.await()
                    val comedy = comedyDef.await()
                    val horror = horrorDef.await()
                    val sciFi = sciFiDef.await()

                    val rails = distinctCatalogShelves(
                        listOf(popular, latest, action, comedy, horror, sciFi)
                    )
                    _uiState.value = MoviesUiState(
                        isLoading = false,
                        heroMovie = popular.firstOrNull(),
                        popularMovies = rails[0],
                        latestMovies = rails[1],
                        actionMovies = rails[2],
                        comedyMovies = rails[3],
                        horrorMovies = rails[4],
                        sciFiMovies = rails[5]
                    )
                }

                // Defer extra editorial shelves until the first screen is rendered.
                // Only two genre requests at a time to avoid a burst of heavy
                // poster/network work on mobile and low-powered TV devices.
                delay(650)
                val editorial = listOf(
                    Triple("Petualangan", "movie", "adventure"),
                    Triple("Thriller", "movie", "thriller"),
                    Triple("Romansa", "movie", "romance"),
                    Triple("Animasi", "movie", "animation"),
                    Triple("Keluarga", "movie", "family"),
                    Triple("Misteri", "movie", "mystery")
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
                    error = e.localizedMessage ?: "Gagal memuat katalog film"
                )
            }
        }
    }

    fun setHeroMovie(movie: MovieItem) {
        _uiState.value = _uiState.value.copy(heroMovie = movie)
    }
}
