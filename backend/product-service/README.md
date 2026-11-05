# Product Service - NexaCommerce Microservices

The **Product Service** is a core microservice in the **NexaCommerce** e-commerce platform. It is responsible for managing the product catalog, including product details, pricing, inventory stock thresholds, and category hierarchies following Clean Architecture principles.

---

## 🏗️ Architecture & Tech Stack

* **Language & Framework:** Java 25 / Spring Boot 4.x
* **Architecture:** Clean Architecture (`domain`, `application`, `infrastructure`, `presentation`)
* **Persistence:** Spring Data JPA with PostgreSQL
* **Validation:** Jakarta Bean Validation (`spring-boot-starter-validation`)
* **Containerization:** Docker & Docker Compose
* **Documentation & Testing:** Postman & Actuator Health Monitoring

---

## 📁 Project Structure

```text
product-service/
├── src/main/java/com/nexacommerce/product_service/
│   ├── application/
│   │   ├── dto/           # CategoryRequest, CategoryResponse, ProductRequest, ProductResponse
│   │   └── service/       # CategoryService, ProductService (Business logic)
│   ├── domain/
│   │   └── model/         # Category, Product (JPA Domain Entities)
│   ├── infrastructure/
│   │   └── persistence/   # CategoryRepository, ProductRepository (Spring Data JPA)
│   └── presentation/
│       ├── controller/    # CategoryController, ProductController (REST Endpoints)
│       └── exception/     # GlobalExceptionHandler, ResourceNotFoundException
├── docs/
│   └── product-service-testing-guide.md # Detailed Postman testing guide
├── src/main/resources/
│   └── application.properties           # Application & Database Configuration
├── .env.example                         # Environment configuration template
├── pom.xml
└── README.md
```

---

## ⚙️ Configuration & Environment

The service runs on **Port `8082`** and connects to PostgreSQL.

Create a `.env` file inside `backend/product-service/` by copying `.env.example`:

```properties
SERVER_PORT=8082
DB_HOST=your_db_host
DB_PORT=5432
DB_NAME=product_db
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
```

| Property | Environment Variable | Default / Template Value | Description |
| :--- | :--- | :--- | :--- |
| `server.port` | `SERVER_PORT` | `8082` | HTTP Port for Product Service |
| `spring.datasource.url` | `DB_HOST`, `DB_PORT`, `DB_NAME` | `jdbc:postgresql://your_db_host:5432/product_db` | PostgreSQL Database URL |
| `spring.datasource.username` | `DB_USERNAME` | `your_db_username` | Database Username |
| `spring.datasource.password` | `DB_PASSWORD` | `your_db_password` | Database Password |
| `spring.jpa.hibernate.ddl-auto`| - | `update` | Hibernate Schema Sync |

---

## 🚀 Getting Started

### Prerequisites
* **Java 25** or compatible JDK installed
* **PostgreSQL** or **Docker Desktop** installed and running

### Running the Application

1. **Configure Environment Variables:**
   Copy `.env.example` to `.env` and set your database connection details:
   ```powershell
   cp .env.example .env
   ```

2. **Start PostgreSQL Database:**
   Ensure PostgreSQL is running with your database name (default: `product_db`).

3. **Run via Maven:**
   ```powershell
   ./mvnw spring-boot:run
   ```

4. **Check Service Health:**
   ```text
   GET http://localhost:8082/actuator/health
   ```

---

## 📡 REST API Endpoints

Base URL: `http://localhost:8082/api/v1`

### 1. Categories (`/api/v1/categories`)

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/categories` | Create a new category | `201 Created` |
| `GET` | `/api/v1/categories` | Get all categories | `200 OK` |
| `GET` | `/api/v1/categories/{id}` | Get category by ID | `200 OK` |
| `PUT` | `/api/v1/categories/{id}` | Update category details | `200 OK` |
| `DELETE` | `/api/v1/categories/{id}` | Delete category by ID | `204 No Content` |

### 2. Products (`/api/v1/products`)

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/products` | Create a new product | `201 Created` |
| `GET` | `/api/v1/products` | Get all products | `200 OK` |
| `GET` | `/api/v1/products/{id}` | Get product by ID | `200 OK` |
| `PUT` | `/api/v1/products/{id}` | Update product details | `200 OK` |
| `DELETE` | `/api/v1/products/{id}` | Delete product by ID | `204 No Content` |

> 📖 **Detailed Testing Guide:** For sample request/response JSON payloads and test workflows, refer to [`docs/product-service-testing-guide.md`](./docs/product-service-testing-guide.md).
