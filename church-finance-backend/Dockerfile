# syntax=docker/dockerfile:1.7

# --- Build stage -------------------------------------------------------------
FROM gradle:9.0.0-jdk21-alpine AS build
WORKDIR /workspace

# Cache dependencies first for faster incremental builds.
COPY settings.gradle.kts build.gradle.kts gradle.properties* ./
COPY gradle ./gradle
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN gradle --no-daemon clean bootJar -x test

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
