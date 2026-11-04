package com.idlix.tv.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idlix.tv.data.local.WatchHistoryStore
import com.idlix.tv.data.model.MovieItem
import com.idlix.tv.data.model.SeriesItem
import com.idlix.tv.data.model.WatchHistoryItem
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

data class HomeUiState(
    val isLoading: Boolean = true,
    val heroMovie: MovieItem? = null,
    val continueWatching: List<WatchHistoryItem> = emptyList(),
    val trending: List<MovieItem> = emptyList(),
    val trendingCountry: List<MovieItem> = emptyList(),
    val networkPicks: List<MovieItem> = emptyList(),
    val popularMovies: List<MovieItem> = emptyList(),
    val popularSeries: List<SeriesItem> = emptyList(),
    val latestMovies: List<MovieItem> = emptyList(),
    val latestSeries: List<MovieItem> = emptyList(),
    val actionMovies: List<MovieItem> = emptyList(),
    val extraShelves: List<CatalogShelf> = emptyList(),
    val error: String? = null
)

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = IdlixRepository()
    private val historyStore = WatchHistoryStore.getInstance(application.applicationContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Collect unfinished watch history items reactively
        viewModelScope.launch {
            historyStore.historyFlow.collect { allItems ->
                val unfinished = allItems.filter { it.isUnfinished }
                _uiState.value = _uiState.value.copy(continueWatching = unfinished)
            }
        }
        loadHomeData()
    }

    private var catalogLoadJob: Job? = null

    fun loadHomeData() {
        catalogLoadJob?.cancel()
        catalogLoadJob = viewModelScope.launch {
            // Never append editorial rails from an older refresh.
            _uiState.value = _uiState.value.copy(
                isLoading = true, error = null, extraShelves = emptyList()
            )
            try {
                coroutineScope {
                    val trendingDeferred = async { repository.getTrendingMovies() }
                    val popularDeferred = async { repository.getPopularMovies() }
                    val seriesDeferred = async { repository.getPopularSeries() }
                    val latestMoviesDeferred = async { repository.getMoviesBySort("createdAt") }
                    val latestSeriesDeferred = async { repository.getSeriesBySort("createdAt").map { it.toMovieItem() } }
                    val actionDeferred = async { repository.getMoviesByGenre("action") }

                    val trendingMovies = trendingDeferred.await()
                    val popular = popularDeferred.await()
                    // Render the hero and first shelf before slower genre and
                    // series endpoints finish. Remaining rails stream in later.
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        heroMovie = trendingMovies.firstOrNull() ?: popular.firstOrNull(),
                        trending = shelfItems(trendingMovies),
                        popularMovies = shelfItems(popular)
                    )
                    val series = seriesDeferred.await()
                    // Mix existing trending movie and popular-series API data for
                    // the website's mixed film/series discovery experience.
                    val seriesCards = series.map { it.toMovieItem() }
                    val trending = buildList {
                        repeat(maxOf(trendingMovies.size, seriesCards.size)) { index ->
                            trendingMovies.getOrNull(index)?.let { add(it) }
                            seriesCards.getOrNull(index)?.let { add(it) }
                        }
                    }.distinctBy { "${it.contentType}:${it.id}" }.take(20)
                    val countryTrending = trending.filter { item ->
                        item.country.equals("ID", true) ||
                            item.country.orEmpty().contains("indonesia", true)
                    }
                    val networkPicks = (trending + popular + seriesCards)
                        .distinctBy { "${it.contentType}:${it.id}" }
                        .filter { item ->
                            val metadata = item.networkMetadata
                            listOf("netflix", "hbo", "disney", "prime", "apple")
                                .any(metadata::contains)
                        }.take(20)
                    val latestMovies = latestMoviesDeferred.await()
                    val latestSeries = latestSeriesDeferred.await()
                    val action = actionDeferred.await()
                    val unfinished = historyStore.getUnfinishedItems()

                    val hero = trending.firstOrNull() ?: popular.firstOrNull()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        heroMovie = hero,
                        continueWatching = unfinished,
                        trending = shelfItems(trending),
                        trendingCountry = shelfItems(countryTrending),
                        networkPicks = shelfItems(networkPicks),
                        popularMovies = shelfItems(popular),
                        popularSeries = series,
                        latestMovies = shelfItems(latestMovies),
                        latestSeries = shelfItems(latestSeries),
                        actionMovies = shelfItems(action),
                    )
                }

                // Defer extra editorial shelves until the first screen is rendered.
                // Only two genre requests at a time to avoid a burst of heavy
                // poster/network work on mobile and low-powered TV devices.
                delay(650)
                val editorial = listOf(
                    Triple("Animasi Pilihan", "movie", "animation"),
                    Triple("Drama Pilihan", "series", "drama"),
                    Triple("Romansa", "movie", "romance"),
                    Triple("Misteri & Kriminal", "series", "crime")
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
                    error = e.localizedMessage ?: "Terjadi kesalahan saat memuat data"
                )
            }
        }
    }

    fun refreshContinueWatching() {
        val unfinished = historyStore.getUnfinishedItems()
        _uiState.value = _uiState.value.copy(continueWatching = unfinished)
    }

    fun removeFromHistory(contentId: String) {
        historyStore.removeItem(contentId)
        refreshContinueWatching()
    }

    fun setHeroMovie(movie: MovieItem) {
        _uiState.value = _uiState.value.copy(heroMovie = movie)
    }
}
