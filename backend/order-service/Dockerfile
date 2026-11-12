# Stage 1: Build stage (JDK + Maven dependencies & compilation)
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and POM configuration
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Normalize line endings for Windows compatibility and grant execute permissions
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies first to leverage Docker layer caching
RUN ./mvnw dependency:go-offline -B

# Copy application source code
COPY src ./src

# Build the executable Spring Boot JAR file (skipping unit tests during container build)
RUN ./mvnw clean package -DskipTests -B


# Stage 2: Runtime stage (Lightweight JRE for Production)
FROM eclipse-temurin:25-jre-alpine AS runner

WORKDIR /app

# Security: Create and run under a non-root system user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy only the compiled JAR artifact from the builder stage
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

# Expose Order Service default port
EXPOSE 8085

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
