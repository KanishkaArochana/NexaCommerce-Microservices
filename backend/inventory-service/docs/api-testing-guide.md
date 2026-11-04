# NexaCommerce Inventory Service - REST API Testing Guide

Comprehensive guide for testing the Inventory Service REST API via Postman, cURL, and automated testing tools.

---

## 1. Environment & Architecture Overview

The Inventory Service is built on **Spring Boot 4.1.1** with **Java 25**, backed by **PostgreSQL 17** running in a Docker container.

```
[ Postman / Client ] 
        │
        │ HTTP (JSON) :8083
        ▼
[ Inventory Service (Spring Boot) ]
        │
        │ JDBC :5433 (Host) -> :5432 (Container)
        ▼
[ Docker Container: inventory-db (PostgreSQL 17) ]
```

### Connection Details

| Property | Value | Description |
| :--- | :--- | :--- |
| **Service Host** | `http://localhost:8083` | Spring Boot Application Port |
| **API Base Path** | `/api/v1/inventory` | REST API Root Endpoint |
| **Database Host** | `localhost:5433` | Host-mapped port for Docker PostgreSQL |
| **Database Name** | `inventory_db` | PostgreSQL Database |
| **Database User** | `your_database_username_here` | Configured via `.env` |

---

## 2. Prerequisites & Service Startup

### Step 1: Start PostgreSQL in Docker
```powershell
# From backend/inventory-service directory
docker compose up -d inventory-db
```

Verify that the database container is healthy:
```powershell
docker ps --filter "name=inventory-db"
```

### Step 2: Start the Inventory Service
Run via IntelliJ IDEA by executing `InventoryServiceApplication.java`, or via CLI:
```powershell
.\mvnw spring-boot:run
```

Verify health check:
```http
GET http://localhost:8083/actuator/health
```
**Expected Response:** `{"status": "UP"}`

---

## 3. Detailed REST API Endpoints Specification

### 3.1. Create Inventory (`POST`)

Creates a new inventory record for a specific product.

* **URL:** `http://localhost:8083/api/v1/inventory`
* **Method:** `POST`
* **Headers:** `Content-Type: application/json`

#### Request Body:
```json
{
  "productId": 1,
  "stockQuantity": 100,
  "warehouseLocation": "Warehouse Colombo"
}
```

#### Field Constraints:
* `productId`: Required, must be greater than 0 (`@NotNull`, `@Positive`).
* `stockQuantity`: Required, minimum 0 (`@Min(0)`).
* `warehouseLocation`: Required, non-empty text (`@NotBlank`).

#### Success Response (`200 OK`):
```json
{
  "id": 1,
  "productId": 1,
  "stockQuantity": 100,
  "reservedQuantity": 0,
  "availableQuantity": 100,
  "warehouseLocation": "Warehouse Colombo",
  "updatedAt": "2026-10-24T10:18:34.420569"
}
```

---

### 3.2. Get All Inventories (`GET`)

Retrieves a list of all inventories stored in the system.

* **URL:** `http://localhost:8083/api/v1/inventory`
* **Method:** `GET`

#### Success Response (`200 OK`):
```json
[
  {
    "id": 1,
    "productId": 1,
    "stockQuantity": 100,
    "reservedQuantity": 0,
    "availableQuantity": 100,
    "warehouseLocation": "Warehouse Colombo",
    "updatedAt": "2026-10-24T10:18:34.420569"
  },
  {
    "id": 2,
    "productId": 2,
    "stockQuantity": 200,
    "reservedQuantity": 0,
    "availableQuantity": 200,
    "warehouseLocation": "Warehouse Colombo 7",
    "updatedAt": "2026-10-24T10:20:00.000000"
  }
]
```

---

### 3.3. Get Inventory by Product ID (`GET`)

Retrieves the inventory status for a single product.

* **URL:** `http://localhost:8083/api/v1/inventory/{productId}`
* **Method:** `GET`
* **Example:** `http://localhost:8083/api/v1/inventory/1`

#### Success Response (`200 OK`):
```json
{
  "id": 1,
  "productId": 1,
  "stockQuantity": 100,
  "reservedQuantity": 0,
  "availableQuantity": 100,
  "warehouseLocation": "Warehouse Colombo",
  "updatedAt": "2026-10-24T10:18:34.420569"
}
```

---

### 3.4. Reserve Stock (`POST`)

Reserves a specific quantity of stock for an active customer order.

* **URL:** `http://localhost:8083/api/v1/inventory/{productId}/reserve?quantity={amount}`
* **Method:** `POST`
* **Example:** `http://localhost:8083/api/v1/inventory/1/reserve?quantity=20`

#### Behavior:
* Increases `reservedQuantity` by the requested quantity.
* Re-calculates `availableQuantity` (`stockQuantity - reservedQuantity`).

#### Success Response (`200 OK`):
```json
{
  "id": 1,
  "productId": 1,
  "stockQuantity": 100,
  "reservedQuantity": 20,
  "availableQuantity": 80,
  "warehouseLocation": "Warehouse Colombo",
  "updatedAt": "2026-10-24T10:22:15.123456"
}
```

---

### 3.5. Update Stock Quantity (`PUT`)

Updates total physical warehouse stock (e.g., when receiving fresh inventory).

* **URL:** `http://localhost:8083/api/v1/inventory/{productId}?quantity={newQuantity}`
* **Method:** `PUT`
* **Example:** `http://localhost:8083/api/v1/inventory/1?quantity=150`

#### Success Response (`200 OK`):
```json
{
  "id": 1,
  "productId": 1,
  "stockQuantity": 150,
  "reservedQuantity": 20,
  "availableQuantity": 130,
  "warehouseLocation": "Warehouse Colombo",
  "updatedAt": "2026-10-24T10:25:00.000000"
}
```

---

## 4. Error Handling & Edge Case Testing

The API uses `GlobalExceptionHandler` to translate exceptions into standardized `ErrorResponse` payloads.

### Scenario A: Duplicate Product Creation
* **Request:** `POST /api/v1/inventory` with an existing `productId`.
* **Status:** `400 Bad Request`
```json
{
  "timestamp": "2026-10-24T10:26:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Inventory already exists for this product",
  "path": "/api/v1/inventory"
}
```

### Scenario B: Product Not Found
* **Request:** `GET /api/v1/inventory/999`
* **Status:** `400 Bad Request`
```json
{
  "timestamp": "2026-10-24T10:27:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Inventory not found for product: 999",
  "path": "/api/v1/inventory/999"
}
```

### Scenario C: Over-Reservation
* **Request:** `POST /api/v1/inventory/1/reserve?quantity=500` (available: 80)
* **Status:** `400 Bad Request`
```json
{
  "timestamp": "2026-10-24T10:28:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Not enough stock available to reserve",
  "path": "/api/v1/inventory/1/reserve"
}
```

### Scenario D: Bean Validation Failure
* **Request:** `POST /api/v1/inventory`
```json
{
  "productId": null,
  "stockQuantity": -10,
  "warehouseLocation": ""
}
```
* **Status:** `400 Bad Request`
```json
{
  "timestamp": "2026-10-24T10:29:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: stockQuantity: must be greater than or equal to 0, warehouseLocation: must not be blank, productId: must not be null",
  "path": "/api/v1/inventory"
}
```

---

## 5. Database Verification (PostgreSQL in Docker)

### Why is `availableQuantity` NOT a column in PostgreSQL?
* In `Inventory.java`, `availableQuantity` is a **derived property** calculated via `stockQuantity - reservedQuantity`.
* Persisting computed values leads to data synchronization bugs and violates database normalization principles.
* The API DTO (`InventoryResponse`) automatically evaluates `inventory.getAvailableQuantity()` and presents it to clients.

### Verify via Terminal:
```powershell
docker exec inventory-db psql -U postgres -d inventory_db -c "SELECT * FROM inventory;"
```

### Verify via pgAdmin 4:
1. Connect to **`localhost:5433`** (Username: `postgres`, Password: `your_database_password_here`).
2. Navigate to `inventory_db` -> `Schemas` -> `public` -> `Tables` -> `inventory`.
3. Right-click -> **View/Edit Data** -> **All Rows**.

---

## 6. Ready-to-Import Postman Collection

Save the JSON below as `NexaCommerce-Inventory.postman_collection.json` and import it into Postman:

```json
{
  "info": {
    "name": "NexaCommerce - Inventory Service",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Create Inventory",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Content-Type", "value": "application/json" }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"productId\": 1,\n  \"stockQuantity\": 100,\n  \"warehouseLocation\": \"Warehouse Colombo\"\n}"
        },
        "url": {
          "raw": "http://localhost:8083/api/v1/inventory",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8083",
          "path": ["api", "v1", "inventory"]
        }
      }
    },
    {
      "name": "2. Get All Inventories",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8083/api/v1/inventory",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8083",
          "path": ["api", "v1", "inventory"]
        }
      }
    },
    {
      "name": "3. Get Inventory by Product ID",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8083/api/v1/inventory/1",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8083",
          "path": ["api", "v1", "inventory", "1"]
        }
      }
    },
    {
      "name": "4. Reserve Stock",
      "request": {
        "method": "POST",
        "url": {
          "raw": "http://localhost:8083/api/v1/inventory/1/reserve?quantity=20",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8083",
          "path": ["api", "v1", "inventory", "1", "reserve"],
          "query": [
            { "key": "quantity", "value": "20" }
          ]
        }
      }
    },
    {
      "name": "5. Update Stock",
      "request": {
        "method": "PUT",
        "url": {
          "raw": "http://localhost:8083/api/v1/inventory/1?quantity=150",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8083",
          "path": ["api", "v1", "inventory", "1"],
          "query": [
            { "key": "quantity", "value": "150" }
          ]
        }
      }
    }
  ]
}
```
