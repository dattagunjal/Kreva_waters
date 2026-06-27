# 💧 AquaPure — Mineral Water Management System

Full-stack web application for managing mineral water product sales, orders, and admin reporting.

## Tech Stack

- **Backend**: Spring Boot 3 (Java 17), Spring Security + JWT, Spring Data JPA, MySQL
- **Frontend**: Angular 17, TypeScript, SCSS, Reactive Forms

## Project Structure

```
mineral-water/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/mineralwater/
│   │   ├── controller/         # REST API controllers
│   │   ├── dto/                # Data Transfer Objects
│   │   ├── model/              # JPA Entities
│   │   ├── repository/         # Spring Data repositories
│   │   ├── security/           # JWT + Spring Security config
│   │   └── service/            # Business logic
│   └── src/main/resources/
│       └── application.properties
│
└── frontend/                   # Angular 17 application
    └── src/
        ├── app/
        │   ├── app.module.ts
        │   ├── app-routing.module.ts
        │   ├── app.component.ts/html
        │   ├── core/
        │   │   ├── guards/         # AuthGuard, AdminGuard
        │   │   ├── interceptors/   # JwtInterceptor
        │   │   └── services/       # auth, cart, order, product, sales
        │   ├── shared/
        │   │   └── models/         # TypeScript interfaces
        │   └── modules/
        │       ├── auth/           # login, register
        │       ├── products/       # product-list
        │       ├── cart/           # cart view
        │       ├── orders/         # checkout + order history
        │       └── admin/
        │           ├── dashboard/  # product & order management
        │           └── sales/      # daily/monthly sales reports + PDF export
        ├── environments/
        │   ├── environment.ts      # development (localhost:8080)
        │   └── environment.prod.ts
        └── styles.scss             # global styles
```

## Setup & Run

### Prerequisites
- Java 17+
- Node.js 18+ & npm
- MySQL 8

### Backend

```bash
cd backend

# Update src/main/resources/application.properties with your DB credentials
# spring.datasource.username=root
# spring.datasource.password=your_password

mvn spring-boot:run
# Runs on http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm start
# Runs on http://localhost:4200
```

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/auth/register | Public | Register |
| POST | /api/auth/login | Public | Login |
| GET | /api/products | Public | List products |
| POST | /api/products | Admin | Create product |
| PUT | /api/products/{id} | Admin | Update product |
| DELETE | /api/products/{id} | Admin | Delete product |
| POST | /api/orders | User | Place order |
| GET | /api/orders/my | User | My orders |
| GET | /api/orders/admin/all | Admin | All orders |
| PUT | /api/orders/admin/{id}/status | Admin | Update order status |
| GET | /api/admin/sales/daily | Admin | Daily sales report |
| GET | /api/admin/sales/monthly | Admin | Monthly sales report |
| GET | /api/admin/sales/monthly-chart | Admin | Yearly chart data |
| GET | /api/admin/sales/export/daily | Admin | Export daily PDF |
| GET | /api/admin/sales/export/monthly | Admin | Export monthly PDF |

## Features

- **User**: Register/Login, browse products, add to cart, checkout, view order history, cancel pending orders
- **Admin**: Product CRUD, order status management, daily/monthly sales dashboard, PDF export
- **Security**: JWT authentication, role-based access (USER / ADMIN), HTTP interceptor for token injection
