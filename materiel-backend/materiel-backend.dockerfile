# Multi-stage Dockerfile for Spring Boot (Java 21)
# 1. Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Proxy arguments (empty by default for direct internet access, passed via CI/CD for ETNC)
ARG HTTP_PROXY=""
ARG HTTPS_PROXY=""
ARG NO_PROXY=""

ENV http_proxy=${HTTP_PROXY} \
    https_proxy=${HTTPS_PROXY} \
    HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY} \
    no_proxy=${NO_PROXY} \
    NO_PROXY=${NO_PROXY}

# Copy Gradle wrapper and configuration files for caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties* ./

# Convert Windows CRLF to UNIX LF (essential on Windows hosts) and set permissions
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Configure proxy dynamically: if HTTP_PROXY is provided, configure Gradle properties & options
RUN if [ -n "$HTTP_PROXY" ]; then \
      PROXY_HOST=$(echo "$HTTP_PROXY" | sed -e 's~^https\?://~~' -e 's~:.*~~'); \
      PROXY_PORT=$(echo "$HTTP_PROXY" | sed -e 's~.*:~~' -e 's~/.*~~'); \
      NON_PROXY=$(echo "${NO_PROXY:-localhost|127.0.0.1}" | tr ',' '|'); \
      mkdir -p /root/.gradle && \
      printf "systemProp.http.proxyHost=%s\nsystemProp.http.proxyPort=%s\nsystemProp.https.proxyHost=%s\nsystemProp.https.proxyPort=%s\nsystemProp.http.nonProxyHosts=%s\n" \
        "$PROXY_HOST" "$PROXY_PORT" "$PROXY_HOST" "$PROXY_PORT" "$NON_PROXY" >> /root/.gradle/gradle.properties; \
    fi

# Download dependencies (cached layer)
RUN if [ -n "$HTTP_PROXY" ]; then \
      PROXY_HOST=$(echo "$HTTP_PROXY" | sed -e 's~^https\?://~~' -e 's~:.*~~'); \
      PROXY_PORT=$(echo "$HTTP_PROXY" | sed -e 's~.*:~~' -e 's~/.*~~'); \
      NON_PROXY=$(echo "${NO_PROXY:-localhost|127.0.0.1}" | tr ',' '|'); \
      export GRADLE_OPTS="-Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT -Dhttp.nonProxyHosts=\"$NON_PROXY\""; \
      export JAVA_OPTS="$GRADLE_OPTS"; \
    fi && \
    ./gradlew dependencies --no-daemon

# Copy source code and package JAR
COPY src src
RUN if [ -n "$HTTP_PROXY" ]; then \
      PROXY_HOST=$(echo "$HTTP_PROXY" | sed -e 's~^https\?://~~' -e 's~:.*~~'); \
      PROXY_PORT=$(echo "$HTTP_PROXY" | sed -e 's~.*:~~' -e 's~/.*~~'); \
      NON_PROXY=$(echo "${NO_PROXY:-localhost|127.0.0.1}" | tr ',' '|'); \
      export GRADLE_OPTS="-Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT -Dhttp.nonProxyHosts=\"$NON_PROXY\""; \
      export JAVA_OPTS="$GRADLE_OPTS"; \
    fi && \
    ./gradlew bootJar --no-daemon -x test

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
