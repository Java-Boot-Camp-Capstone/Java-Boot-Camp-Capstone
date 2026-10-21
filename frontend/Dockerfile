# syntax=docker/dockerfile:1
# Northstar CRM - Angular agent UI image
#
# Local (docker compose): stage 1 builds the Angular app, stage 2 serves it with nginx.
# CI (GitHub Actions):    the "frontend" job already ran `ng build`, so it replaces stage 1 with
#                         its tested output (build-contexts: build=frontend/.ci) - no second build.

# ---------- Stage 1: build (skipped in CI) ----------
FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npx ng build --configuration production \
 && mkdir -p /out \
 && if [ -d dist/*/browser ]; then cp -r dist/*/browser/. /out/; else cp -r dist/*/. /out/; fi

# ---------- Stage 2: runtime ----------
FROM nginxinc/nginx-unprivileged:1.30-alpine
LABEL org.opencontainers.image.title="northstar-crm-frontend" \
      org.opencontainers.image.description="Northstar CRM Angular agent UI" \
      org.opencontainers.image.source="https://github.com/Java-Boot-Camp-Capstone/Java-Boot-Camp-Capstone"

# Rendered with envsubst at container start (nginx official entrypoint)
ENV BACKEND_URL=http://backend:8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /out /usr/share/nginx/html

EXPOSE 8080
