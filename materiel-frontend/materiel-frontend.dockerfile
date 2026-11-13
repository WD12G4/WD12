# Multi-stage Dockerfile for Vue 3 + Vite Frontend
# 1. Build stage
FROM node:24-alpine AS builder
WORKDIR /app

# Proxy arguments (empty by default, passed via CI/CD for ETNC environment)
ARG HTTP_PROXY=""
ARG HTTPS_PROXY=""
ARG NO_PROXY=""

ENV http_proxy=${HTTP_PROXY} \
    https_proxy=${HTTPS_PROXY} \
    HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY} \
    no_proxy=${NO_PROXY} \
    NO_PROXY=${NO_PROXY}

# Copy dependency definition files (and the DSFR license consent required by @gouvfr/dsfr's install)
COPY package*.json .dsfr.yml ./

# Configure npm proxy dynamically if provided and install dependencies
RUN if [ -n "$HTTP_PROXY" ]; then \
      npm config set proxy "$HTTP_PROXY" && \
      npm config set https-proxy "$HTTPS_PROXY"; \
    fi && \
    npm ci

# Copy source code and build production assets
COPY . .
RUN npm run build

# 2. Production stage (Nginx + OpenSSL pour HTTPS)
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Install openssl for self-signed certificates generation and remove default static assets
RUN apk add --no-cache openssl && rm -rf ./*

# Copy built assets from builder stage
COPY --from=builder /app/dist .

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy entrypoint script for automatic SSL generation
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80 443

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
