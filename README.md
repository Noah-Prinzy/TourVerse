# TourVerse

A starter full-stack tourism platform with:

- Android: Kotlin + Jetpack Compose
- Web: TypeScript + React
- Backend: Kotlin + Ktor
- Database target: PostgreSQL
- Main communication style: REST API with JSON

## Project structure

```text
TourVerse/
├── backend/
├── androidApp/
├── webApp/
├── docs/
├── .gitignore
└── README.md
```

## Architecture

```text
┌──────────────────────────┐
│ Android Application      │
│ Kotlin + Jetpack Compose │
└─────────────┬────────────┘
              │
              │ HTTP / JSON
              │
┌─────────────▼────────────┐
│ Kotlin Ktor REST API     │
│ Routes → Services → Repo │
└─────────────┬────────────┘
              │
              │ SQL
              │
┌─────────────▼────────────┐
│ PostgreSQL Database      │
└─────────────▲────────────┘
              │
              │ HTTP / JSON
              │
┌─────────────┴────────────┐
│ Web Application          │
│ TypeScript + React       │
└──────────────────────────┘
```

Both frontend applications use the same backend and the same endpoint contracts.

## Initial API endpoint

```http
GET /api/destinations
GET /api/destinations/{id}
GET /api/health
```

## Default development URLs

- Backend: `http://localhost:8080`
- Web app: `http://localhost:5173`
- Android emulator backend URL: `http://10.0.2.2:8080`

## Next recommended features

1. PostgreSQL database integration
2. User registration and JWT login
3. Destination administration
4. Favourites
5. Reviews and ratings
6. Trip itineraries
7. Maps and location
8. Image upload
9. Weather API integration
