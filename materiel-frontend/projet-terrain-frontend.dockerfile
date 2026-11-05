# ==============================================================================
# Multi-stage Dockerfile for Vue 3 + Vite Frontend - Projet Terrain
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Étape de Build (Node 24 Alpine)
# ------------------------------------------------------------------------------
FROM node:24-alpine AS builder
WORKDIR /app

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

# Copie des manifestes de dépendances
COPY package*.json ./

# Configuration dynamique du proxy npm si présent et installation propre
RUN if [ -n "$HTTP_PROXY" ]; then \
      npm config set proxy "$HTTP_PROXY" && \
      npm config set https-proxy "$HTTPS_PROXY"; \
    fi && \
    npm ci

# Copie des sources et compilation des assets statiques (Vite)
COPY . .
RUN npm run build

# ------------------------------------------------------------------------------
# 2. Étape d'Exécution (Nginx Alpine)
# ------------------------------------------------------------------------------
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Nettoyage des assets par défaut de Nginx
RUN rm -rf ./*

# Récupération du build de l'étape précédente
COPY --from=builder /app/dist .

# Configuration Nginx (SPA + proxy inverse /api vers le backend)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
