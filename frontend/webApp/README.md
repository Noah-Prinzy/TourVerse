# TourVerse Web Application

This is the authoritative guide to the React client currently implemented in
`frontend/webApp`.

## Current implementation

The web application is a routed responsive client. It currently:

- Renders a header, hero section, destination grid, and footer
- Consumes the backend's paginated UUID destination response
- Supports debounced search, country/city/category filters, sorting, page size,
  and previous/next pagination
- Shows loading, empty, backend-error, and network-error states
- Renders a reusable destination card for each result
- Cancels stale requests through `AbortController`
- Uses responsive three-, two-, and one-column layouts
- Allows the backend base URL to be configured with `VITE_API_BASE_URL`
- Provides registration, login, refresh rotation, logout, protected routes,
  profile editing, and account deletion
- Provides destination details, backend categories, favorites, review CRUD, and
  private trip CRUD with destination add/remove
- Uses React Router and a shared role-aware application shell

## Technology

| Area | Implementation |
| --- | --- |
| UI | React 19.1 |
| Language | TypeScript 5.8 |
| Build/dev server | Vite 6.3 |
| Styling | A single responsive CSS stylesheet |
| API access | Browser `fetch` |
| Tests | Vitest API and query tests |

There is no component framework, router, form library, server-state library, or
client-side persistence dependency.

## Source organization

```text
frontend/webApp/
|-- src/
|   |-- components/DestinationCard.tsx
|   |-- models/Destination.ts
|   |-- services/api.ts
|   |-- styles/index.css
|   |-- App.tsx
|   |-- main.tsx
|   `-- vite-env.d.ts
|-- index.html
|-- package.json
|-- tsconfig.app.json
|-- tsconfig.node.json
|-- vite.config.ts
`-- README.md
```

Data flow:

```text
App useEffect
    |
    v
getDestinations()
    |
    v
fetch(VITE_API_BASE_URL + /api/destinations)
    |
    +--> loading state
    +--> error state
    `--> DestinationCard grid
```

## Setup

Install dependencies:

```powershell
npm.cmd install
```

Start the development server:

```powershell
npm.cmd run dev
```

Vite listens on:

```text
http://localhost:5173
```

The API defaults to:

```text
http://localhost:8081
```

Override it with a local untracked environment file:

```dotenv
VITE_API_BASE_URL=http://localhost:8081
VITE_GOOGLE_MAPS_API_KEY=
```

Vite exposes `VITE_` variables to browser code. Never place passwords, private
API keys, JWT signing secrets, or other server secrets in these variables.
The Maps key is browser-visible by design; restrict it by HTTP referrer and to
the Maps JavaScript API. Never reuse the backend Places key.

## Build and preview

Run TypeScript project compilation and a production Vite build:

```powershell
npm.cmd run build
```

Run the focused API tests:

```powershell
npm.cmd run test
```

Preview the generated build:

```powershell
npm.cmd run preview
```

The project has no lint script. Its current automated coverage focuses on query
serialization, response parsing, backend errors, malformed errors, and network
errors.

## UI and responsive behavior

The page includes:

- A white header with TourVerse branding and anchor navigation
- A global-destination hero with an externally hosted background image
- A featured-destinations section with search/filter/sort controls
- Destination cards with image fallback, category, name, city/country,
  description, and action button
- Loading, empty, error/retry, result-count, and pagination states
- A dark footer

The grid uses three columns above 900 px, two columns between 681 and 900 px,
and one column at 680 px or below. Mobile styling hides the header navigation
and stacks the section header and search field.

The hero background depends on an external URL. Destinations without a cover
URL use a local styled fallback.

## Backend destination contract

The TypeScript models now match the backend's string UUID, nullable fields,
timestamps, paginated response, sort values, and standard error response:

```json
{
  "items": [],
  "page": 1,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

`getDestinations` accepts a typed query, safely constructs `URLSearchParams`,
supports cancellation, and preserves backend `ApiMessage` errors. Cards derive
location from nullable city and country and no longer invent a rating.

## Known limitations and next work

- No server-state cache beyond current React component state.
- No component/browser tests or lint command.
- Remote image failures do not yet switch to the local null-image fallback.
- Refresh-token persistence is centralized but JavaScript-readable because the
  backend currently returns tokens in JSON; a future HttpOnly-cookie design
  would reduce XSS exposure.
- Services, bookings, notifications, and full role-specific portals are not yet
  exposed.

## Catalogue administration

Public country choices are loaded from approved backend destination counts and
filter by ISO code. A signed-in `ADMIN` receives a Catalogue admin link to
`/admin/destination-imports`, where bounded provider searches create unverified
review candidates. Candidate approval is explicit and surfaces source,
duplicate, category, and image-licensing concerns. Provider keys are never sent
to or configured in Vite.

## Production container

Set `VITE_API_BASE_URL` to the deployed HTTPS API origin before a standalone
build. The provider-neutral deployment in `../../deploy` serves the web app and
API from the same origin, so its container uses relative `/api` requests.
Restrict `VITE_GOOGLE_MAPS_API_KEY` to the exact production HTTPS origin.

## Destination maps

Destination details render one Google marker only when coordinates are valid
and `VITE_GOOGLE_MAPS_API_KEY` is configured. A shared loader prevents duplicate
script injection. Missing keys, coordinates, invalid coordinates, and script
failure preserve the detail page and show a clear fallback. “Open in Google
Maps” uses an encoded URL and optional linked Place ID. React never calls
Wikidata, OpenTripMap, or Google Places for destination data.
