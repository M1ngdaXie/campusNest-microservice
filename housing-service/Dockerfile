# Multi-stage build for smaller image size
FROM maven:3.9-eclipse-temurin-17 AS build

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
LABEL description="Housing Service for CampusNest Platform"

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8082

# Run the application with optimized JVM settings
# -Xms: Initial heap size (256MB)
# -Xmx: Maximum heap size (512MB - leaves room for non-heap memory)
# -XX:+UseG1GC: Use G1 garbage collector (good for microservices)
# -XX:MaxGCPauseMillis: Target max GC pause time
ENTRYPOINT ["java", \
    "-Xms256m", \
    "-Xmx512m", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-jar", "app.jar"]