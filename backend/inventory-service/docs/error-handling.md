# Global Error Handling Guide

## 1. Overview
In the **NexaCommerce Microservices Architecture**, error handling is managed centrally using Spring Boot's `@RestControllerAdvice`. Instead of scattering individual `try-catch` blocks across controllers and services, exceptions are allowed to bubble up and are intercepted globally to produce consistent, structured, and predictable HTTP responses.

---

## 2. Error Flow Architecture

```text
[ Client / Frontend / Postman ]
               │
               ▼  HTTP Request
      [ InventoryController ]
               │
               ▼  Method Call
       [ InventoryService ]
               │
               ▼  Business Check
          [ Inventory ]  ──── (Validation Fails) ────► throws Exception
                                                             │
                                                             ▼
                                                [ GlobalExceptionHandler ]
                                                    (@RestControllerAdvice)
                                                             │
                                                             ▼
                                                    [ ErrorResponse ]
                                                             │
[ Client receives clean JSON + HTTP Status Code ] ◄──────────┘
```

---

## 3. Standard Error Response Format

All error responses across the microservices adhere to the [`ErrorResponse`](file:///c:/D%20Driver/Projects/Full%20Project/NexaCommerce-Microservices/backend/inventory-service/src/main/java/com/nexacommerce/inventory_service/presentation/exception/ErrorResponse.java) structure:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient available stock",
  "validationErrors": null,
  "timestamp": "2026-10-22T21:14:00.123456"
}
```

### Response Fields

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | Integer | HTTP Status Code (e.g., `400`, `404`, `409`, `500`). |
| `error` | String | Standard HTTP error phrase (e.g., `"Bad Request"`, `"Not Found"`). |
| `message` | String | A user-friendly, descriptive explanation of why the request failed. |
| `validationErrors` | Map<String, String> | Key-value pairs of field names and validation messages (only present for `400` validation errors; `null` otherwise). |
| `timestamp` | LocalDateTime | Timestamp when the error was captured on the server. |

---

## 4. HTTP Status Code Mapping

[`GlobalExceptionHandler`](file:///c:/D%20Driver/Projects/Full%20Project/NexaCommerce-Microservices/backend/inventory-service/src/main/java/com/nexacommerce/inventory_service/presentation/exception/GlobalExceptionHandler.java) maps exceptions to appropriate HTTP statuses:

### A. 400 Bad Request (Business Logic Violations)
* **Triggered by:** `IllegalArgumentException` (domain invariants violated).
* **Examples:**
  - Reserving more stock than available (`quantity > getAvailableQuantity()`).
  - Setting negative stock quantities (`quantity < 0`).
  - Invalid product ID (`productId <= 0`).

**Response Example:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient available stock",
  "validationErrors": null,
  "timestamp": "2026-10-22T21:14:00"
}
```

---

### B. 400 Bad Request (DTO Validation Failures)
* **Triggered by:** `MethodArgumentNotValidException` (Jakarta Bean Validation `@Valid` fails on incoming request body).
* **Examples:**
  - `@NotNull` on `productId`
  - `@Min(0)` on `stockQuantity`
  - `@NotBlank` on `warehouseLocation`

**Response Example:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "validationErrors": {
    "productId": "must not be null",
    "stockQuantity": "must be greater than or equal to 0",
    "warehouseLocation": "must not be blank"
  },
  "timestamp": "2026-10-22T21:14:00"
}
```

---

### C. 404 Not Found (Missing Resources)
* **Triggered by:** `IllegalArgumentException` containing `"not found"`.
* **Example:** Requesting a product ID that does not exist in the database.

**Response Example (`GET /api/inventory/9999`):**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Inventory not found for product: 9999",
  "validationErrors": null,
  "timestamp": "2026-10-22T21:14:00"
}
```

---

### D. 409 Conflict (Duplicate Records)
* **Triggered by:** `IllegalArgumentException` containing `"already exists"`.
* **Example:** Attempting to create an inventory record for a `productId` that already has an existing inventory.

**Response Example (`POST /api/inventory`):**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Inventory already exists for this product",
  "validationErrors": null,
  "timestamp": "2026-10-22T21:14:00"
}
```

---

### E. 500 Internal Server Error (Fallback)
* **Triggered by:** Any unhandled `java.lang.Exception`.
* **Purpose:** Catches unexpected system crashes or database connectivity errors gracefully without leaking raw database stack traces to the public API client.

**Response Example:**
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected internal error occurred: Connection timeout",
  "validationErrors": null,
  "timestamp": "2026-10-22T21:14:00"
}
```

---

## 5. Developer Guidelines: How to Throw Errors

When writing new features in services or domain entities, follow these conventions:

### Rule 1: Use Guard Clauses in Domain Entities
Always protect entity invariants in constructors and business methods:
```java
public void reserveStock(int quantity) {
    if (quantity <= 0) {
        throw new IllegalArgumentException("Reservation quantity must be positive");
    }
    if (quantity > getAvailableQuantity()) {
        throw new IllegalArgumentException("Insufficient available stock");
    }
    this.reservedQuantity += quantity;
}
```

### Rule 2: Throw Meaningful Messages in Services
```java
// Resource not found: Include "not found" in the message for 404 mapping
inventoryRepository.findByProductId(productId)
    .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product: " + productId));

// Duplicate resource: Include "already exists" for 409 mapping
if (inventoryRepository.findByProductId(request.productId()).isPresent()) {
    throw new IllegalArgumentException("Inventory already exists for this product");
}
```

### Rule 3: Do NOT Use Local `try-catch` in Controllers
Controllers should remain lean and only declare business calls. Let the `GlobalExceptionHandler` handle the exceptions:

```java
// ✅ RECOMMENDED:
@PostMapping
public InventoryResponse create(@Valid @RequestBody InventoryRequest request) {
    return inventoryService.create(request);
}

// ❌ AVOID:
@PostMapping
public ResponseEntity<?> create(@RequestBody InventoryRequest request) {
    try {
        return ResponseEntity.ok(inventoryService.create(request));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage()); // Don't do this!
    }
}
```
