# Build stage
FROM eclipse-temurin:21-jdk as builder
WORKDIR /workspace
COPY . .
RUN ./gradlew clean build -x test

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/app.jar"]