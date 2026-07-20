FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy parent POM and shared-kernel first for caching
COPY pom.xml .
COPY shared-kernel ./shared-kernel
COPY transaction-service/pom.xml ./transaction-service/

# Download dependencies
RUN mvn dependency:go-offline -pl transaction-service

# Copy source and build
COPY transaction-service/src ./transaction-service/src
RUN mvn clean package -pl transaction-service -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Add non-root user
RUN addgroup -g 1001 appuser && adduser -D -u 1001 -G appuser appuser
USER appuser

# Copy JAR
COPY --from=build /app/transaction-service/target/*.jar app.jar

# Health check dependencies
RUN apk add --no-cache curl

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
