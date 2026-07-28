# TourVerse REST API Contract

This contract is derived from the current Kotlin routes and serializable models.
The running OpenAPI resource is available at `/api/openapi.yaml`, but the Kotlin
implementation remains authoritative where the two differ.

## Conventions

Default base URL:

```text
http://localhost:8080
```

Requests and responses use JSON. Protected routes require:

```http
Authorization: Bearer <access-token>
Content-Type: application/json
```

IDs are UUID strings. Dates are ISO `YYYY-MM-DD`; instants are ISO-8601 values.

Successful mutations return the affected response model unless the endpoint
documents an `ApiMessage`:

```json
{
  "status": "success",
  "message": "Operation completed."
}
```

Errors use the same shape with `status: "error"`. Expected statuses include:

| Status | Meaning |
| --- | --- |
| 400 | Invalid path, query, JSON, or validation |
| 401 | Missing, malformed, invalid, or expired bearer token |
| 403 | Authenticated but not authorized |
| 404 | Resource not found or not visible to the caller |
| 409 | Duplicate email, review, favorite, or trip destination |
| 429 | Per-client rate limit exceeded |
| 500 | Unexpected server failure with a safe public message |

## Endpoint matrix

### System

| Method | Path | Access | Response |
| --- | --- | --- | --- |
| GET | `/api/health` | Public | `ApiMessage("ok", ...)` |
| GET | `/api/docs` | Public | HTML |
| GET | `/api/openapi.yaml` | Public | YAML |

### Authentication and current user

| Method | Path | Access | Request |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | `RegisterRequest` |
| POST | `/api/auth/login` | Public | `LoginRequest` |
| POST | `/api/auth/refresh` | Public | `RefreshTokenRequest` |
| POST | `/api/auth/logout` | Public | `LogoutRequest` |
| POST | `/api/auth/logout-all` | Authenticated | None |
| GET | `/api/users/me` | Authenticated | None |
| PUT | `/api/users/me` | Authenticated | `UpdateProfileRequest` |
| GET | `/api/users/me/profile` | Authenticated | None |
| PUT | `/api/users/me/profile` | Authenticated | `UpdateUserProfileRequest` |
| PUT | `/api/users/me/profile/image` | Authenticated | `UpdateProfileImageRequest` |
| DELETE | `/api/users/me` | Authenticated | `DeleteAccountRequest` |

```json
{
  "firstName": "Noah",
  "lastName": "Prince",
  "email": "noah@example.com",
  "password": "A-strong-password"
}
```

Registration and login return:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": {
    "id": "00000000-0000-0000-0000-000000000000",
    "firstName": "Noah",
    "lastName": "Prince",
    "email": "noah@example.com",
    "profileImageUrl": null,
    "bio": null,
    "role": "USER",
    "createdAt": "2026-07-28T00:00:00Z"
  }
}
```

Refresh and logout bodies contain `refreshToken`. Basic profile updates accept
nullable `firstName`, `lastName`, `bio`, and `profileImageUrl`. Extended profile
updates accept nullable `firstName`, `lastName`, `bio`, `nationality`,
`travelInterests`, and `profilePublic`.

### Destinations

| Method | Path | Current access | Request/response |
| --- | --- | --- | --- |
| GET | `/api/destinations` | Public | `PagedDestinationResponse` |
| GET | `/api/destinations/{id}` | Public | `Destination` |
| POST | `/api/destinations` | Public | `CreateDestinationRequest` -> 201 |
| PUT | `/api/destinations/{id}` | Public | Full `UpdateDestinationRequest` |
| DELETE | `/api/destinations/{id}` | Public | `ApiMessage` |

Destination writes are public in the current route implementation. This should
be reviewed before production.

List query parameters:

| Name | Rule |
| --- | --- |
| `search` | Case-insensitive match across name, country, city, description, category |
| `country`, `city`, `category` | Case-insensitive exact filters |
| `page` | Integer, minimum 1, default 1 |
| `size` | Integer 1–100, default 20 |
| `sortBy` | `name`, `country`, `city`, `category`, `createdAt`, `updatedAt` |
| `sortDirection` | `asc` or `desc`, default `desc` |

Create and update use the same full body:

```json
{
  "name": "Maasai Mara National Reserve",
  "country": "Kenya",
  "city": "Narok",
  "description": "A wildlife reserve in southwestern Kenya.",
  "category": "Wildlife",
  "latitude": -1.4061,
  "longitude": 35.01,
  "coverImageUrl": "https://example.com/mara.jpg"
}
```

`city`, both coordinates, and `coverImageUrl` are optional. Coordinates must be
supplied together. A list response is:

```json
{
  "items": [
    {
      "id": "00000000-0000-0000-0000-000000000000",
      "name": "Maasai Mara National Reserve",
      "country": "Kenya",
      "city": "Narok",
      "description": "A wildlife reserve in southwestern Kenya.",
      "category": "Wildlife",
      "latitude": -1.4061,
      "longitude": 35.01,
      "coverImageUrl": "https://example.com/mara.jpg",
      "createdAt": "2026-07-28T00:00:00Z",
      "updatedAt": "2026-07-28T00:00:00Z"
    }
  ],
  "page": 1,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

### Categories

| Method | Path | Access | Request |
| --- | --- | --- | --- |
| GET | `/api/categories` | Public; active only | None |
| GET | `/api/categories/{id}` | Public | None |
| POST | `/api/categories` | Admin | `CreateCategoryRequest` |
| PUT | `/api/categories/{id}` | Admin | `UpdateCategoryRequest` |
| DELETE | `/api/categories/{id}` | Admin | None |
| GET | `/api/admin/categories` | Admin; includes inactive | None |

Create:

```json
{
  "name": "Food & Drink",
  "description": "Local food and culinary experiences",
  "iconUrl": "https://example.com/food.png",
  "active": true
}
```

Only `name` is required. Update uses the same nullable fields and requires at
least one supplied field. The backend derives a unique slug such as
`food-and-drink`.

### Reviews and ratings

| Method | Path | Access | Request |
| --- | --- | --- | --- |
| GET | `/api/destinations/{destinationId}/reviews` | Public | None |
| POST | `/api/destinations/{destinationId}/reviews` | Authenticated | `CreateReviewRequest` |
| PUT | `/api/reviews/{id}` | Owner or admin | `UpdateReviewRequest` |
| DELETE | `/api/reviews/{id}` | Owner or admin | None |

```json
{
  "rating": 5,
  "comment": "Excellent destination."
}
```

Create requires `rating`; update makes `rating` and `comment` nullable partial
fields. Ratings are 1–5, comments are at most 2,000 characters, and a user may
review a destination once. The GET response contains `averageRating`,
`reviewCount`, and `reviews`.

### Favorites

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/favorites` | Authenticated |
| POST | `/api/favorites/{destinationId}` | Authenticated |
| DELETE | `/api/favorites/{destinationId}` | Authenticated |

Favorites are unique per user and destination. List/create responses embed the
destination and include favorite ID and creation time.

### Trips and itineraries

| Method | Path | Access | Request |
| --- | --- | --- | --- |
| GET | `/api/trips` | Authenticated owner | None |
| GET | `/api/trips/{id}` | Authenticated owner | None |
| POST | `/api/trips` | Authenticated | `CreateTripRequest` |
| PUT | `/api/trips/{id}` | Authenticated owner | `UpdateTripRequest` |
| DELETE | `/api/trips/{id}` | Authenticated owner | None |
| POST | `/api/trips/{id}/destinations` | Authenticated owner | `AddTripDestinationRequest` |
| DELETE | `/api/trips/{id}/destinations/{destinationId}` | Authenticated owner | None |

Create:

```json
{
  "title": "Kenya wildlife trip",
  "description": "A week of wildlife destinations",
  "startDate": "2026-08-10",
  "endDate": "2026-08-17"
}
```

Update uses nullable partial fields. An itinerary entry uses `destinationId`,
optional `visitDate`, optional `notes`, and nonnegative `displayOrder`. A
destination can appear once in a trip.

### Tourism services

| Method | Path | Access | Request |
| --- | --- | --- | --- |
| GET | `/api/services` | Public | Filters: `type`, `destinationId` |
| GET | `/api/services/{id}` | Public | None |
| POST | `/api/services` | Admin, business owner, tour guide | `CreateTourismServiceRequest` |
| PUT | `/api/services/{id}` | Owner or admin | `UpdateTourismServiceRequest` |
| DELETE | `/api/services/{id}` | Owner or admin | None |

Create requires `name` and `serviceType`; `currency` defaults to `USD`.
Optional fields are `destinationId`, `description`, `phone`, `email`,
`websiteUrl`, `address`, and `priceFrom`. Update makes those fields nullable and
also accepts `active`.

Allowed service types are `HOTEL`, `RESTAURANT`, `TOUR`, `TRANSPORT`, `GUIDE`,
and `ACTIVITY`. Currency is a three-letter code and price cannot be negative.

### Bookings and notifications

| Method | Path | Access | Request |
| --- | --- | --- | --- |
| GET | `/api/bookings` | Authenticated; admins receive all | None |
| POST | `/api/bookings` | Authenticated | `CreateBookingRequest` |
| PUT | `/api/bookings/{id}/cancel` | Booking owner | None |
| GET | `/api/notifications` | Authenticated | None |
| PUT | `/api/notifications/{id}/read` | Authenticated owner | None |
| PUT | `/api/notifications/read-all` | Authenticated | None |

Create booking:

```json
{
  "serviceId": "00000000-0000-0000-0000-000000000000",
  "bookingDate": "2026-08-10",
  "numberOfPeople": 2,
  "notes": "Airport pickup requested"
}
```

Dates cannot be in the past; party size is 1–100. The backend calculates an
estimated total from the service's starting price. It stores payment status but
does not process a payment.

### Administration

| Method | Path | Access | Request |
| --- | --- | --- | --- |
| GET | `/api/admin/statistics` | Admin | None |
| GET | `/api/admin/users` | Admin | None |
| PUT | `/api/admin/users/{id}/role` | Admin | `{ "role": "..." }` |
| GET | `/api/admin/services` | Admin | None |
| GET | `/api/admin/bookings` | Admin | None |
| PUT | `/api/admin/bookings/{id}/status` | Admin | `{ "status": "..." }` |

Roles are `USER`, `ADMIN`, `TOUR_GUIDE`, and `BUSINESS_OWNER`. Booking statuses
are `PENDING`, `CONFIRMED`, `CANCELLED`, and `COMPLETED`.

## Client compatibility note

The Android and web clients now implement this destination contract using
string UUIDs, `PagedDestinationResponse`, nullable destination fields, backend
timestamps, pagination, search, country/city/category filters, sorting, and
standard `ApiMessage` error responses.
