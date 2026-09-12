@echo off
REM ==============================================================================
REM MerchantRail - Zero-Install Local Run Script
REM Leverages your existing Docker Desktop and Node.js to build and run everything
REM ==============================================================================

echo ==============================================================================
echo   MerchantRail - Payment Switching ^& Batch Clearing Platform
echo   Starting complete system using existing Docker environment...
echo ==============================================================================

echo [1/3] Starting Infrastructure (PostgreSQL, Redis, Kafka, Prometheus)...
docker compose up -d postgres redis kafka prometheus
if %errorlevel% neq 0 (
    echo Error starting Docker infrastructure. Make sure Docker Desktop is running.
    exit /b %errorlevel%
)

echo.
echo [2/3] Building Backend Services using Dockerized Maven...
docker run --rm -v "%cd%":/app -w /app maven:3.9-eclipse-temurin-17 mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo Error building Maven artifacts.
    exit /b %errorlevel%
)

echo.
echo [3/3] Starting Transaction and Gateway Services...
docker compose up -d transaction-service

echo.
echo ==============================================================================
echo   Backend Infrastructure ^& Services are UP and RUNNING!
echo   - PostgreSQL: localhost:5432
echo   - Redis: localhost:6379
echo   - Kafka: localhost:9092
echo   - Transaction Service: http://localhost:8081
echo   - Prometheus: http://localhost:9090
echo.
echo   To launch the React Frontend:
echo     cd frontend
echo     npm install
echo     npm start
echo ==============================================================================
