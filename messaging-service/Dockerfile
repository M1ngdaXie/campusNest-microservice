# Multi-stage build for smaller image size
# Use non-alpine version for better ARM64 (Apple Silicon) support
FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy common-library and build it first
COPY common-library ./common-library

# Build and install common-library to local Maven repo
WORKDIR /app/common-library
RUN mvn clean install -DskipTests

# Copy service pom.xml
WORKDIR /app
COPY messaging-service/pom.xml ./messaging-service/

# Download dependencies (cached layer)
WORKDIR /app/messaging-service
RUN mvn dependency:go-offline -B || true

# Copy service source code
COPY messaging-service/src ./src

# Build the service
RUN mvn clean package -DskipTests

# Runtime stage - JRE image with ARM64 support
FROM eclipse-temurin:17-jre

# Add metadata
LABEL maintainer="xiemingda2020@gmail.com"
LABEL description="Messaging Service for CampusNest Platform"

# Create non-root user for security (Debian-based commands instead of Alpine)
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/messaging-service/target/*.jar app.jar

# Expose port
EXPOSE 8083

# Health check (use curl instead of wget for Debian base)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]