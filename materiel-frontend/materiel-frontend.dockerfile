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

# 2. Production stage (Nginx)
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets
RUN rm -rf ./*

# Copy built assets from builder stage
COPY --from=builder /app/dist .

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
