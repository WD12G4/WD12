# ==============================================================================
# Multi-stage Dockerfile for Spring Boot (Java 21) - Projet Terrain
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Étape de Build (Temurin JDK 21 Alpine)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Arguments de proxy (transmis par la CI/CD pour l'environnement ETNC)
ARG HTTP_PROXY=""
ARG HTTPS_PROXY=""
ARG NO_PROXY=""

ENV http_proxy=${HTTP_PROXY} \
    https_proxy=${HTTPS_PROXY} \
    HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY} \
    no_proxy=${NO_PROXY} \
    NO_PROXY=${NO_PROXY}

# Copie des scripts Gradle pour bénéficier du cache Docker
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties* ./

# Normalisation CRLF -> LF (indispensable sur hôtes Windows) et droits d'exécution
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Configuration dynamique du proxy pour Gradle
RUN if [ -n "$HTTP_PROXY" ]; then \
      PROXY_HOST=$(echo "$HTTP_PROXY" | sed -e 's~^https\?://~~' -e 's~:.*~~'); \
      PROXY_PORT=$(echo "$HTTP_PROXY" | sed -e 's~.*:~~' -e 's~/.*~~'); \
      NON_PROXY=$(echo "${NO_PROXY:-localhost|127.0.0.1}" | tr ',' '|'); \
      mkdir -p /root/.gradle && \
      printf "systemProp.http.proxyHost=%s\nsystemProp.http.proxyPort=%s\nsystemProp.https.proxyHost=%s\nsystemProp.https.proxyPort=%s\nsystemProp.http.nonProxyHosts=%s\n" \
        "$PROXY_HOST" "$PROXY_PORT" "$PROXY_HOST" "$PROXY_PORT" "$NON_PROXY" >> /root/.gradle/gradle.properties; \
    fi

# Téléchargement et mise en cache des dépendances Gradle
RUN if [ -n "$HTTP_PROXY" ]; then \
      PROXY_HOST=$(echo "$HTTP_PROXY" | sed -e 's~^https\?://~~' -e 's~:.*~~'); \
      PROXY_PORT=$(echo "$HTTP_PROXY" | sed -e 's~.*:~~' -e 's~/.*~~'); \
      NON_PROXY=$(echo "${NO_PROXY:-localhost|127.0.0.1}" | tr ',' '|'); \
      export GRADLE_OPTS="-Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT -Dhttp.nonProxyHosts=\"$NON_PROXY\""; \
      export JAVA_OPTS="$GRADLE_OPTS"; \
    fi && \
    ./gradlew dependencies --no-daemon

# Copie des sources et packaging de l'exécutable JAR
COPY src src
RUN if [ -n "$HTTP_PROXY" ]; then \
      PROXY_HOST=$(echo "$HTTP_PROXY" | sed -e 's~^https\?://~~' -e 's~:.*~~'); \
      PROXY_PORT=$(echo "$HTTP_PROXY" | sed -e 's~.*:~~' -e 's~/.*~~'); \
      NON_PROXY=$(echo "${NO_PROXY:-localhost|127.0.0.1}" | tr ',' '|'); \
      export GRADLE_OPTS="-Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT -Dhttps.proxyHost=$PROXY_HOST -Dhttps.proxyPort=$PROXY_PORT -Dhttp.nonProxyHosts=\"$NON_PROXY\""; \
      export JAVA_OPTS="$GRADLE_OPTS"; \
    fi && \
    ./gradlew bootJar --no-daemon -x test

# ------------------------------------------------------------------------------
# 2. Étape d'Exécution (Temurin JRE 21 Alpine)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Création d'un utilisateur non-root pour la sécurité
RUN addgroup -S spring && adduser -S spring -G spring

# Répertoire de logs avec permissions pour l'utilisateur spring
RUN mkdir -p /app/logs && chown -R spring:spring /app

USER spring:spring

# Copie du jar compilé
COPY --from=builder --chown=spring:spring /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
