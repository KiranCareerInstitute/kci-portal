# Use OpenJDK 17 as base
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy built JAR (built externally or via Docker multi-stage in future)
COPY target/portal-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8082

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]