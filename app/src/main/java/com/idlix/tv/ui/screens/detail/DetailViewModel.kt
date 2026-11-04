package com.idlix.tv.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idlix.tv.data.model.EpisodeItem
import com.idlix.tv.data.model.MovieDetail
import com.idlix.tv.data.model.Season
import com.idlix.tv.data.model.SeriesDetail
import com.idlix.tv.data.repository.IdlixRepository
import com.idlix.tv.data.repository.TitleLogoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val movie: MovieDetail? = null,
    val series: SeriesDetail? = null,
    val logoUrl: String? = null,
    val selectedSeason: Season? = null,
    val episodes: List<EpisodeItem> = emptyList(),
    val isSeasonLoading: Boolean = false,
    val seasonError: String? = null,
    val error: String? = null
)

class DetailViewModel(
    private val repository: IdlixRepository = IdlixRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private var detailJob: Job? = null
    private var seasonJob: Job? = null
    private var logoJob: Job? = null

    private fun resolveTitleLogo(
        contentType: String, slug: String, direct: String?, imdbId: String?
    ) {
        logoJob?.cancel()
        logoJob = viewModelScope.launch {
            try {
                val result = TitleLogoRepository.resolve(contentType, slug, direct, imdbId)
                if (isActive && !result.isNullOrBlank()) {
                    val current = _uiState.value
                    if (current.movie?.slug == slug || current.series?.slug == slug) {
                        _uiState.value = current.copy(logoUrl = result)
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // Title text remains readable when artwork is unavailable.
            }
        }
    }

    fun loadDetail(contentType: String, slug: String) {
        detailJob?.cancel()
        seasonJob?.cancel()
        logoJob?.cancel()
        _uiState.value = DetailUiState(isLoading = true)

        detailJob = viewModelScope.launch {
            try {
                if (contentType == "tv_series" || contentType == "series") {
                    val series = repository.getSeriesDetail(slug)
                    if (!isActive) return@launch
                    if (series == null) {
                        _uiState.value = DetailUiState(isLoading = false, error = "Detail serial tidak tersedia. Coba kembali dan buka lagi.")
                        return@launch
                    }

                    // A default season may differ from the first season in the server's list.
                    val initialSeason = series.seasons.firstOrNull {
                        it.seasonNumber == series.defaultSeason?.seasonNumber
                    } ?: series.seasons.firstOrNull()
                    val episodes = when {
                        initialSeason == null -> series.defaultSeason?.episodes.orEmpty()
                        initialSeason.seasonNumber == series.defaultSeason?.seasonNumber ->
                            series.defaultSeason?.episodes.orEmpty()
                        else -> repository.getEpisodeDetail(series.slug, initialSeason.seasonNumber, 1)
                            ?.season?.episodes.orEmpty()
                    }
                    if (!isActive) return@launch

                    _uiState.value = DetailUiState(
                        isLoading = false,
                        series = series,
                        logoUrl = series.titleLogoUrl,
                        selectedSeason = initialSeason,
                        episodes = episodes
                    )
                    resolveTitleLogo("series", series.slug, series.titleLogoUrl, series.imdbId)
                } else {
                    val movie = repository.getMovieDetail(slug)
                    if (!isActive) return@launch
                    _uiState.value = if (movie != null) {
                        DetailUiState(isLoading = false, movie = movie, logoUrl = movie.titleLogoUrl)
                    } else {
                        DetailUiState(isLoading = false, error = "Detail film tidak tersedia. Coba kembali dan buka lagi.")
                    }
                    if (movie != null) resolveTitleLogo("movie", movie.slug, movie.titleLogoUrl, movie.imdbId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isActive) {
                    _uiState.value = DetailUiState(
                        isLoading = false,
                        error = e.localizedMessage ?: "Gagal memuat detail"
                    )
                }
            }
        }
    }

    fun selectSeason(seriesSlug: String, season: Season) {
        seasonJob?.cancel()

        val series = _uiState.value.series ?: return
        val cachedEpisodes = series.defaultSeason?.takeIf {
            it.seasonNumber == season.seasonNumber && it.episodes.isNotEmpty()
        }?.episodes
        if (cachedEpisodes != null) {
            _uiState.value = _uiState.value.copy(
                selectedSeason = season,
                episodes = cachedEpisodes,
                isSeasonLoading = false,
                seasonError = null
            )
            return
        }

        // Clear the previous season immediately to avoid playing an incorrect episode.
        _uiState.value = _uiState.value.copy(
            selectedSeason = season,
            episodes = emptyList(),
            isSeasonLoading = true,
            seasonError = null
        )
        seasonJob = viewModelScope.launch {
            try {
                val episodeResponse = repository.getEpisodeDetail(seriesSlug, season.seasonNumber, 1)
                if (!isActive) return@launch
                val episodes = episodeResponse?.season?.episodes.orEmpty()
                _uiState.value = _uiState.value.copy(
                    episodes = episodes,
                    isSeasonLoading = false,
                    seasonError = if (episodes.isEmpty()) "Episode season ini belum tersedia." else null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isActive) {
                    _uiState.value = _uiState.value.copy(
                        episodes = emptyList(),
                        isSeasonLoading = false,
                        seasonError = "Gagal memuat episode. Pilih season lagi untuk mencoba."
                    )
                }
            }
        }
    }
}
