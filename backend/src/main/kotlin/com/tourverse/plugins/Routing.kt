package com.tourverse.plugins

import com.tourverse.repositories.PostgresDestinationRepository
import com.tourverse.repositories.PostgresCategoryRepository
import com.tourverse.repositories.PostgresDestinationImportRepository
import com.tourverse.routes.authRoutes
import com.tourverse.routes.destinationRoutes
import com.tourverse.routes.documentationRoutes
import com.tourverse.routes.healthRoutes
import com.tourverse.routes.categoryRoutes
import com.tourverse.routes.userProfileRoutes
import com.tourverse.routes.communityRoutes
import com.tourverse.routes.platformRoutes
import com.tourverse.routes.destinationImportRoutes
import com.tourverse.routes.destinationCatalogueRoutes
import com.tourverse.services.AuthService
import com.tourverse.services.DestinationService
import com.tourverse.services.CategoryService
import com.tourverse.services.UserProfileService
import com.tourverse.services.CommunityService
import com.tourverse.services.PlatformService
import com.tourverse.services.DestinationImportService
import com.tourverse.services.WikidataDestinationImportProvider
import com.tourverse.services.OpenTripMapDestinationImportProvider
import com.tourverse.services.DestinationCatalogueService
import com.tourverse.services.GooglePlaceLinkService
import com.tourverse.services.GooglePlacesHttpClient
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

// Registers every API route and injects the service instances they depend on.
fun Application.configureRouting() {
    // Build the service layer once so each route can reuse the same repositories.
    val destinationService = DestinationService(PostgresDestinationRepository())
    val authService = AuthService()
    val profileService = UserProfileService()
    val categoryService = CategoryService(PostgresCategoryRepository())
    val communityService = CommunityService()
    val platformService = PlatformService()
    val importRepository = PostgresDestinationImportRepository()
    val destinationRepository = PostgresDestinationRepository()
    val importService = DestinationImportService(
        importRepository,
        listOf(WikidataDestinationImportProvider(), OpenTripMapDestinationImportProvider())
    )
    val catalogueService = DestinationCatalogueService(importRepository, importService)
    val googlePlaceLinkService = GooglePlaceLinkService(
        destinationRepository,
        importRepository,
        GooglePlacesHttpClient()
    )

    routing {
        documentationRoutes()
        healthRoutes()
        authRoutes(authService)
        destinationRoutes(destinationService)
        userProfileRoutes(profileService)
        categoryRoutes(categoryService)
        communityRoutes(communityService)
        platformRoutes(platformService)
        destinationImportRoutes(importService)
        destinationCatalogueRoutes(catalogueService, googlePlaceLinkService)
    }
}
