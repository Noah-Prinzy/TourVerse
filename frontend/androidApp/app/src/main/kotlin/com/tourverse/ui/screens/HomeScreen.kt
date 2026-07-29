package com.tourverse.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.tourverse.data.model.Destination

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onDestinationClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        when {
            state.isLoading && state.destinations.isEmpty() -> LoadingContent(
                modifier = Modifier.padding(innerPadding)
            )

            state.errorMessage != null -> ErrorContent(
                message = state.errorMessage.orEmpty(),
                onRetry = viewModel::retry,
                modifier = Modifier.padding(innerPadding)
            )

            else -> DestinationList(
                state = state,
                onSearchChange = viewModel::updateSearch,
                onCountryClick = viewModel::cycleCountry,
                onCountryRetry = viewModel::loadCountries,
                onCityChange = viewModel::updateCity,
                onCategoryClick = viewModel::cycleCategory,
                onSortFieldClick = viewModel::cycleSortField,
                onSortDirectionClick = viewModel::toggleSortDirection,
                onPageSizeClick = viewModel::cyclePageSize,
                onPrevious = viewModel::previousPage,
                onNext = viewModel::nextPage,
                onDestinationClick = onDestinationClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun DestinationList(
    state: HomeUiState,
    onSearchChange: (String) -> Unit,
    onCountryClick: () -> Unit,
    onCountryRetry: () -> Unit,
    onCityChange: (String) -> Unit,
    onCategoryClick: () -> Unit,
    onSortFieldClick: () -> Unit,
    onSortDirectionClick: () -> Unit,
    onPageSizeClick: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDestinationClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TourVerse",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Discover unforgettable destinations.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = state.search,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search") },
                    singleLine = true
                )
                TextButton(onClick = onCountryClick, enabled = !state.countriesLoading) {
                    val selected = state.countries.find { it.code == state.country }
                    Text("Country: ${selected?.let { "${it.name} (${it.destinationCount})" } ?: "All countries"}")
                }
                state.countriesError?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onCountryRetry) { Text("Retry") }
                    }
                }
                OutlinedTextField(
                    value = state.city,
                    onValueChange = onCityChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("City") },
                    singleLine = true
                )
                TextButton(onClick = onCategoryClick) {
                    Text("Category: ${state.category.ifBlank { "Any" }}")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(onClick = onSortFieldClick) {
                        Text("Sort: ${state.sortBy.label}")
                    }
                    TextButton(onClick = onSortDirectionClick) {
                        Text(state.sortDirection.label)
                    }
                    TextButton(onClick = onPageSizeClick) {
                        Text("${state.pageSize} per page")
                    }
                }
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        if (state.isEmpty) {
            item {
                Text(
                    text = "No destinations match the current search and filters.",
                    modifier = Modifier.padding(vertical = 32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.destinations, key = { it.id }) { destination ->
                DestinationCard(destination, onDestinationClick)
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onPrevious, enabled = state.canGoPrevious) {
                    Text("Previous")
                }
                Text(
                    text = if (state.totalPages == 0) {
                        "Page 1 of 1"
                    } else {
                        "Page ${state.currentPage} of ${state.totalPages} · ${state.totalItems} total"
                    }
                )
                Button(onClick = onNext, enabled = state.canGoNext) {
                    Text("Next")
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(destination: Destination, onDestinationClick: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onDestinationClick(destination.id) }
    ) {
        Column {
            val imageUrl = destination.coverImageUrl?.trim()?.takeIf(String::isNotEmpty)
            if (imageUrl == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No image available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = destination.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = destination.category,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = destination.displayLocation,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = destination.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message)
        Spacer(modifier = Modifier.height(14.dp))
        Button(onClick = onRetry) {
            Text("Try again")
        }
    }
}
