package com.tourverse.exceptions

// Groups the api exception logic and dependencies for this feature.
open class ApiException(message: String) : RuntimeException(message)
// Groups the unauthorized exception logic and dependencies for this feature.
class UnauthorizedException(message: String = "Authentication required") : ApiException(message)
// Groups the forbidden exception logic and dependencies for this feature.
class ForbiddenException(message: String = "You do not have permission to perform this action") : ApiException(message)
// Groups the not found exception logic and dependencies for this feature.
class NotFoundException(message: String) : ApiException(message)
// Groups the conflict exception logic and dependencies for this feature.
class ConflictException(message: String) : ApiException(message)
// Groups the provider not configured exception logic and dependencies for this feature.
class ProviderNotConfiguredException(message: String) : ApiException(message)
