package com.tourverse.plugins

import com.tourverse.repositories.PostgresDestinationRepository
import com.tourverse.repositories.PostgresCategoryRepository
import com.tourverse.routes.authRoutes
import com.tourverse.routes.destinationRoutes
import com.tourverse.routes.documentationRoutes
import com.tourverse.routes.healthRoutes
import com.tourverse.routes.categoryRoutes
import com.tourverse.routes.userProfileRoutes
import com.tourverse.routes.communityRoutes
import com.tourverse.routes.platformRoutes
import com.tourverse.services.AuthService
import com.tourverse.services.DestinationService
import com.tourverse.services.CategoryService
import com.tourverse.services.UserProfileService
import com.tourverse.services.CommunityService
import com.tourverse.services.PlatformService
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    val destinationService = DestinationService(PostgresDestinationRepository())
    val authService = AuthService()
    val profileService = UserProfileService()
    val categoryService = CategoryService(PostgresCategoryRepository())
    val communityService = CommunityService()
    val platformService = PlatformService()

    routing {
        documentationRoutes()
        healthRoutes()
        authRoutes(authService)
        destinationRoutes(destinationService)
        userProfileRoutes(profileService)
        categoryRoutes(categoryService)
        communityRoutes(communityService)
        platformRoutes(platformService)
    }
}
