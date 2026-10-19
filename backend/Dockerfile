# =============================================================================
# Multi-stage build for the Spring Boot API.
# Stage 1 builds the fat jar with Maven + JDK 21; stage 2 ships a slim JRE.
# =============================================================================

# --- Build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies: copy the POM first and warm the local repository.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Compile, run tests are skipped here (CI runs them separately).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as a non-root user.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/task-management-backend.jar app.jar
USER app

# The platform injects $PORT at runtime (see application.yml).
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
