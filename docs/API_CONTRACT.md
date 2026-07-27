# Initial REST API Contract

## Health check

### GET `/api/health`

Response:

```json
{
  "status": "ok",
  "message": "Tourism API is running"
}
```

## Get destinations

### GET `/api/destinations`

Response:

```json
[
  {
    "id": 1,
    "name": "Murchison Falls National Park",
    "description": "A major national park known for wildlife and the Nile.",
    "location": "Northwestern Uganda",
    "category": "Wildlife",
    "imageUrl": "https://example.com/murchison.jpg",
    "latitude": 2.2758,
    "longitude": 31.6841,
    "rating": 4.8
  }
]
```

## Get one destination

### GET `/api/destinations/{id}`

Possible responses:

- `200 OK`
- `400 Bad Request`
- `404 Not Found`
