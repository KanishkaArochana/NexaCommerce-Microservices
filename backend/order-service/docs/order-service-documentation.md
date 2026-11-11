# NexaCommerce Order Service - Comprehensive Technical Documentation

Comprehensive architectural, design, containerization, and API testing documentation for the **NexaCommerce Order Service**.

---

## 1. Executive Overview & Tech Stack

The **Order Service** manages customer order lifecycles, item line calculations, order status transitions, and persistence within the NexaCommerce microservices ecosystem.

### Technology Stack
* **Language & Runtime:** Java 25 (Eclipse Temurin)
* **Framework:** Spring Boot 4.1.2 (Spring Framework 7.x)
* **Data Access & ORM:** Spring Data JPA, Hibernate 7.4.x
* **Database:** PostgreSQL 17 (Alpine)
* **Validation:** Jakarta Validation (`hibernate-validator` 9.x)
* **Testing:** JUnit 5 (Jupiter 6.x), Mockito 5.x, AssertJ, Spring WebMvcTest (`MockMvc`)
* **Containerization:** Docker (Multi-Stage Build), Docker Compose

---

## 2. Architecture & Design Patterns

The service follows a **Layered Domain-Driven Architecture (Clean Architecture)** ensuring high cohesion and loose coupling.

```
                           [ HTTP / REST Clients (Postman / Gateway / Frontend) ]
                                                     │
┌────────────────────────────────────────────────────┼────────────────────────────────────────────────────┐
│ PRESENTATION LAYER                                 │                                                    │
│                                                    ▼                                                    │
│   OrderController (/api/v1/orders) ──► Validates Request DTOs (@Valid)                                 │
└────────────────────────────────────────────────────┬────────────────────────────────────────────────────┘
                                                     │ (OrderRequest, OrderItemRequest)
┌────────────────────────────────────────────────────┼────────────────────────────────────────────────────┐
│ APPLICATION LAYER                                  │                                                    │
│                                                    ▼                                                    │
│   OrderService ─────────────────────► Business Rules, DTO Mapping, Transaction Management (@Transactional)
└────────────────────────────────────────────────────┬────────────────────────────────────────────────────┘
                                                     │
┌────────────────────────────────────────────────────┼────────────────────────────────────────────────────┐
│ DOMAIN LAYER                                       │                                                    │
│                                                    ▼                                                    │
│   Order ◄───(1:N)───► OrderItem       Encapsulates business rules (status transitions, total amount)   │
│   OrderStatus                         (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)              │
└────────────────────────────────────────────────────┬────────────────────────────────────────────────────┘
                                                     │
┌────────────────────────────────────────────────────┼────────────────────────────────────────────────────┐
│ INFRASTRUCTURE LAYER                               │                                                    │
│                                                    ▼                                                    │
│   OrderRepository                     Spring Data JPA abstraction over PostgreSQL                       │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

1. **`presentation/controller`** (`OrderController`):
   * Exposes RESTful HTTP endpoints (`src/main/java/com/nexacommerce/order_service/presentation/controller/OrderController.java`).
   * Enforces input contract validation via Jakarta Validation (`@NotNull`, `@Positive`, `@NotEmpty`, `@DecimalMin`).
   * Maps domain responses to HTTP status codes (`201 CREATED`, `200 OK`, `400 BAD REQUEST`).

2. **`application/service`** (`OrderService`):
   * Orchestrates use-cases: create order, find by ID, list by user, update status (`src/main/java/com/nexacommerce/order_service/application/service/OrderService.java`).
   * Manages transactional boundaries (`@Transactional`).
   * Transforms incoming Records (DTOs) into Domain Entities and vice-versa.

3. **`domain/model`** (`Order`, `OrderItem`, `OrderStatus`):
   * Contains core business rules, entity relationships, and internal calculation logic (`src/main/java/com/nexacommerce/order_service/domain/model/`).
   * Uses JPA lifecycle callbacks (`@PrePersist`, `@PreUpdate`) for audit timestamps.

4. **`infrastructure/persistence`** (`OrderRepository`):
   * Handles database persistence queries (`src/main/java/com/nexacommerce/order_service/infrastructure/persistence/OrderRepository.java`).

---

## 3. Deep Dive: `Order` & `OrderItem` Relationship

The core of the order domain is the bidirectional **One-to-Many** mapping between `Order` (Parent Aggregate Root) and `OrderItem` (Child Entity).

```
 ┌──────────────────────────────────────────────┐
 │                    Order                     │
 ├──────────────────────────────────────────────┤
 │ - id: Long (PK)                              │
 │ - userId: Long                               │
 │ - orderDate: LocalDateTime                   │
 │ - status: OrderStatus                        │
 │ - totalAmount: BigDecimal                    │
 │ - createdAt: LocalDateTime                   │
 │ - updatedAt: LocalDateTime                   │
 └──────────────────────┬───────────────────────┘
                        │ 1
                        │
                        │ owns / cascades
                        │
                        ▼ *
 ┌──────────────────────────────────────────────┐
 │                  OrderItem                   │
 ├──────────────────────────────────────────────┤
 │ - id: Long (PK)                              │
 │ - order_id: Long (FK -> orders.id)           │
 │ - productId: Long                            │
 │ - quantity: Integer                          │
 │ - unitPrice: BigDecimal                      │
 │ - createdAt: LocalDateTime                   │
 │ - updatedAt: LocalDateTime                   │
 └──────────────────────────────────────────────┘
```

### Relationship Implementation

```java
// Inside Order.java (Parent)
@OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
)
private List<OrderItem> items = new ArrayList<>();
```

```java
// Inside OrderItem.java (Child)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id", nullable = false)
private Order order;
```

### Detailed Breakdown of Annotations & Properties

| Configuration | Why It Is Used & What It Does |
| :--- | :--- |
| **`mappedBy = "order"`** | Declares `OrderItem` as the owner of the foreign key relationship. The foreign key column `order_id` resides in the `order_items` table. It informs Hibernate not to create a redundant join table. |
| **`cascade = CascadeType.ALL`** | Propagates all state transitions (`PERSIST`, `MERGE`, `REMOVE`, `REFRESH`, `DETACH`) from parent to children. Saving an `Order` (`orderRepository.save(order)`) automatically saves all associated `OrderItem` entities without needing an `OrderItemRepository`. |
| **`orphanRemoval = true`** | Ensures data integrity. If an `OrderItem` is removed from the `items` list in memory (e.g., `order.getItems().remove(item)`), Hibernate issues a database `DELETE` query to delete the orphaned row. |
| **`fetch = FetchType.LAZY`** | When loading an `OrderItem`, the parent `Order` is not eagerly fetched unless explicitly accessed, optimizing memory and query performance. |

### Domain Encapsulation & Synchronization Method (`addItem`)

To maintain bidirectional consistency in memory, the domain model encapsulates item addition:

```java
public void addItem(OrderItem item) {
    item.setOrder(this);      // 1. Sets the child's reference back to parent
    items.add(item);          // 2. Adds child to parent list
    calculateTotal();         // 3. Recalculates total price from item subtotals
}

private void calculateTotal() {
    totalAmount = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

* **Benefit:** Prevents mismatched state where the parent knows about the child but the child's foreign key is null.
* **Benefit:** Guarantee that `totalAmount` is calculated from entity state and cannot be tampered with via request payloads.

---

## 4. API Endpoints & Postman Testing Guide

* **Base URL:** `http://localhost:8085/api/v1/orders`
* **Default Port:** `8085`

### Summary of Endpoints

| HTTP Method | Endpoint | Description | Success Status |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/orders` | Create a new order with items | `201 Created` |
| **GET** | `/api/v1/orders/{id}` | Get order details by order ID | `200 OK` |
| **GET** | `/api/v1/orders` | Retrieve all orders | `200 OK` |
| **GET** | `/api/v1/orders/user/{userId}` | Retrieve all orders for a specific user | `200 OK` |
| **PATCH** | `/api/v1/orders/{id}/status?status={STATUS}` | Update order status | `200 OK` |
| **GET** | `/actuator/health` | Healthcheck probe for container orchestration | `200 OK` |

---

### Detailed Request & Response Examples

#### 1. Create a New Order (`POST`)
* **URL:** `http://localhost:8085/api/v1/orders`
* **Method:** `POST`
* **Headers:** `Content-Type: application/json`
* **Request Body:**
```json
{
  "userId": 1001,
  "items": [
    {
      "productId": 501,
      "quantity": 2,
      "unitPrice": 2500.00
    },
    {
      "productId": 502,
      "quantity": 1,
      "unitPrice": 1200.00
    }
  ]
}
```
* **Response:** (`201 Created`)
```json
{
  "id": 1,
  "userId": 1001,
  "orderDate": "2026-11-11T14:30:00",
  "status": "PENDING",
  "totalAmount": 6200.00,
  "items": [
    {
      "id": 1,
      "productId": 501,
      "quantity": 2,
      "unitPrice": 2500.00,
      "subtotal": 5000.00
    },
    {
      "id": 2,
      "productId": 502,
      "quantity": 1,
      "unitPrice": 1200.00,
      "subtotal": 1200.00
    }
  ],
  "createdAt": "2026-11-11T14:30:00",
  "updatedAt": "2026-11-11T14:30:00"
}
```

---

#### 2. Get Order by ID (`GET`)
* **URL:** `http://localhost:8085/api/v1/orders/1`
* **Method:** `GET`
* **Response:** (`200 OK`) — Returns the order JSON object.

---

#### 3. Get Orders by User ID (`GET`)
* **URL:** `http://localhost:8085/api/v1/orders/user/1001`
* **Method:** `GET`
* **Response:** (`200 OK`) — Returns a JSON array of orders for user `1001`.

---

#### 4. Update Order Status (`PATCH`)
* **URL:** `http://localhost:8085/api/v1/orders/1/status?status=CONFIRMED`
* **Method:** `PATCH`
* **Available Status Values:** `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`
* **Response:** (`200 OK`)
```json
{
  "id": 1,
  "userId": 1001,
  "status": "CONFIRMED",
  "totalAmount": 6200.00,
  ...
}
```

---

### Common Postman Pitfalls & Troubleshooting

1. **`Error: connect ECONNREFUSED 127.0.0.1:8085`**:
   * **Cause:** The Order Service application is not running.
   * **Fix:** Start the service in IntelliJ (`OrderServiceApplication.java` ▶) or start via Docker (`docker compose up -d`).

2. **Sending `GET` instead of `POST` for Order Creation**:
   * **Cause:** In Postman, keeping `GET` selected when sending a body to `/api/v1/orders`.
   * **Fix:** Change the HTTP method dropdown in Postman to **`POST`**.

3. **`400 Bad Request` on Creation**:
   * **Cause:** Validation constraint failed (`userId` is null or negative, `items` array is empty, or `unitPrice` is 0 or negative).
   * **Fix:** Ensure request body satisfies all `@NotNull`, `@Positive`, and `@DecimalMin` rules.

---

## 5. Docker & Containerization Architecture

The container environment is orchestrated via a multi-stage Dockerfile and Docker Compose setup.

```
                              [ Host Machine (Developer PC) ]
                                             │
                      ┌──────────────────────┴──────────────────────┐
                      │                                             │
                Port 8085 (HTTP)                              Port 5435 (TCP)
                      │                                             │
                      ▼                                             ▼
           ┌─────────────────────┐                       ┌─────────────────────┐
           │    order-service    │                       │       order-db      │
           │     (Spring Boot)   │                       │    (PostgreSQL 17)  │
           └──────────┬──────────┘                       └──────────▲──────────┘
                      │                                             │
                      │         Docker Bridge Network               │
                      │        "nexacommerce-network"               │
                      └─────────────────────────────────────────────┘
                             JDBC: order-db:5432/order_db
```

### Multi-Stage Dockerfile Strategy

`Dockerfile` uses **Eclipse Temurin 25 Alpine**:

1. **Stage 1 (Builder):**
   * Base: `eclipse-temurin:25-jdk-alpine`
   * Copies `mvnw` and `pom.xml`, normalizes CRLF to LF, and runs `./mvnw dependency:go-offline -B` (layer caching).
   * Compiles source code with `./mvnw clean package -DskipTests -B`.
2. **Stage 2 (Runner):**
   * Base: `eclipse-temurin:25-jre-alpine` (lightweight production runtime, ~150MB).
   * Creates a dedicated non-root user (`appuser:appgroup`) for security.
   * Copies only the final `app.jar` from Stage 1.
   * Runs with `ENTRYPOINT ["java", "-jar", "app.jar"]`.

---

### Docker Compose Architecture

`docker-compose.yml` provides:
* **Port Conflict Prevention:** Maps internal PostgreSQL `5432` to host **`5435`** (preventing clashes with `product-db` on `5432`, `inventory-db` on `5433`, or `user-db` on `5434`).
* **Healthcheck Dependency:** `order-service` specifies `depends_on: order-db (condition: service_healthy)` with `pg_isready`.
* **Data Persistence:** Dedicated named volume `order_db_data`.

### Environment Configuration (`.env.example`)

```properties
# Application Configuration
SERVER_PORT=8085

# PostgreSQL Database Configuration
DB_HOST=order-db
DB_PORT=5432
HOST_DB_PORT=5435
DB_NAME=order_db
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
```

---

### Docker Commands & Execution Guide

#### 1. Build and start containers in the background:
```powershell
docker compose up -d --build
```

#### 2. Start existing containers in the background (Quick Start):
```powershell
docker compose up -d
```

#### 3. Check running containers and health status:
```powershell
docker compose ps
```

#### 4. View streaming application logs:
```powershell
docker compose logs -f order-service
```

#### 5. Stop containers gracefully:
```powershell
docker compose down
```

#### 6. Stop containers and reset database volume (Clean Reset):
```powershell
docker compose down -v
```

---

## 6. Testing Strategy & Execution

The service enforces automated test coverage across the two primary architectural tiers:

### 1. `OrderServiceTest` (Unit Tests)
* **File:** `src/test/java/com/nexacommerce/order_service/application/service/OrderServiceTest.java`
* **Technology:** JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
* **Coverage:** 9 isolated test cases verifying order calculations, entity saves, ID lookups, user filters, and status transitions without requiring a database.

### 2. `OrderControllerTest` (Web Slice Tests)
* **File:** `src/test/java/com/nexacommerce/order_service/presentation/controller/OrderControllerTest.java`
* **Technology:** Spring `@WebMvcTest(OrderController.class)` + `MockMvc` + `@MockitoBean`
* **Coverage:** 6 endpoint tests verifying HTTP 201/200/400 responses, JSON payloads, and `@Valid` constraints.

### Executing Tests

```powershell
# Run all Service and Controller tests
.\mvnw.cmd test "-Dtest=OrderServiceTest,OrderControllerTest"

# Run entire test suite
.\mvnw.cmd test
```
