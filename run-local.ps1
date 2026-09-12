# ==============================================================================
# MerchantRail - Zero-Install Local Run Script (PowerShell)
# Uses existing Docker Desktop and Node.js without downloading Java or Maven
# ==============================================================================

Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "  MerchantRail - Payment Switching & Batch Clearing Platform" -ForegroundColor Green
Write-Host "  Starting complete system using existing Docker Desktop..." -ForegroundColor Cyan
Write-Host "==============================================================================" -ForegroundColor Cyan

Write-Host "`n[1/3] Starting Infrastructure (PostgreSQL, Redis, Kafka)..." -ForegroundColor Yellow
docker compose up -d postgres redis kafka

Write-Host "`n[2/3] Building Backend with Dockerized Maven (Zero tools needed on host)..." -ForegroundColor Yellow
$currentDir = (Get-Location).Path
docker run --rm -v "${currentDir}:/app" -w /app maven:3.9-eclipse-temurin-17 mvn clean package -DskipTests

Write-Host "`n[3/3] Starting Transaction Service..." -ForegroundColor Yellow
docker compose up -d transaction-service

Write-Host "`n==============================================================================" -ForegroundColor Green
Write-Host "  Backend Infrastructure & Services are UP and RUNNING!" -ForegroundColor Green
Write-Host "  - PostgreSQL: localhost:5432" -ForegroundColor White
Write-Host "  - Redis: localhost:6379" -ForegroundColor White
Write-Host "  - Kafka: localhost:9092" -ForegroundColor White
Write-Host "  - Transaction Service: http://localhost:8081/actuator/health" -ForegroundColor White
Write-Host "`n  To launch the React Frontend in a new terminal:" -ForegroundColor Cyan
Write-Host "    cd frontend" -ForegroundColor Yellow
Write-Host "    npm install" -ForegroundColor Yellow
Write-Host "    npm start" -ForegroundColor Yellow
Write-Host "==============================================================================" -ForegroundColor Green
