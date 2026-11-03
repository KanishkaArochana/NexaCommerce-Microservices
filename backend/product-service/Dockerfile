# Stage 1: Build with JDK & Maven Wrapper

FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Copy Maven Wrapper and POM configuration
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Convert Windows line endings (CRLF to LF) and grant execution permission
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Download dependencies (Docker layer cache)
RUN ./mvnw dependency:go-offline -B

# Copy project source and package the jar (skipping unit tests during image build)
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal & Secure Runtime (JRE)

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Run as an unprivileged non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the generated Spring Boot fat JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the product-service application port
EXPOSE 8082

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
