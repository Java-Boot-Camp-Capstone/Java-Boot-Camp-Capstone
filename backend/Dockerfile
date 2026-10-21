# syntax=docker/dockerfile:1
# Northstar CRM - Spring Boot API image
#
# Local (docker compose): stage 1 compiles the jar with Maven, stage 2 runs it on a slim JRE.
# CI (GitHub Actions):    the "backend" job already ran `mvn verify`, so it replaces stage 1 with
#                         the tested jar (build-contexts: build=backend/.ci) - no second compile.

# ---------- Stage 1: build (skipped in CI) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests \
 && mkdir -p /out && cp target/*.jar /out/app.jar

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.title="northstar-crm-backend" \
      org.opencontainers.image.description="Northstar CRM Spring Boot REST API" \
      org.opencontainers.image.source="https://github.com/Java-Boot-Camp-Capstone/Java-Boot-Camp-Capstone"

# apk upgrade pulls patched OS packages released after the base image was built (Trivy findings).
# Numeric UID/GID so Kubernetes `runAsNonRoot: true` can verify the user is not root.
RUN apk upgrade --no-cache \
 && addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=build --chown=app:app /out/app.jar app.jar
USER 10001:10001

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# Readiness signal for plain `docker run` (k3s uses its own probes on the same endpoint)
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health/readiness | grep -q UP || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
