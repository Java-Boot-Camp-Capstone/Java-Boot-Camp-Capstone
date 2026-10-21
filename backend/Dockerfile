# syntax=docker/dockerfile:1
# Northstar CRM - Spring Boot API image
# Multi-stage: build with Maven + JDK 21, run on a slim JRE 21 as a non-root user.

# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in the GitHub Actions "backend" job; skip here so the image build stays fast.
RUN mvn -B -q package -DskipTests \
 && cp target/*.jar /workspace/app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.title="northstar-crm-backend" \
      org.opencontainers.image.description="Northstar CRM Spring Boot REST API" \
      org.opencontainers.image.source="https://github.com/Java-Boot-Camp-Capstone/Java-Boot-Camp-Capstone"

# apk upgrade pulls patched OS packages released after the base image was built (Trivy findings).
# Numeric UID/GID so Kubernetes `runAsNonRoot: true` can verify the user is not root.
RUN apk upgrade --no-cache \
 && addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER 10001:10001

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
# Readiness signal for plain `docker run` (k3s uses its own probes on the same endpoint)
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health/readiness | grep -q UP || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
