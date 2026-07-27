package com.tourverse.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tourverse.data.model.Destination
import com.tourverse.data.repository.DestinationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val destinations: List<Destination> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: DestinationRepository = DestinationRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDestinations()
    }

    fun loadDestinations() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)

            try {
                val destinations = repository.getDestinations()
                _uiState.value = HomeUiState(
                    isLoading = false,
                    destinations = destinations
                )
            } catch (exception: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    errorMessage = exception.message ?: "Could not load destinations."
                )
            }
        }
    }
}
