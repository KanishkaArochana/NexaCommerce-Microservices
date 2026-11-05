# NexaCommerce Inventory Service - Complete Docker Documentation

An exhaustive, in-depth architectural and operational guide to **Docker containerization**, covering core concepts, the multi-stage Dockerfile anatomy, image layers, security hardening, container lifecycle management, storage, networking, and production practices.

---

## 1. Fundamental Docker Concepts

### 1.1. Virtual Machines vs. Docker Containers

Traditional Virtual Machines (VMs) virtualize physical hardware, requiring a complete guest operating system (kernel, binaries, drivers) for each application. 

Docker operates at the **operating system level**, virtualizing only the user space. Containers share the host Linux kernel (via WSL2 on Windows), making them lightweight, instantaneous to start, and resource-efficient.

```
       [ Virtual Machines (Heavy: GBs) ]                [ Docker Containers (Light: MBs) ]
┌─────────────────────────────────────────────┐  ┌─────────────────────────────────────────────┐
│ App 1 (JAR)  │ App 2 (JAR)  │ App 3 (DB)    │  │ App 1 (JAR)  │ App 2 (JAR)  │ App 3 (DB)    │
├──────────────┼──────────────┼───────────────┤  ├──────────────┼──────────────┼───────────────┤
│ Bins / Libs  │ Bins / Libs  │ Bins / Libs   │  │ Bins / Libs  │ Bins / Libs  │ Bins / Libs   │
├──────────────┼──────────────┼───────────────┤  └──────────────┴──────────────┴───────────────┘
│ Guest OS     │ Guest OS     │ Guest OS      │  ┌─────────────────────────────────────────────┐
├──────────────┴──────────────┴───────────────┤  │            Docker Engine (runc)             │
│            Hypervisor (Type 1/2)            │  ├─────────────────────────────────────────────┤
├─────────────────────────────────────────────┤  │          Host Operating System (Kernel)     │
│             Host Hardware                   │  ├─────────────────────────────────────────────┤
└─────────────────────────────────────────────┘  │                 Host Hardware               │
                                                 └─────────────────────────────────────────────┘
```

### 1.2. Docker Architecture Components

* **Docker Daemon (`dockerd`):** Persistent background process managing Docker objects (images, containers, networks, volumes). On Windows, it runs inside the lightweight WSL2 Linux subsystem (`dockerDesktopLinuxEngine`).
* **Docker Client (`docker` CLI):** The command-line interface that communicates with the daemon over local IPC / named pipes (`//./pipe/dockerDesktopLinuxEngine`) or Unix domain sockets.
* **Docker Registry (Docker Hub):** Public or private registry containing pre-built, versioned base images (e.g., `postgres`, `eclipse-temurin`).

### 1.3. Image vs. Container

* **Docker Image:** An **immutable, read-only template** consisting of layered snapshots of libraries, configuration, and application binaries.
* **Docker Container:** A **stateful, runnable instance** of an image. When an image is run, Docker adds a thin, writable layer (Container Layer) on top of the immutable image layers via a Union File System (Overlay2).

---

## 2. Deep Dive: Multi-Stage Dockerfile Anatomy

The Inventory Service uses a **two-stage build** (`backend/inventory-service/Dockerfile`) designed to minimize final image size and enforce enterprise security.

```
[ Stage 1: builder ]
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests -B
        │
        │ Extract ONLY /app/target/*.jar
        ▼
[ Stage 2: runner ]
FROM eclipse-temurin:25-jre-alpine AS runner
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2.1. Stage 1: Build & Compilation (`builder`)

```dockerfile
FROM eclipse-temurin:25-jdk-alpine AS builder
```
* **Base Image:** Alpine Linux 3.x paired with the Eclipse Temurin OpenJDK 25.
* **Why Alpine?** Standard Ubuntu/Debian JDK images are ~500–800 MB. Alpine uses `musl libc` and `BusyBox`, bringing the base JDK down to ~150 MB.
* **`AS builder`:** Names this build stage so subsequent stages can reference artifacts from it.

```dockerfile
WORKDIR /app
```
* Sets the container execution directory to `/app`. All subsequent commands run relative to this path.

```dockerfile
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
```
* **Selective Copying for Layer Caching:** Only Maven wrapper configurations and `pom.xml` are copied first. Docker caches the results of each Dockerfile directive. If `pom.xml` hasn't changed, Docker reuses cached layers in future builds.

```dockerfile
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
```
* **Windows Line Ending Fix:** Windows editors save files with CRLF (`\r\n`). Linux requires LF (`\n`). This command strips `\r` from `mvnw` and grants execution permissions (`+x`) to prevent `bad interpreter: No such file or directory` errors.

```dockerfile
RUN ./mvnw dependency:go-offline -B
```
* Downloads all project dependencies into the local container `.m2` repository before compiling the source code.
* `-B` (`--batch-mode`): Runs non-interactively, suppressing interactive progress bars to keep Docker logs clean.

```dockerfile
COPY src ./src
RUN ./mvnw clean package -DskipTests -B
```
* Copies the application Java source code.
* Compiles, packages, and produces the executable fat JAR (`target/inventory-service-0.0.1-SNAPSHOT.jar`).
* `-DskipTests`: Unit tests are executed separately in the CI/CD pipeline (`mvn test`). Skipping tests during image build accelerates container build times.

---

### 2.2. Stage 2: Production Runtime (`runner`)

```dockerfile
FROM eclipse-temurin:25-jre-alpine AS runner
```
* **Runtime Only:** Uses the Java Runtime Environment (JRE) instead of the full JDK. Compilers (`javac`), debuggers, and build tools are completely absent, drastically reducing attack surface.

```dockerfile
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
```
* **Principle of Least Privilege (Non-Root User):**
  * By default, Docker containers run as the host's `root` user (`UID 0`). If an application suffers a remote code execution (RCE) vulnerability, an attacker gains root privileges inside the container and potentially the host kernel.
  * Creating a dedicated system group `appgroup` and system user `appuser` ensures the process runs without elevated capabilities.

```dockerfile
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar
```
* **Artifact Extraction:** Copies **only** the packaged JAR file from the `builder` stage.
* `--chown=appuser:appgroup`: Ensures the copied file is owned by `appuser`, allowing the process to read and execute it.
* **Resulting Footprint:** ~800 MB of compilers, caches, and intermediate files are discarded. The final image is only **~200 MB**.

```dockerfile
EXPOSE 8083
```
* Documentation directive indicating that the container listens on port 8083 at runtime.

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
```
* **Exec Form:** Executes `java -jar app.jar` directly as Process ID 1 (`PID 1`).
* **Why Exec Form `["..."]` over Shell Form `java -jar...`?**
  * Exec form passes OS signals (such as `SIGTERM` and `SIGINT`) directly to the Java process. When Docker runs `docker stop`, Spring Boot receives the signal and gracefully shuts down connections, completes active transactions, and closes thread pools.

---

## 3. Build Context & `.dockerignore`

When running `docker build`, the Docker CLI sends the directory contents (the **Build Context**) to the Docker Daemon.

The `.dockerignore` file (`backend/inventory-service/.dockerignore`) prevents unnecessary files from inflating the build context:

```dockerignore
# Build artifacts & target (Avoid uploading gigabytes of host build files)
target/
build/
out/

# Environment files and credentials (CRITICAL: Never bake secrets into images)
.env
.env.*

# IDE and editor settings
.idea/
.vscode/
*.iml
*.iws
*.ipr

# Version control
.git/
.gitignore

# Logs and temporary files
*.log
logs/
*.tmp
```

---

## 4. Docker CLI Command Mastery

### 4.1. Building Images

```powershell
# Build image with a specific tag:
docker build -t nexacommerce/inventory-service:1.0.0 .

# Build without using cached layers (force clean rebuild):
docker build --no-cache -t nexacommerce/inventory-service:latest .

# Inspect the layers of an image:
docker history nexacommerce/inventory-service:latest
```

### 4.2. Running & Managing Containers

```powershell
# Run container in background (detached mode) with port forwarding and custom name:
docker run -d \
  --name inventory-service \
  -p 8083:8083 \
  -e SERVER_PORT=8083 \
  -e DB_URL=jdbc:postgresql://inventory-db:5432/inventory_db \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=your_database_password_here \
  --network nexacommerce-network \
  nexacommerce/inventory-service:latest

# List all active containers:
docker ps

# List all containers (including stopped/exited ones):
docker ps -a

# View real-time container resource utilization (CPU, Memory, Network I/O):
docker stats inventory-service

# Stream live container logs:
docker logs -f inventory-service

# View the last 50 lines of logs:
docker logs --tail 50 inventory-service

# Inspect complete container configuration, IP, and mount metadata (JSON):
docker inspect inventory-service
```

### 4.3. Interactive Shell & Troubleshooting

```powershell
# Open an interactive shell inside the running container:
docker exec -it inventory-service sh

# Check the current active user inside the container (outputs: appuser):
docker exec inventory-service whoami

# Check Java version running inside container:
docker exec inventory-service java -version

# Copy a file from host to container:
docker cp ./test.txt inventory-service:/app/test.txt

# Copy a file from container to host:
docker cp inventory-service:/app/app.jar ./downloaded-app.jar
```

### 4.4. Container Lifecycle Controls

```powershell
# Gracefully stop container (sends SIGTERM, waits 10s, then SIGKILL):
docker stop inventory-service

# Immediately kill container (sends SIGKILL):
docker kill inventory-service

# Restart container:
docker restart inventory-service

# Remove stopped container:
docker rm inventory-service

# Force remove running container:
docker rm -f inventory-service
```

---

## 5. Docker Storage: Volumes vs. Bind Mounts

Containers have an ephemeral filesystem; data stored in the writable container layer is lost when the container is deleted. Docker provides two primary storage mechanisms:

```
[ Host Filesystem ]                           [ Container Filesystem ]
┌────────────────────────┐                    ┌──────────────────────┐
│ Docker Managed Storage │ ── Named Volume ─> │ /var/lib/postgresql/ │ (Database Data)
│ (e.g., \\wsl$\docker)  │                    │ data                 │
├────────────────────────┤                    ├──────────────────────┤
│ Arbitrary Host Path    │ ── Bind Mount ───> │ /app/logs            │ (Live Debugging)
│ (e.g., C:\Projects\..) │                    │                      │
└────────────────────────┘                    └──────────────────────┘
```

| Feature | Named Volumes (`volume`) | Bind Mounts (`bind`) |
| :--- | :--- | :--- |
| **Location** | Managed by Docker daemon (`/var/lib/docker/volumes/`). | Any directory on the host machine. |
| **Performance** | Native Linux filesystem performance (I/O optimized). | Slower on Windows host paths due to VM translation. |
| **Best Used For** | Databases (`inventory_db_data`), persistent storage. | Local code sharing, development hot reload. |

### Volume Management Commands

```powershell
# List all Docker volumes:
docker volume ls

# Inspect volume details (mount path, driver):
docker volume inspect inventory-service_inventory_db_data

# Remove an unused volume:
docker volume rm inventory-service_inventory_db_data

# Clean up all dangling/unused volumes:
docker volume prune
```

---

## 6. Docker Networking

Docker provides multiple network drivers:

1. **Bridge (`bridge`):** Default network driver. Creates an internal software bridge where containers communicate using internal IP addresses or container names.
2. **Host (`host`):** Removes network isolation between container and host. The container shares the host's networking namespace directly (Linux only).
3. **None (`none`):** Disables all external and internal networking for the container.
4. **Overlay (`overlay`):** Multi-host networking across multiple Docker daemons (used in Docker Swarm & Kubernetes).

### Service Discovery & DNS

In our user-defined bridge network (`nexacommerce-network`):
* Docker runs an embedded DNS server at `127.0.0.11`.
* Containers automatically resolve other container names as hostnames.
* For example, `inventory-service` connects to `jdbc:postgresql://inventory-db:5432/inventory_db`. Docker's internal DNS automatically routes `inventory-db` to the database container's private IP (`172.x.x.x`).

```powershell
# Create a user-defined bridge network:
docker network create --driver bridge nexacommerce-network

# List all networks:
docker network ls

# Inspect network and view connected containers with their internal IPs:
docker network inspect nexacommerce-network

# Disconnect a container from a network:
docker network disconnect nexacommerce-network inventory-service
```

---

## 7. Container Resource Optimization & JVM Memory

Java applications running inside containers must be aware of container cgroups limits.

Starting with Java 10+ (and fully refined in Java 25), the OpenJDK is container-aware via `-XX:+UseContainerSupport` (enabled by default). It automatically calculates heap sizes based on container memory limits rather than physical host RAM.

### Recommended Production Resource Constraints:

```powershell
docker run -d \
  --name inventory-service \
  --memory="512m" \
  --memory-reservation="256m" \
  --cpus="1.0" \
  -e JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0" \
  nexacommerce/inventory-service:latest
```

* `--memory="512m"`: Hard limit. If memory exceeds 512MB, the Linux OOM (Out Of Memory) killer halts the container.
* `-XX:MaxRAMPercentage=75.0`: Tells the JVM to use at most 75% of the container memory (~384MB) for heap, leaving 25% for Metaspace, threads, and off-heap memory.

---

## 8. Clean-Up & Maintenance Reference

To prevent Docker from consuming excessive disk space over time:

```powershell
# Remove all stopped containers:
docker container prune -f

# Remove all unused (dangling) images:
docker image prune -f

# Nuclear Clean-up: Removes all stopped containers, unused networks, and dangling images:
docker system prune -f

# Complete Deep Wipe: Removes all unused containers, images, networks, and volumes:
docker system prune -a --volumes -f
```
