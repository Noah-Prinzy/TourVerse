package com.tourverse.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.tourverse.BuildConfig
import com.tourverse.data.model.Destination
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class DestinationMapState {
    READY, MISSING_KEY, MISSING_COORDINATES, INVALID_COORDINATES
}

fun validMapCoordinates(latitude: Double?, longitude: Double?): Boolean =
    latitude != null && longitude != null &&
        latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0

fun destinationMapState(
    configured: Boolean,
    latitude: Double?,
    longitude: Double?
): DestinationMapState = when {
    latitude == null || longitude == null -> DestinationMapState.MISSING_COORDINATES
    !validMapCoordinates(latitude, longitude) -> DestinationMapState.INVALID_COORDINATES
    !configured -> DestinationMapState.MISSING_KEY
    else -> DestinationMapState.READY
}

fun googleMapsUris(destination: Destination): List<String> {
    if (!validMapCoordinates(destination.latitude, destination.longitude)) return emptyList()
    val query = "${destination.latitude},${destination.longitude} (${destination.name})"
    val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20")
    val placeId = destination.googlePlaceId?.trim()?.takeIf(String::isNotEmpty)
    val web = buildString {
        append("https://www.google.com/maps/search/?api=1&query=").append(encoded)
        if (placeId != null) append("&query_place_id=")
            .append(URLEncoder.encode(placeId, StandardCharsets.UTF_8))
    }
    return listOf("geo:${destination.latitude},${destination.longitude}?q=$encoded", web)
}

fun openDestinationInGoogleMaps(context: Context, destination: Destination): Boolean {
    for (uri in googleMapsUris(destination)) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
            return true
        } catch (_: ActivityNotFoundException) {
            // Try the browser-safe HTTPS URL after the geo URI.
        }
    }
    return false
}

@Composable
fun DestinationMap(destination: Destination, openExternal: () -> Unit) {
    val state = destinationMapState(
        BuildConfig.GOOGLE_MAPS_CONFIGURED,
        destination.latitude,
        destination.longitude
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Map", style = MaterialTheme.typography.headlineSmall)
        if (state == DestinationMapState.READY) {
            val position = LatLng(destination.latitude!!, destination.longitude!!)
            val camera = rememberCameraPositionState {
                this.position = CameraPosition.fromLatLngZoom(position, 13f)
            }
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                cameraPositionState = camera
            ) {
                Marker(
                    state = rememberUpdatedMarkerState(position = position),
                    title = destination.name
                )
            }
        } else {
            val message = when (state) {
                DestinationMapState.MISSING_KEY ->
                    "Interactive map unavailable: no Android Maps key is configured."
                DestinationMapState.MISSING_COORDINATES ->
                    "Map unavailable: this destination has no coordinates."
                DestinationMapState.INVALID_COORDINATES ->
                    "Map unavailable: destination coordinates are invalid."
                DestinationMapState.READY -> ""
            }
            Card(Modifier.fillMaxWidth()) {
                Text(message, Modifier.padding(16.dp))
            }
        }
        if (googleMapsUris(destination).isNotEmpty()) {
            TextButton(onClick = openExternal) { Text("Open in Google Maps") }
        }
    }
}
