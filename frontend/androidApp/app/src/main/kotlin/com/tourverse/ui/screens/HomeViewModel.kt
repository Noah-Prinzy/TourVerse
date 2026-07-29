package com.tourverse.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourverse.data.model.Destination
import com.tourverse.data.model.DestinationQuery
import com.tourverse.data.model.DestinationSortField
import com.tourverse.data.model.DestinationCountry
import com.tourverse.data.model.SortDirection
import com.tourverse.data.repository.DestinationRepository
import com.tourverse.data.remote.TourismApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class HomeUiState(
    val destinations: List<Destination> = emptyList(),
    val currentPage: Int = 1,
    val pageSize: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
    val search: String = "",
    val country: String = "",
    val city: String = "",
    val category: String = "",
    val sortBy: DestinationSortField = DestinationSortField.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<String> = emptyList(),
    val countries: List<DestinationCountry> = emptyList(),
    val countriesLoading: Boolean = true,
    val countriesError: String? = null
) {
    val canGoPrevious: Boolean get() = !isLoading && currentPage > 1
    val canGoNext: Boolean get() = !isLoading && currentPage < totalPages

    fun query(): DestinationQuery = DestinationQuery(
        search = search,
        countryCode = country,
        city = city,
        category = category,
        page = currentPage.coerceAtLeast(1),
        size = pageSize.coerceIn(1, 100),
        sortBy = sortBy,
        sortDirection = sortDirection
    )
}

class HomeViewModel(
    private val repository: DestinationRepository = DestinationRepository(),
    private val tourismApi: TourismApi = TourismApi()
) : ViewModel() {

    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(HomeUiState())
    val uiState: kotlinx.coroutines.flow.StateFlow<HomeUiState> = _uiState

    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var requestVersion = 0L

    init {
        loadDestinations()
        viewModelScope.launch {
            runCatching { tourismApi.getCategories().map { it.name } }
                .onSuccess { values -> _uiState.value = _uiState.value.copy(categories = values) }
        }
        loadCountries()
    }

    fun retry() = loadDestinations()

    fun updateSearch(value: String) {
        _uiState.value = _uiState.value.copy(search = value, currentPage = 1)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadDestinations()
        }
    }

    fun updateCountry(value: String) {
        _uiState.value = _uiState.value.copy(country = value, currentPage = 1)
        loadDestinations()
    }

    fun cycleCountry() {
        val choices = listOf("") + _uiState.value.countries.map { it.code }
        val current = choices.indexOf(_uiState.value.country).coerceAtLeast(0)
        updateCountry(choices[(current + 1) % choices.size])
    }

    fun loadCountries() {
        _uiState.value = _uiState.value.copy(countriesLoading = true, countriesError = null)
        viewModelScope.launch {
            runCatching { repository.getCountries().countries }
                .onSuccess { values ->
                    _uiState.value = _uiState.value.copy(countries = values, countriesLoading = false)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        countries = emptyList(), countriesLoading = false,
                        countriesError = "Country filters are temporarily unavailable."
                    )
                }
        }
    }

    fun updateCity(value: String) {
        _uiState.value = _uiState.value.copy(city = value, currentPage = 1)
        loadDestinations()
    }

    fun updateCategory(value: String) {
        _uiState.value = _uiState.value.copy(category = value, currentPage = 1)
        loadDestinations()
    }

    fun cycleCategory() {
        val choices = listOf("") + _uiState.value.categories
        val current = choices.indexOf(_uiState.value.category).coerceAtLeast(0)
        updateCategory(choices[(current + 1) % choices.size])
    }

    fun cycleSortField() {
        _uiState.value = _uiState.value.copy(
            sortBy = _uiState.value.sortBy.next(),
            currentPage = 1
        )
        loadDestinations()
    }

    fun toggleSortDirection() {
        _uiState.value = _uiState.value.copy(
            sortDirection = _uiState.value.sortDirection.toggled(),
            currentPage = 1
        )
        loadDestinations()
    }

    fun cyclePageSize() {
        val nextSize = when (_uiState.value.pageSize) {
            10 -> 20
            20 -> 50
            else -> 10
        }
        _uiState.value = _uiState.value.copy(pageSize = nextSize, currentPage = 1)
        loadDestinations()
    }

    fun previousPage() {
        if (!_uiState.value.canGoPrevious) return
        _uiState.value = _uiState.value.copy(currentPage = _uiState.value.currentPage - 1)
        loadDestinations()
    }

    fun nextPage() {
        if (!_uiState.value.canGoNext) return
        _uiState.value = _uiState.value.copy(currentPage = _uiState.value.currentPage + 1)
        loadDestinations()
    }

    fun loadDestinations() {
        searchJob?.cancel()
        val version = ++requestVersion
        val query = _uiState.value.query()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching { repository.getDestinations(query) }
                .onSuccess { response ->
                    if (version != requestVersion) return@onSuccess
                    _uiState.value = _uiState.value.copy(
                        destinations = response.items,
                        currentPage = response.page,
                        pageSize = response.size,
                        totalItems = response.totalItems,
                        totalPages = response.totalPages,
                        isLoading = false,
                        isEmpty = response.items.isEmpty(),
                        errorMessage = null
                    )
                }
                .onFailure { exception ->
                    if (version != requestVersion) return@onFailure
                    _uiState.value = _uiState.value.copy(
                        destinations = emptyList(),
                        totalItems = 0,
                        totalPages = 0,
                        isLoading = false,
                        isEmpty = false,
                        errorMessage = exception.message
                            ?: "Unable to load destinations. Please try again."
                    )
                }
        }
    }
}
