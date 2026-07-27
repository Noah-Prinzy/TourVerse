# Visual Architecture

```text
                         TOURISM PLATFORM
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                            │
│                                                                 │
│  ┌───────────────────────┐       ┌───────────────────────────┐  │
│  │ Android Application   │       │ Web Application           │  │
│  │ Kotlin                │       │ TypeScript                │  │
│  │ Jetpack Compose       │       │ React                     │  │
│  │ ViewModel             │       │ Components and Services   │  │
│  │ Repository            │       │ Responsive UI             │  │
│  └───────────┬───────────┘       └──────────────┬────────────┘  │
└──────────────┼──────────────────────────────────┼───────────────┘
               │                                  │
               └──────────── HTTP/JSON ───────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────────┐
│                       BACKEND LAYER                             │
│                      Kotlin + Ktor                              │
│                                                                 │
│  Routing → Controllers → Services → Repositories → Database     │
│                                                                 │
│  Modules:                                                       │
│  Authentication | Destinations | Categories | Favourites        │
│  Reviews | Trips | Profiles | External Integrations             │
└─────────────────────────────────┬───────────────────────────────┘
                                  │
                         SQL / Database Driver
                                  │
┌─────────────────────────────────▼───────────────────────────────┐
│                         DATA LAYER                              │
│                       PostgreSQL                                │
│                                                                 │
│ Users | Destinations | Categories | Favourites | Reviews | Trips│
└─────────────────────────────────────────────────────────────────┘
```

## Request flow

```text
User Action
   ↓
Android Screen or React Page
   ↓
Frontend API Service
   ↓
Ktor Route
   ↓
Service
   ↓
Repository
   ↓
PostgreSQL
   ↓
JSON Response
   ↓
Updated User Interface
```
