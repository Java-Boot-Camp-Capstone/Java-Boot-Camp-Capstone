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

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER app

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
