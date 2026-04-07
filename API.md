# Mercato API Documentation

All requests go through **HTTPS** at `https://localhost` (nginx-ssl → frontend → api-gateway).  

---

## Authentication

Protected endpoints require a JWT token:

```
Authorization: Bearer <your_token>
```

Tokens are returned by `/api/auth/register` and `/api/auth/login`.

---

## Auth Endpoints

### Register

```
POST /api/auth/register
Content-Type: application/json
```

**Body:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123",
  "role": "CLIENT"
}
```

> `role` must be `"CLIENT"` or `"SELLER"`

**Response `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "id": "664abc123...",
  "username": "johndoe",
  "email": "john@example.com",
  "role": "CLIENT",
  "avatarUrl": null
}
```

| Status | When |
|--------|------|
| `400`  | Missing/invalid fields |
| `405`  | Wrong HTTP method |
| `409`  | Email or username already taken |

---

### Login

```
POST /api/auth/login
Content-Type: application/json
```

**Body:**
```json
{
  "email": "john@example.com",
  "password": "secret123"
}
```

**Response `200 OK`:** Same shape as register response.

| Status | When |
|--------|------|
| `400`  | Missing fields |
| `401`  | Wrong email or password |
| `405`  | Wrong HTTP method |

---

## User Endpoints

### Get My Profile

```
GET /api/users/me
Authorization: Bearer <token>
```

**Response `200 OK`:**
```json
{
  "id": "664abc123...",
  "username": "johndoe",
  "email": "john@example.com",
  "role": "CLIENT",
  "avatarUrl": "/api/media/images/media789...",
  "createdAt": "2024-05-20T10:30:00"
}
```

| Status | When |
|--------|------|
| `401`  | No/invalid token |
| `405`  | Wrong HTTP method |

---

### Update My Profile

```
PUT /api/users/me
Authorization: Bearer <token>
Content-Type: application/json
```

**Body:** *(all fields optional — only include what you want to change)*
```json
{
  "username": "john_updated",
  "avatarUrl": "/api/media/images/media789..."
}
```

> Send only the fields you want to change. Username must be 3–50 characters.

**Response `200 OK`:** Updated user object.

| Status | When |
|--------|------|
| `400`  | Username too short/long (< 3 or > 50 chars) |
| `401`  | No/invalid token |
| `405`  | Wrong HTTP method |
| `409`  | Username already taken by another user |

---

### Delete My Account

```
DELETE /api/users/me
Authorization: Bearer <token>
```

**Response `204 No Content`**

| Status | When |
|--------|------|
| `401`  | No/invalid token |
| `404`  | User not found |
| `405`  | Wrong HTTP method |

---

## Product Endpoints

### List All Products *(public)*

```
GET /api/products
```

**Response `200 OK`:**
```json
[
  {
    "id": "prod123...",
    "name": "Wireless Headphones",
    "description": "Premium noise-cancelling headphones",
    "price": 79.99,
    "quantity": 50,
    "sellerId": "seller456...",
    "sellerName": "techstore",
    "imageUrls": ["/api/media/images/media789..."],
    "createdAt": "2024-05-20T10:30:00",
    "updatedAt": "2024-05-20T10:30:00"
  }
]
```

---

### Get Product by ID *(public)*

```
GET /api/products/{id}
```

**Response `200 OK`:** Single product object.

| Status | When |
|--------|------|
| `404`  | Product not found |

---

### Get My Products *(SELLER only)*

```
GET /api/products/my
Authorization: Bearer <seller_token>
```

**Response `200 OK`:** Array of the authenticated seller's products.

| Status | When |
|--------|------|
| `401`  | No/invalid token |
| `403`  | Not a SELLER |

---

### Create Product *(SELLER only)*

```
POST /api/products
Authorization: Bearer <seller_token>
Content-Type: application/json
```

**Body:**
```json
{
  "name": "Wireless Headphones",
  "description": "Premium noise-cancelling headphones",
  "price": 79.99,
  "quantity": 50,
  "imageUrls": []
}
```

> `price` must be > 0 · `quantity` must be ≥ 0  
> `imageUrls` — use the `url` field returned from Upload Image, or leave as `[]`

**Response `201 Created`:** Created product object.

| Status | When |
|--------|------|
| `400`  | Validation failure (price ≤ 0, name missing, etc.) |
| `401`  | No/invalid token |
| `403`  | Not a SELLER |
| `405`  | Wrong HTTP method |

---

### Update Product *(SELLER only, must own product)*

```
PUT /api/products/{id}
Authorization: Bearer <seller_token>
Content-Type: application/json
```

**Body:** Same as Create.

**Response `200 OK`:** Updated product object.

| Status | When |
|--------|------|
| `400`  | Validation failure |
| `401`  | No/invalid token |
| `403`  | Not a SELLER or don't own this product |
| `404`  | Product not found |

---

### Delete Product *(SELLER only, must own product)*

```
DELETE /api/products/{id}
Authorization: Bearer <seller_token>
```

**Response `204 No Content`**

| Status | When |
|--------|------|
| `401`  | No/invalid token |
| `403`  | Not a SELLER or don't own this product |
| `404`  | Product not found |

---

## Order Endpoints

### Place Order *(CLIENT only)*

```
POST /api/orders
Authorization: Bearer <client_token>
Content-Type: application/json
```

**Body:**
```json
{
  "items": [
    { "productId": "prod123...", "quantity": 2 },
    { "productId": "prod456...", "quantity": 1 }
  ]
}
```

**Response `201 Created`:**
```json
{
  "id": "order789...",
  "buyerId": "client123...",
  "items": [
    {
      "productId": "prod123...",
      "productName": "Wireless Headphones",
      "sellerName": "techstore",
      "imageUrl": "/api/media/images/media789...",
      "price": 79.99,
      "quantity": 2,
      "subtotal": 159.98
    }
  ],
  "total": 159.98,
  "status": "PLACED",
  "createdAt": "2024-05-20T11:00:00"
}
```

| Status | When |
|--------|------|
| `400`  | Missing items or quantity < 1 |
| `401`  | No/invalid token |
| `403`  | Not a CLIENT |
| `404`  | Product not found |
| `409`  | Insufficient stock |

---

### Get My Orders *(authenticated)*

```
GET /api/orders/my
Authorization: Bearer <token>
```

**Response `200 OK`:** Array of order objects, newest first.

| Status | When |
|--------|------|
| `401`  | No/invalid token |

---

## Media Endpoints

### Upload Image *(SELLER only)*

```
POST /api/media/images
Authorization: Bearer <seller_token>
Content-Type: multipart/form-data
```

**Form fields:**

| Field | Required | Description |
|-------|----------|-------------|
| `file` | Yes | Image file (JPEG, PNG, GIF, WebP) — max **2 MB** |
| `productId` | No | Associate image with a product ID |

**How to send in Postman:**
1. Set method to `POST`
2. Body → `form-data`
3. Add key `file`, change type to **File**, select your image
4. Add key `productId` (optional), type Text, enter a product ID

**Response `201 Created`:**
```json
{
  "id": "media789...",
  "originalFilename": "headphones.jpg",
  "mimeType": "image/jpeg",
  "size": 145320,
  "url": "/api/media/images/media789...",
  "uploadedBy": "seller456...",
  "productId": "prod123...",
  "createdAt": "2024-05-20T11:00:00"
}
```

> Use the returned `url` value in product `imageUrls` when creating/updating a product.

| Status | When |
|--------|------|
| `400`  | Not an image, or file exceeds 2 MB |
| `401`  | No/invalid token |
| `403`  | Not a SELLER |

---

### Serve Image *(public)*

```
GET /api/media/images/{id}
```

Returns the raw image binary with `Content-Type` and `Cache-Control` headers.

---

### Get Image Info *(authenticated)*

```
GET /api/media/images/{id}/info
Authorization: Bearer <token>
```

**Response `200 OK`:** Media object (same shape as upload response).

---

### Get My Uploads *(SELLER only)*

```
GET /api/media/my
Authorization: Bearer <seller_token>
```

**Response `200 OK`:** Array of media objects uploaded by the authenticated seller.

| Status | When |
|--------|------|
| `401`  | No/invalid token |
| `403`  | Not a SELLER |

---

### Delete Image *(SELLER only, must own image)*

```
DELETE /api/media/images/{id}
Authorization: Bearer <seller_token>
```

**Response `204 No Content`**

| Status | When |
|--------|------|
| `401`  | No/invalid token |
| `403`  | Not a SELLER or don't own this image |
| `404`  | Image not found |

---

## Common Error Format

```json
{
  "timestamp": "2024-05-20T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: insufficient permissions"
}
```

Validation errors (`400`) include field-level details:

```json
{
  "timestamp": "2024-05-20T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "details": {
    "price": "must be greater than 0",
    "name": "must not be blank"
  }
}
```

---

## Health Checks

| Service | URL |
|---------|-----|
| API Gateway | `GET https://localhost/actuator/health` |
| User Service | `GET http://localhost:8081/actuator/health` |
| Product Service | `GET http://localhost:8082/actuator/health` |
| Media Service | `GET http://localhost:8083/actuator/health` |
| Eureka | `GET http://localhost:8761` |
| MinIO Console | `http://localhost:9001` (user: `minioadmin` / pass: `minioadmin`) |

---

## Rate Limits

| Route | Limit |
|-------|-------|
| `POST /api/auth/*` | 10 requests/minute per IP |
| `POST /api/media/*` | 20 requests/minute per IP |
| All other routes | No limit |

Exceeded limit returns `429 Too Many Requests` with `Retry-After: 60` header.
