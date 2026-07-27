package com.tourverse.repositories

import com.tourverse.models.Destination

interface DestinationRepository {
    fun getAll(): List<Destination>
    fun getById(id: Int): Destination?
}
