# NexaCommerce API Gateway - Technical Documentation

Public-release edition: architecture, configuration, containerization, testing and security notes for the NexaCommerce API Gateway.

---

## 1. Overview & Tech Stack

The **API Gateway** is the central reverse proxy and single entry point for client requests to the NexaCommerce microservices. It provides routing, request dispatching, protocol abstraction and centralized health monitoring. Together with the network setup described in Section 7, it is designed to keep internal services from being exposed directly to clients.

### Technology Stack

- **Language & Runtime:** Java 25 (Eclipse Temurin)
- **Framework:** Spring Boot 4.1.2 (Spring Framework 7.x)
- **Gateway Engine:** Spring Cloud Gateway Server WebMvc (Spring Cloud 2025.1.4)
- **Monitoring:** Spring Boot Actuator
- **Testing:** JUnit 5, Spring Boot Test (`@SpringBootTest`)
- **Containerization:** Docker (multi-stage build), Docker Compose
- **Networking:** Docker bridge network (`nexacommerce-network`)

---

## 2. Architecture & Design Patterns

The gateway implements the **API Gateway** and **Reverse Proxy** patterns. In a correctly configured production deployment, clients reach only the gateway; downstream services are not published to the host (see Section 7).

```
                      [ Clients / Frontend / Postman ]
                                     |
                                     |  Port 8080
                                     v
+---------------------------------------------------------------------------+
| NEXACOMMERCE API GATEWAY (Spring Cloud Gateway Server WebMvc)             |
|                                                                           |
|  /api/v1/products/**, /api/v1/categories/**  -> product-service:8082      |
|  /api/v1/inventory/**                        -> inventory-service:8083    |
|  /api/v1/users/**                            -> user-service:8084         |
|  /api/v1/orders/**                           -> order-service:8085        |
|  /actuator/health, /actuator/info            -> local health diagnostics  |
+---------------------------------------------------------------------------+
                                     |
                       Docker network: nexacommerce-network
          +--------------------+-----------+--------------------+
          |                    |                                |
   product-service      inventory-service                 order-service
      Port 8082             Port 8083                        Port 8085
                               |
                          user-service
                            Port 8084
```

### Routing Table

| Route ID | Path Predicate | Target (Docker) | Target (Local Dev) | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| `product-service` | `/api/v1/products/**`, `/api/v1/categories/**` | `http://product-service:8082` | `http://localhost:8082` | Product and category catalog |
| `inventory-service` | `/api/v1/inventory/**` | `http://inventory-service:8083` | `http://localhost:8083` | Stock levels and availability |
| `user-service` | `/api/v1/users/**` | `http://user-service:8084` | `http://localhost:8084` | User profiles, login, registration |
| `order-service` | `/api/v1/orders/**` | `http://order-service:8085` | `http://localhost:8085` | Order lifecycle and line calculations |
| `actuator` | `/actuator/**` | Local (`api-gateway`) | Local (`api-gateway`) | Health, liveness, readiness |

---

## 3. Configuration & Environment Variables

Real values live in `.env`, which is **never committed**. `.env.example` is the committed template and contains placeholders only.

| Variable | Default / Fallback | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | Port the gateway listens on |
| `PRODUCT_SERVICE_HOST` | `localhost` (local) / `product-service` (Docker) | Product Service host |
| `PRODUCT_SERVICE_PORT` | `8082` | Product Service port |
| `INVENTORY_SERVICE_HOST` | `localhost` (local) / `inventory-service` (Docker) | Inventory Service host |
| `INVENTORY_SERVICE_PORT` | `8083` | Inventory Service port |
| `USER_SERVICE_HOST` | `localhost` (local) / `user-service` (Docker) | User Service host |
| `USER_SERVICE_PORT` | `8084` | User Service port |
| `ORDER_SERVICE_HOST` | `localhost` (local) / `order-service` (Docker) | Order Service host |
| `ORDER_SERVICE_PORT` | `8085` | Order Service port |
| `HEALTH_DETAILS` | `when-authorized` (default) / `always` (local development only) | Actuator health detail exposure policy |

> **Warning:** Never set `HEALTH_DETAILS=always` in production. It exposes internal component details to anyone who can reach the endpoint.

### Setup

```bash
cp .env.example .env
# Edit .env with your local values. Never commit this file.
```

Make sure `.env` is listed in `.gitignore`.

---

## 4. Containerization & Docker Execution

The service uses a multi-stage Docker build to keep images small and run as a non-root user.

- **Stage 1 (Builder):** `eclipse-temurin:25-jdk-alpine`; caches dependencies (`dependency:go-offline`) and builds the JAR (`mvnw clean package -DskipTests`).
- **Stage 2 (Runner):** `eclipse-temurin:25-jre-alpine`; creates a non-root group and user (`appgroup:appuser`), copies the JAR and exposes port `8080`.

### Docker Compose Commands

Linux / macOS:

```bash
cd backend/api-gateway
docker compose up -d --build
```

Windows (PowerShell):

```powershell
cd "backend\api-gateway"
docker compose up -d --build
```

Other useful commands:

```bash
docker compose ps                      # status
docker compose logs -f api-gateway     # live logs
docker compose down                    # stop and remove
```

---

## 5. Local Development vs. Production Compose

To avoid exposing downstream services by accident, port mappings are separated by environment.

- **Base `docker-compose.yml`:** no `ports:` for downstream services. Only the gateway publishes port `8080`.
- **`docker-compose.override.yml` (local development):** loaded automatically by `docker compose up`; publishes downstream ports so each service can be tested directly (for example with Postman).

```yaml
# docker-compose.override.yml (local development only)
services:
  product-service:
    ports:
      - "${SERVER_PORT:-8082}:${SERVER_PORT:-8082}"
```

In production, run only the base file so development ports are never published:

```bash
docker compose -f docker-compose.yml up -d
```

> **Note:** `expose:` is documentation only and does not restrict access. Services on the same Docker network can reach each other regardless. What keeps a service off the host is the absence of `ports:`.

---

## 6. API Testing & Health Checks

### Actuator Health

- **Method / URL:** `GET http://localhost:8080/actuator/health`
- **Expected with `HEALTH_DETAILS=always`** (development only):

```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP", "details": { "...": "..." } },
    "livenessState": { "status": "UP" },
    "ping": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

In production (`when-authorized`), an unauthenticated caller sees only `{"status": "UP"}`.

### Route Dispatch Verification

Once downstream services are running on `nexacommerce-network`:

```http
GET http://localhost:8080/api/v1/products
GET http://localhost:8080/api/v1/categories
GET http://localhost:8080/api/v1/orders

POST http://localhost:8080/api/v1/users/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "<your-password>"
}
```

---

## 7. Security Considerations

### Current Security Posture

- **Network isolation (production):** In production deployments, downstream services must not publish ports to the host, so they are reachable only through the gateway on `nexacommerce-network`. Local development setups may publish ports for direct testing (Section 5).
- **Non-root container:** The runtime image runs as `appuser:appgroup`, not `root`.
- **Secrets management:** Real configuration lives in `.env`, which is excluded from version control. Only `.env.example` (placeholders, no secrets) is committed.
- **Minimal actuator exposure:** Only `health` and `info` are exposed. Sensitive endpoints (`env`, `beans`, `heapdump`, `configprops`) must remain disabled.

### Production Hardening Checklist

The items below are recommendations. Update the Status column to reflect what is actually implemented in your deployment.

| Area | Recommendation | Status |
| :--- | :--- | :--- |
| Health details | Set `HEALTH_DETAILS=when-authorized`. Never use `always` in production. | Default |
| TLS/HTTPS | Terminate TLS at a reverse proxy or load balancer in front of the gateway. | Recommended |
| Authentication | Validate JWTs at the gateway before forwarding requests to protected routes. | Planned |
| Rate limiting | Apply per-client rate limits, especially on login and public endpoints. | Planned |
| CORS | Restrict allowed origins to known frontend domains; avoid wildcard `*`. | Planned |
| Actuator access | Restrict `/actuator/**` to internal networks or authenticated operators. | Recommended |
| Dependencies | Keep base images and Maven dependencies updated; scan with Trivy or Dependabot. | Recommended |
