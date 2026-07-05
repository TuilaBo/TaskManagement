# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml first (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from build stage
COPY --from=builder /app/target/taskmanagement-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run with env vars (must be provided)
ENTRYPOINT ["sh", "-c", "java -jar app.jar \
    --spring.config.additional-location=file:/app/config/application-prod.properties \
    --spring.datasource.url=${DB_URL} \
    --spring.datasource.username=${DB_USERNAME} \
    --spring.datasource.password=${DB_PASSWORD} \
    --jwt.secret=${JWT_SECRET} \
    --spring.mail.username=${MAIL_USERNAME} \
    --spring.mail.password=${MAIL_PASSWORD} \
    --cloudinary.api-secret=${CLOUDINARY_API_SECRET} \
    --cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME} \
    --cloudinary.api-key=${CLOUDINARY_API_KEY} \
    --cors.allowed-origins=${CORS_ORIGINS}"]
