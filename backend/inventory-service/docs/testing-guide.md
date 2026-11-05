# Testing Architecture & Guide

## 1. Overview
In the **NexaCommerce Microservices Architecture**, automated testing is a first-class requirement. The test suite guarantees that business logic, data validation, domain invariants, and HTTP API contracts remain intact as new features are added or existing code is refactored.

### Test Suite Summary
* **Total Tests:** 40
* **Failures / Errors:** 0
* **Pass Rate:** 100%
* **Technology Stack:**
  - **JUnit 5 (Jupiter):** Test lifecycle, assertions, nested tests (`@Nested`), display names (`@DisplayName`).
  - **Mockito:** Mocking repository dependencies and isolating service logic (`@Mock`, `@InjectMocks`).
  - **Spring Test & MockMvc:** Fast, standalone HTTP layer testing without starting full web servers.
  - **Jakarta Bean Validation (Hibernate Validator):** Direct validation testing on request DTOs.

---

## 2. Testing Pyramid & Layers

```text
               / \
              /   \
             / E2E \       1 Test: Full Application Context
            /-------\      (InventoryServiceApplicationTests)
           /         \
          / Controller\    7 Tests: MockMvc REST API Tests
         /   & Advices \   4 Tests: GlobalExceptionHandler Tests
        /---------------\
       /    Services     \ 13 Tests: Service Layer with Mockito
      /-------------------\
     / Domain Entities &   \ 10 Tests: Inventory Domain Model
    /         DTOs          \ 5 Tests: InventoryRequest Validation
   /-------------------------\
```

---

## 3. Test Coverage Breakdown

### Layer 1: Domain Model Unit Tests ([`InventoryTest.java`](file:///c:/D%20Driver/Projects/Full%20Project/NexaCommerce-Microservices/backend/inventory-service/src/test/java/com/nexacommerce/inventory_service/domain/model/InventoryTest.java))
Domain tests verify pure Java business invariants without Spring or database dependencies:

* **Entity Creation Invariants:**
  - Valid creation sets initial `reservedQuantity` to 0 and initializes `updatedAt`.
  - Rejects `null` product IDs.
  - Rejects zero or negative product IDs (`0`, `-5`).
  - Rejects negative initial stock quantities.
* **Available Quantity Calculations:**
  - Verifies invariant: `availableQuantity = stockQuantity - reservedQuantity`.
* **Stock Updates:**
  - Allows valid stock updates and updates `updatedAt`.
  - Rejects negative stock quantities.
  - Rejects new stock values that are less than already reserved quantities.
* **Stock Reservations:**
  - Successfully increments `reservedQuantity` and decrements `availableQuantity`.
  - Allows reserving the exact available quantity.
  - Rejects zero or negative reservation amounts (`<= 0`).
  - Rejects reservation requests that exceed available stock.

---

### Layer 2: DTO Validation Tests ([`InventoryRequestTest.java`](file:///c:/D%20Driver/Projects/Full%20Project/NexaCommerce-Microservices/backend/inventory-service/src/test/java/com/nexacommerce/inventory_service/application/dto/InventoryRequestTest.java))
Directly exercises Jakarta Validation constraints on the `InventoryRequest` record using the Validator API:

* **Valid Payload:** Produces 0 constraint violations.
* **`@NotNull` & `@Positive` on `productId`:** Fails when `productId` is null, 0, or negative.
* **`@Min(0)` on `stockQuantity`:** Fails when stock is less than 0.
* **`@NotBlank` on `warehouseLocation`:** Fails when string is null, empty (`""`), or only whitespace (`"   "`).

---

### Layer 3: Application Service Unit Tests ([`InventoryServiceTest.java`](file:///c:/D%20Driver/Projects/Full%20Project/NexaCommerce-Microservices/backend/inventory-service/src/test/java/com/nexacommerce/inventory_service/application/service/InventoryServiceTest.java))
Tests business workflows in complete isolation from the database using Mockito:

* **`create()`:**
  - Verifies that new products are persisted via `repository.save()` when no duplicate exists.
  - Verifies that duplicate product IDs throw an `IllegalArgumentException` and `save()` is never called.
* **`getByProductId()`:**
  - Returns mapped `InventoryResponse` when found.
  - Throws `IllegalArgumentException` ("Inventory not found for product: ...") when missing.
* **`getAll()`:**
  - Returns list of mapped responses for multi-item sets and handles empty sets gracefully.
* **`updateStock()`:**
  - Finds entity, invokes domain update, saves, and returns response.
  - Throws exception if product ID does not exist.
* **`reserveStock()`:**
  - Successfully reserves available stock and saves.
  - Throws exception if product does not exist or requested quantity exceeds available stock.

---

### Layer 4: REST Controller MockMvc Tests ([`InventoryControllerTest.java`](file:///c:/D%20Driver/Projects/Full%20Project/NexaCommerce-Microservices/backend/inventory-service/src/test/java/com/nexacommerce/inventory_service/presentation/controller/InventoryControllerTest.java))
Uses standalone `MockMvc` with simulated HTTP calls:

* **`POST /api/v1/inventory`:** Returns HTTP `200 OK` with JSON fields mapped correctly.
* **`GET /api/v1/inventory/{productId}`:**
  - Returns HTTP `200 OK` with stock details when found.
  - Returns HTTP `404 Not Found` with structured `ErrorResponse` when product does not exist.
* **`GET /api/v1/inventory`:** Returns HTTP `200 OK` with JSON array of inventory items.
* **`PUT /api/v1/inventory/{productId}?quantity=...`:** Returns HTTP `200 OK` with updated stock.
* **`POST /api/v1/inventory/{productId}/reserve?quantity=...`:**
  - Returns HTTP `200 OK` when stock is reserved.
  - Returns HTTP `400 Bad Request` with structured `ErrorResponse` when stock is insufficient.

---

### Layer 5: Global Exception Handler Tests ([`GlobalExceptionHandlerTest.java`](file:///c:/D%20Driver/Projects/Full%20Project/NexaCommerce-Microservices/backend/inventory-service/src/test/java/com/nexacommerce/inventory_service/presentation/exception/GlobalExceptionHandlerTest.java))
Tests exception-to-HTTP-status mapping logic:

* `IllegalArgumentException` containing `"not found"` ➔ **`404 Not Found`**.
* `IllegalArgumentException` containing `"already exists"` ➔ **`409 Conflict`**.
* Generic `IllegalArgumentException` (e.g., negative input) ➔ **`400 Bad Request`**.
* Generic unhandled `Exception` ➔ **`500 Internal Server Error`**.

---

### Layer 6: Application Context Bootstrap ([`InventoryServiceApplicationTests.java`](file:///c:/D%20Driver/Projects/Full%20Project/NexaCommerce-Microservices/backend/inventory-service/src/test/java/com/nexacommerce/inventory_service/InventoryServiceApplicationTests.java))
Full integration smoke test verifying that:
* The Spring Boot application context initializes without bean creation errors.
* Spring Data JPA scans and registers `InventoryRepository`.
* Database connectivity is established.

---

## 4. The Standard Test Pattern (AAA: Arrange, Act, Assert)

Every unit test follows the industry-standard **Arrange, Act, Assert** structure:

```java
@Test
@DisplayName("Should calculate available quantity as stock minus reserved")
void shouldCalculateAvailableQuantity() {
    // 1. Arrange: Prepare data and preconditions
    Inventory inventory = new Inventory(1L, 100, "Warehouse A");
    inventory.reserveStock(30);

    // 2. Act: Execute the method under test
    int available = inventory.getAvailableQuantity();

    // 3. Assert: Verify the outcome
    assertEquals(70, available);
}
```

---

## 5. How to Run the Tests

### Option A: Via Command Line (Maven Wrapper)

* **Run all tests:**
  ```powershell
  .\mvnw test
  ```

* **Run a specific test class:**
  ```powershell
  .\mvnw test -Dtest=InventoryTest
  .\mvnw test -Dtest=InventoryServiceTest
  .\mvnw test -Dtest=InventoryControllerTest
  ```

* **Run a specific test method:**
  ```powershell
  .\mvnw test -Dtest=InventoryTest#shouldReserveStockSuccessfully
  ```

### Option B: Inside IntelliJ IDEA

1. Navigate to any test file under `src/test/java`.
2. Click the **green Play button (▶)** in the left gutter next to the class or method name.
3. Choose **Run ... with Coverage** to see exact line and branch test coverage.
