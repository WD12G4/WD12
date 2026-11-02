# Multi-stage Dockerfile for Spring Boot (Java 21)
# 1. Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Gradle wrapper and configuration files for caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties* ./

# Convert Windows CRLF to UNIX LF (essential on Windows hosts) and set permissions
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code and package JAR
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# 2. Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Create logs directory with ownership for spring user
RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring:spring

COPY --from=builder --chown=spring:spring /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
