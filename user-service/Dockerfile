# Multi-stage build for smaller image size
# Use non-alpine version for better ARM64 (Apple Silicon) support
FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests

# Runtime stage - JRE image with ARM64 support
FROM eclipse-temurin:17-jre

# Add metadata
LABEL maintainer="xiemingda2020@gmail.com"
LABEL description="User Service for CampusNest Platform"

# Create non-root user for security (Debian-based commands instead of Alpine)
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8081

# Health check (use curl instead of wget for Debian base)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]