package com.tourverse.repositories

import com.tourverse.models.Destination

class InMemoryDestinationRepository : DestinationRepository {

    private val destinations = listOf(
        Destination(
            id = 1,
            name = "Murchison Falls National Park",
            description = "A major national park known for wildlife, boat cruises and the River Nile.",
            location = "Northwestern Uganda",
            category = "Wildlife",
            imageUrl = "https://images.unsplash.com/photo-1516426122078-c23e76319801",
            latitude = 2.2758,
            longitude = 31.6841,
            rating = 4.8
        ),
        Destination(
            id = 2,
            name = "Source of the Nile",
            description = "A popular destination for sightseeing and adventure activities in Jinja.",
            location = "Jinja, Uganda",
            category = "Adventure",
            imageUrl = "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee",
            latitude = 0.4232,
            longitude = 33.2041,
            rating = 4.6
        ),
        Destination(
            id = 3,
            name = "Bwindi Impenetrable National Park",
            description = "A UNESCO-listed forest famous for mountain gorilla trekking.",
            location = "Southwestern Uganda",
            category = "Nature",
            imageUrl = "https://images.unsplash.com/photo-1540573133985-87b6da6d54a9",
            latitude = -1.0521,
            longitude = 29.6219,
            rating = 4.9
        )
    )

    override fun getAll(): List<Destination> = destinations

    override fun getById(id: Int): Destination? =
        destinations.find { it.id == id }
}
