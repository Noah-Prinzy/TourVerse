package com.tourverse.services

import com.tourverse.models.Destination
import com.tourverse.repositories.DestinationRepository

class DestinationService(
    private val repository: DestinationRepository
) {
    fun getAllDestinations(): List<Destination> = repository.getAll()

    fun getDestinationById(id: Int): Destination? = repository.getById(id)
}
