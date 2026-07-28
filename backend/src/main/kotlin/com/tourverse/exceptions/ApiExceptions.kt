package com.tourverse.exceptions

open class ApiException(message: String) : RuntimeException(message)
class UnauthorizedException(message: String = "Authentication required") : ApiException(message)
class ForbiddenException(message: String = "You do not have permission to perform this action") : ApiException(message)
class NotFoundException(message: String) : ApiException(message)
class ConflictException(message: String) : ApiException(message)
