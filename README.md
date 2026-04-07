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
- Upload product images (JPEG, PNG, GIF — up to 2 MB each)
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
docker compose up --build
```

> The first time takes about 5–10 minutes while everything downloads and builds.

**3. Open the app**

Go to **http://localhost** in your browser.

That's it! 🎉

---

## Pages

| Page | URL | Who can access |
|------|-----|----------------|
| Product Listing | http://localhost | Everyone |
| Sign In | http://localhost/login | Everyone |
| Sign Up | http://localhost/register | Everyone |
| Seller Dashboard | http://localhost/seller/dashboard | Sellers |
| My Products | http://localhost/seller/products | Sellers |
| Add Product | http://localhost/seller/products/new | Sellers |
| Media Manager | http://localhost/seller/media | Sellers |
| My Profile | http://localhost/profile | Logged-in users |

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
