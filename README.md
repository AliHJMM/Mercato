# Buy-01 — Online Marketplace

Buy-01 is a full-featured online marketplace where **sellers** can list products and manage their catalog, while **buyers** browse and discover items — all through a clean, modern web interface.

---

## What Can You Do?

### As a Buyer (Client)

- Browse all available products on the marketplace
- View product details, images, price, and stock
- Create an account and manage your profile

### As a Seller

- Register as a seller and set up your storefront
- Create, edit, and delete your product listings
- Upload product images (JPEG, PNG, GIF, WebP, SVG — up to 2 MB each)
- Manage all your uploaded media from one place
- Update your profile and avatar photo
- View your seller dashboard with sales stats

---

## How to Run

### You need

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

### Steps

**1. Clone the project**

```bash
git clone <repo-url>
cd buy-01
```

**2. Start everything with one command**

```bash
docker compose up --build -d
```

> The first time takes about 5–10 minutes while everything downloads and builds.

**3. Open the app**

Go to **https://localhost** in your browser.

Your browser may show a warning for the local development certificate the first time. Accept it and continue to the site.

That's it!

### API Testing Notes

- Use `https://localhost` for browser and Postman testing.
- Use the exact endpoint path without a trailing slash.
- Example: `/api/auth/register`, not `/api/auth/register/`.
- For image upload, send `multipart/form-data` with the file field named `file`.
- Upload responses return image URLs in the form `/api/media/images/{id}`.
- Full endpoint examples are in `API.md`.

---

## Pages

| Page             | URL                                   | Who can access  |
| ---------------- | ------------------------------------- | --------------- |
| Product Listing  | https://localhost                     | Everyone        |
| Sign In          | https://localhost/login               | Everyone        |
| Sign Up          | https://localhost/register            | Everyone        |
| Seller Dashboard | https://localhost/seller/dashboard    | Sellers         |
| My Products      | https://localhost/seller/products     | Sellers         |
| Add Product      | https://localhost/seller/products/new | Sellers         |
| Media Manager    | https://localhost/seller/media        | Sellers         |
| My Profile       | https://localhost/profile             | Logged-in users |

---

## Project Structure

```
buy-01/
├── backend/          Spring Boot microservices
│   ├── eureka-server/    Service discovery
│   ├── api-gateway/      Main entry point for all API calls
│   ├── user-service/     Accounts and authentication
│   ├── product-service/  Product management
│   └── media-service/    Image uploads and storage
├── frontend/         Angular web application
├── docker-compose.yml
├── API.md            Full API documentation
└── README.md
```

---

## Stopping the App

```bash
docker compose down
```

To also remove all stored data (products, users, images):

```bash
docker compose down -v
```
