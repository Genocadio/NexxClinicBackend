# Upload API — REST Endpoints

Base path: `/api/uploads`

---

## POST `/api/uploads` — Upload a file

### Request

| Field        | Type   | Location | Required | Default  | Description |
|-------------|--------|----------|----------|----------|-------------|
| `file`      | File   | form-data | yes     | —        | The file to upload |
| `visibility` | String | form-data | no      | `PUBLIC` | `PUBLIC` or `PRIVATE` |

```
POST /api/uploads
Content-Type: multipart/form-data

file: @image.jpeg
visibility: PUBLIC
```

### Response — 200 OK

```json
{
  "status": "SUCCESS",
  "message": "File uploaded successfully",
  "data": {
    "id": "a1b2c3d4-...",
    "url": "https://supa.med.rw/storage/v1/object/public/uploads-public/uuid.jpeg"
  }
}
```

Private uploads return `"url": null` — use the id to request a signed URL separately.

### Response — 400 Bad Request

```json
{
  "status": "ERROR",
  "message": "File is empty",
  "data": null
}
```

---

## DELETE `/api/uploads/{id}` — Delete by ID

| Parameter | Location | Description |
|-----------|----------|-------------|
| `id`      | path     | UUID of the upload |

```
DELETE /api/uploads/a1b2c3d4-...
```

### Response

```json
{ "status": "SUCCESS", "message": "File deleted successfully", "data": null }
```

```json
{ "status": "ERROR",  "message": "Upload not found", "data": null }
```

---

## DELETE `/api/uploads/by-url?url=...` — Delete by URL

| Parameter | Location | Description |
|-----------|----------|-------------|
| `url`     | query    | Full public URL of the upload |

```
DELETE /api/uploads/by-url?url=https://supa.med.rw/storage/v1/object/public/uploads-public/uuid.jpeg
```

### Response

Same as delete by ID.

---

## PATCH `/api/uploads/{id}/visibility` — Change visibility by ID

| Parameter     | Location | Description |
|--------------|----------|-------------|
| `id`         | path     | UUID of the upload |
| `visibility` | body     | `"PUBLIC"` or `"PRIVATE"` |

```
PATCH /api/uploads/a1b2c3d4-...
Content-Type: application/json

{ "visibility": "PUBLIC" }
```

### Response

```json
{
  "status": "SUCCESS",
  "message": "Upload visibility updated",
  "data": { "id": "a1b2c3d4-...", "url": "https://..." }
}
```

When changed to `PRIVATE`, `url` is `null`. When changed to `PUBLIC`, `url` is the public URL.

```json
{ "status": "ERROR", "message": "visibility is required", "data": null }
```

---

## PATCH `/api/uploads/by-url/visibility?url=...` — Change visibility by URL

| Parameter     | Location  | Description |
|--------------|-----------|-------------|
| `url`        | query     | Full public URL of the upload |
| `visibility` | body      | `"PUBLIC"` or `"PRIVATE"` |

```
PATCH /api/uploads/by-url/visibility?url=https://supa.med.rw/storage/v1/object/public/uploads-public/uuid.jpeg
Content-Type: application/json

{ "visibility": "PRIVATE" }
```

### Response

Same as update by ID.

---

## GET `/api/uploads/by-url?url=...` — Lookup by URL

```
GET /api/uploads/by-url?url=https://supa.med.rw/storage/v1/object/public/uploads-public/uuid.jpeg
```

### Response

```json
{
  "status": "SUCCESS",
  "message": "Upload found",
  "data": { "id": "a1b2c3d4-...", "url": "https://..." }
}
```

```json
{ "status": "ERROR", "message": "Upload not found for URL: https://...", "data": null }
```

> URLs are unique — no two uploads can share the same URL (`@Column(unique = true)`).

---

## Response envelope

Every response follows the `ApiResponse` shape:

| Field        | Type     | Description |
|-------------|----------|-------------|
| `status`    | String   | `SUCCESS`, `ERROR`, `UNAUTHENTICATED`, `UNAUTHORISED`, or `PARTIAL_SUCCESS` |
| `message`   | String   | Human-readable result message |
| `data`      | Object   | Payload (shape varies per endpoint, can be `null`) |
| `pagination` | Object  | Always `null` for these endpoints |
