package com.tourverse.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import coil3.compose.rememberAsyncImagePainter

@Composable
fun DestinationDetailScreen(viewModel: DestinationDetailViewModel, authenticated: Boolean, userId: String?, onLogin: () -> Unit) {
    val state by viewModel.state.collectAsState()
    if (state.loading) { LoadingBox(); return }
    val destination = state.destination
    if (destination == null) { MessageScreen(state.error ?: "Destination not found.", viewModel::load); return }
    var comment by remember { mutableStateOf("") }; var rating by remember { mutableStateOf("5") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            if (destination.coverImageUrl == null) Box(Modifier.fillMaxWidth().height(220.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text("No image available") }
            else Image(rememberAsyncImagePainter(destination.coverImageUrl), destination.name, Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Crop)
            Text(destination.name, style = MaterialTheme.typography.headlineLarge); Text(destination.displayLocation); Text(destination.category); Text(destination.description)
            if (destination.latitude != null && destination.longitude != null) Text("Coordinates: ${destination.latitude}, ${destination.longitude}")
        }
        item {
            if (authenticated) Button(viewModel::toggleFavorite, enabled = !state.busy) { Text(if (state.favorite) "Remove favorite" else "Add favorite") }
            else Button(onLogin) { Text("Login to favorite or review") }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        item { Text("Reviews", style = MaterialTheme.typography.headlineSmall); Text("${state.reviews?.averageRating ?: 0.0} / 5 · ${state.reviews?.reviewCount ?: 0}") }
        if (authenticated) item {
            OutlinedTextField(rating, { rating = it }, label = { Text("Rating 1-5") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(comment, { comment = it }, label = { Text("Comment") }, modifier = Modifier.fillMaxWidth())
            Button({ viewModel.review(rating.toIntOrNull()?.coerceIn(1,5) ?: 5, comment) }, enabled = !state.busy) { Text("Save review") }
        }
        items(state.reviews?.reviews.orEmpty(), key = { it.id }) { review ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text("${review.rating} / 5"); Text(review.comment ?: "No comment"); if (review.userId == userId) TextButton({ viewModel.deleteReview(review.id) }) { Text("Delete") } } }
        }
    }
}

@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel, openDestination: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    if (state.loading) { LoadingBox(); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Favorites", style = MaterialTheme.typography.headlineLarge); state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (state.items.isEmpty()) item { Text("No saved destinations yet.") }
        items(state.items, key = { it.id }) { favorite ->
            Card(onClick = { openDestination(favorite.destination.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) { Text(favorite.destination.name, style = MaterialTheme.typography.titleLarge); Text(favorite.destination.displayLocation) }
            }
        }
    }
}

@Composable
fun TripsScreen(viewModel: TripsViewModel, openTrip: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    var title by remember { mutableStateOf("") }
    if (state.loading) { LoadingBox(); return }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("My trips", style = MaterialTheme.typography.headlineLarge); state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(title, { title = it }, label = { Text("New trip title") }, modifier = Modifier.fillMaxWidth())
            Button({ viewModel.create(title, null, null, null) { openTrip(it.id) } }, enabled = title.isNotBlank() && !state.busy) { Text("Create trip") }
        }
        if (state.trips.isEmpty()) item { Text("No trips yet.") }
        items(state.trips, key = { it.id }) { trip -> Card(onClick = { openTrip(trip.id) }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(trip.title, style = MaterialTheme.typography.titleLarge); Text("${trip.destinations.size} destinations") } } }
    }
}

@Composable
fun TripDetailScreen(viewModel: TripsViewModel, onDeleted: () -> Unit, openDestination: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    if (state.loading) { LoadingBox(); return }
    val trip = state.selected
    if (trip == null) { MessageScreen(state.error ?: "Trip not found.", viewModel::load); return }
    var title by remember(trip.id) { mutableStateOf(trip.title) }; var description by remember(trip.id) { mutableStateOf(trip.description.orEmpty()) }
    var startDate by remember(trip.id) { mutableStateOf(trip.startDate.orEmpty()) }; var endDate by remember(trip.id) { mutableStateOf(trip.endDate.orEmpty()) }
    var destinationId by remember { mutableStateOf("") }; var visitDate by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Trip details", style = MaterialTheme.typography.headlineLarge); state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(startDate, { startDate = it }, label = { Text("Start date YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(endDate, { endDate = it }, label = { Text("End date YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            Button({ viewModel.update(title, description.ifBlank { null }, startDate.ifBlank { null }, endDate.ifBlank { null }) }, enabled = !state.busy) { Text("Save") }
            OutlinedTextField(destinationId, { destinationId = it }, label = { Text("Destination UUID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(visitDate, { visitDate = it }, label = { Text("Visit date YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Button({ viewModel.addDestination(destinationId, visitDate.ifBlank { null }, notes.ifBlank { null }) }, enabled = destinationId.isNotBlank() && !state.busy) { Text("Add destination") }
        }
        items(trip.destinations, key = { it.id }) { entry -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { TextButton({ openDestination(entry.destination.id) }) { Text(entry.destination.name) }; TextButton({ viewModel.removeDestination(entry.destination.id) }) { Text("Remove") } } } }
        item { Button({ confirmDelete = true }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), enabled = !state.busy) { Text("Delete trip") } }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Delete trip?") },
        text = { Text("This action cannot be undone.") },
        confirmButton = { TextButton({ confirmDelete = false; viewModel.delete(onDeleted) }) { Text("Delete") } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } }
    )
}

@Composable private fun LoadingBox() = Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
@Composable private fun MessageScreen(message: String, retry: () -> Unit) = Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text(message); Button(retry) { Text("Try again") } }
