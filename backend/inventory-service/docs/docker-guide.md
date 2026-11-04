# NexaCommerce Inventory Service - Docker & Docker Compose Guide

Comprehensive reference guide for containerizing and orchestrating the **Inventory Service** and **PostgreSQL Database** using Docker, Multi-stage Dockerfiles, and Docker Compose.

---

## 1. Architecture & Container Topology

The Inventory Service containerization is structured around an isolated Docker bridge network (`nexacommerce-network`) enabling secure inter-container communication without exposing internal database ports unnecessarily.

```
                              [ Host Machine (Developer PC) ]
                                            │
                     ┌──────────────────────┴──────────────────────┐
                     │                                             │
               Port 8083 (HTTP)                              Port 5433 (TCP)
                     │                                             │
                     ▼                                             ▼
          ┌─────────────────────┐                       ┌─────────────────────┐
          │   inventory-service │                       │     inventory-db    │
          │     (Spring Boot)   │                       │    (PostgreSQL 17)  │
          └──────────┬──────────┘                       └──────────▲──────────┘
                     │                                             │
                     │         Docker Bridge Network               │
                     │        "nexacommerce-network"               │
                     └─────────────────────────────────────────────┘
                            JDBC: inventory-db:5432
```

### Port Mapping Strategy

| Container | Internal Port | Host Port | Purpose |
| :--- | :--- | :--- | :--- |
| **`inventory-service`** | `8083` | `8083` | Exposes REST API endpoints (`/api/v1/inventory`) to Postman, Frontend, and other microservices. |
| **`inventory-db`** | `5432` | `5433` | Host port **`5433`** is used to avoid port collision with local Windows PostgreSQL or sibling services (`product-db` on `5432`). |

---

## 2. Multi-Stage Production Dockerfile

The service uses a **Multi-Stage Dockerfile** (`backend/inventory-service/Dockerfile`) based on **Eclipse Temurin 25 Alpine**.

```
[ Stage 1: Builder ]                     [ Stage 2: Runner ]
- eclipse-temurin:25-jdk-alpine          - eclipse-temurin:25-jre-alpine
- Maven Wrapper + pom.xml                - Non-root user: appuser (UID/GID)
- Dependency layer caching               - Copies ONLY app.jar (~40MB)
- Compile & package (-DskipTests)        - Discards compilers & source code
        │                                         │
        └───> Outputs: app.jar ───────────────────┘ (Final Image Size: ~200MB)
```

### Key Optimizations:

1. **Docker Layer Caching:**
   * `pom.xml` and `.mvn/` are copied first, followed by `./mvnw dependency:go-offline`.
   * Dependencies are cached in a Docker layer. Re-building after changing Java code takes only **5–10 seconds** because dependencies are not re-downloaded.
2. **Windows CRLF Normalization:**
   * `RUN sed -i 's/\r$//' mvnw && chmod +x mvnw`
   * Strips Windows carriage-return line endings (`\r`) so the wrapper executes seamlessly on Alpine Linux.
3. **Security Hardening (Non-Root User):**
   * Uses `RUN addgroup -S appgroup && adduser -S appuser -G appgroup`.
   * Container runs under restricted `appuser` permissions rather than default `root`.
4. **Minimal Attack Surface:**
   * The final image contains only the JRE (Java Runtime Environment) and the compiled JAR. Maven, JDK compilers, and source code are discarded.

---

## 3. Docker Compose Configuration Deep Dive

File location: `backend/inventory-service/docker-compose.yml`

```yaml
services:
  inventory-db:
    image: postgres:17-alpine
    container_name: inventory-db
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME:-inventory_db}
      POSTGRES_USER: ${DB_USERNAME:-your_database_username_here}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-your_database_password_here}
    ports:
      - "5433:5432" # Host:Container (Avoids 5432 collision)
    volumes:
      - inventory_db_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-postgres} -d ${DB_NAME:-inventory_db}"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - nexacommerce-network

  inventory-service:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: inventory-service
    restart: unless-stopped
    ports:
      - "8083:8083"
    environment:
      SERVER_PORT: 8083
      DB_URL: jdbc:postgresql://inventory-db:5432/${DB_NAME:-inventory_db}
      DB_USERNAME: ${DB_USERNAME:-your_database_username_here}
      DB_PASSWORD: ${DB_PASSWORD:-your_database_password_here}
    depends_on:
      inventory-db:
        condition: service_healthy
    networks:
      - nexacommerce-network

volumes:
  inventory_db_data:
    driver: local

networks:
  nexacommerce-network:
    driver: bridge
```

### Critical Configuration Elements:

* **`depends_on -> condition: service_healthy`:**
  Spring Boot will not start until PostgreSQL has passed its `pg_isready` health check. This eliminates `Connection refused` race conditions on startup.
* **Service DNS Resolution:**
  Inside the Docker bridge network, containers communicate using service names as hostnames (`jdbc:postgresql://inventory-db:5432/...`).
* **Volume Persistence (`inventory_db_data`):**
  PostgreSQL data is stored in a named Docker volume on the host. Stopping or restarting containers does **not** erase database records.

---

## 4. Operational Modes (Workflows)

### Mode A: Full Container Mode (Production / CI / Full Stack)

Both the Database and the Spring Boot application run inside Docker containers.

```powershell
# 1. Build and start both containers in the background:
docker compose up -d --build

# 2. View streaming logs from the application:
docker compose logs -f inventory-service

# 3. Check status of all containers:
docker compose ps

# 4. Stop containers gracefully:
docker compose down
```

**Access URLs:**
* REST API: `http://localhost:8083/api/v1/inventory`
* Health Check: `http://localhost:8083/actuator/health`

---

### Mode B: Hybrid Development Mode (Recommended for Daily Coding)

The Database runs in a Docker container, while Spring Boot runs directly on your PC (via IntelliJ IDEA or CLI) for instant hot-reload and breakpoint debugging.

```powershell
# 1. Start ONLY the PostgreSQL database:
docker compose up -d inventory-db

# 2. Run the application from IntelliJ IDEA (Play ▶️ button)
# OR from PowerShell:
.\mvnw spring-boot:run
```

**How `.env` is loaded automatically:**
In `src/main/resources/application.properties`, we configured:
```properties
spring.config.import=optional:file:.env[.properties]
```
When running locally, Spring Boot automatically imports `.env`:
```properties
DB_URL=jdbc:postgresql://localhost:5433/inventory_db
DB_USERNAME=your_database_username_here
DB_PASSWORD=your_database_password_here
```
No manual environment variables or IntelliJ configurations are needed.

```powershell
# 3. Stop the database when finished:
docker compose stop inventory-db
```

---

## 5. Database Management & Inspection

### Connecting via CLI (`psql`)

Run a quick query from the terminal without entering interactive mode:
```powershell
docker exec inventory-db psql -U postgres -d inventory_db -c "SELECT * FROM inventory;"
```

Enter interactive `psql` shell:
```powershell
docker exec -it inventory-db psql -U postgres -d inventory_db
```
* `\dt` — List all tables
* `\d inventory` — Describe `inventory` table schema
* `SELECT * FROM inventory;` — View all rows
* `\q` — Exit shell

### Connecting via pgAdmin 4 / DBeaver / Database GUI

* **Host:** `localhost`
* **Port:** **`5433`** *(Always use 5433 for Docker)*
* **Database:** `inventory_db`
* **Username:** `your_database_username_here`
* **Password:** `your_database_password_here`



## 6. Quick Reference Command Cheat Sheet

```powershell
# Start all containers in background
docker compose up -d

# Re-build containers after code modifications
docker compose up -d --build

# Start only the database
docker compose up -d inventory-db

# Check status of containers
docker compose ps

# View live logs of a specific container
docker compose logs -f inventory-service
docker compose logs -f inventory-db

# Stop containers without losing data
docker compose stop

# Stop containers and remove network
docker compose down

# Stop containers and WIPE all database data
docker compose down -v

# Inspect resource usage (CPU/RAM)
docker stats
```
