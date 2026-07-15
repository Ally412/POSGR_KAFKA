# syntax=docker/dockerfile:1
# Multi-stage build for the shelter Spring Boot app.
#   Stage 1 "build"   → full JDK, compiles the jar
#   Stage 2 "runtime" → slim JRE, runs the jar (the final, shipped image)
# Build:  docker build -t shelter:local .
# ---------------------------------------------------------------------------

# ---- Stage 1: build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy ONLY the build definition first, so the dependency download layer
# is cached and re-used until build.gradle/settings.gradle actually change.
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

# Warm the dependency cache as its own layer (skipped on rebuilds when deps are unchanged).
RUN ./gradlew dependencies --no-daemon

# Now copy the source and build the executable jar.
COPY src ./src
RUN ./gradlew bootJar --no-daemon

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user (security: a container breakout can't be root).
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy ONLY the finished jar from the build stage — none of the JDK, source, or Gradle.
COPY --from=build /app/build/libs/*.jar app.jar

# EXPOSE documents the port (not a real mapping); ENTRYPOINT is what runs at container start
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
