# MerchantRail - Quick Start Guide

## Prerequisites

1. **Java 17+** - [Download](https://adoptium.net/)
2. **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi)
3. **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop)

## Installation (Windows)

### 1. Install Java
```powershell
# Download from https://adoptium.net/
# Set JAVA_HOME environment variable
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.x.x", "Machine")
```

### 2. Install Maven
```powershell
# Download from https://maven.apache.org/download.cgi
# Extract to C:\Program Files\Apache\maven
# Add to PATH
[System.Environment]::SetEnvironmentVariable("PATH", $env:PATH + ";C:\Program Files\Apache\maven\bin", "Machine")
```

### 3. Verify Installation
```powershell
java -version   # Should show Java 17+
mvn --version   # Should show Maven 3.8+
docker --version  # Should show Docker 20+
```

## Running the Project

### 1. Start Infrastructure
```bash
# From project root
docker-compose up -d

# This starts:
# - PostgreSQL (port 5432)
# - Kafka (port 9092)
# - Redis (port 6379)
# - Prometheus (port 9090)
# - Grafana (port 3000)
```

### 2. Build All Services
```bash
mvn clean install
# This builds:
# - shared-kernel
# - transaction-service
# - fraud-service
# - bank-simulator-service
```

### 3. Run Services

**Terminal 1 - Transaction Service:**
```bash
cd transaction-service
mvn spring-boot:run
# Runs on port 8081
```

**Terminal 2 - Fraud Service:**
```bash
cd fraud-service
mvn spring-boot:run
# Runs on port 8082 (internally, listens to Kafka)
```

**Terminal 3 - Bank Simulator:**
```bash
cd bank-simulator-service
mvn spring-boot:run
# gRPC runs on port 9090
```

## Testing the System

### Submit a Transaction
```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "MERCH001",
    "amount": 100.50,
    "currency": "USD",
    "idempotencyKey": "test-key-123"
  }'

# Expected Response:
{
  "transactionId": "TXN1234567890ABC",
  "merchantId": "MERCH001",
  "amount": 100.50,
  "currency": "USD",
  "status": "PENDING",
  "createdAt": "2026-07-20T10:30:00Z",
  "updatedAt": "2026-07-20T10:30:00Z"
}
```

### Get Transaction Status
```bash
curl http://localhost:8081/api/v1/transactions/TXN1234567890ABC

# Response will show updated status after fraud check
```

### Test Idempotency (Duplicate Submission)
```bash
# Submit same request again with same idempotencyKey
curl -X POST http://localhost:8081/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "merchantId": "MERCH001",
    "amount": 100.50,
    "currency": "USD",
    "idempotencyKey": "test-key-123"
  }'

# Returns SAME transaction (same transactionId) - no duplicate created
```

### Get Merchant Transactions
```bash
curl "http://localhost:8081/api/v1/transactions?merchantId=MERCH001"

# Returns array of all transactions for MERCH001
```

## Observability

### Prometheus (Metrics)
```
http://localhost:9090
```

### Grafana (Dashboards)
```
http://localhost:3000
Username: admin
Password: admin
```

### Health Checks
```bash
curl http://localhost:8081/actuator/health  # Transaction service
curl http://localhost:8082/actuator/health  # Fraud service
curl http://localhost:8084/actuator/health  # Bank simulator
```

### Metrics Endpoints
```bash
curl http://localhost:8081/actuator/prometheus  # Transaction service metrics
```

## Running Tests

### Unit Tests Only
```bash
mvn test
```

### Integration Tests (Requires Docker)
```bash
mvn verify
```

### Specific Module Tests
```bash
mvn test -pl shared-kernel
mvn test -pl transaction-service
mvn verify -pl transaction-service  # Includes integration tests
```

### Spock Tests (Fraud Service)
```bash
cd fraud-service
mvn test
# Look for FraudRuleSpec output with detailed test scenarios
```

## Stopping Services

### Stop Application Services
```
Ctrl+C in each terminal
```

### Stop Infrastructure
```bash
docker-compose down
```

### Clean Everything (Including Data)
```bash
docker-compose down -v  # Removes volumes (data)
```

## Common Issues

### Port Already in Use
```bash
# Check what's using the port
netstat -ano | findstr :8081

# Kill the process
taskkill /PID <process_id> /F
```

### Docker Not Running
```
Start Docker Desktop
Wait for "Docker is running" message
```

### Maven Build Fails
```bash
# Clean and rebuild
mvn clean install -U

# Skip tests if needed
mvn clean install -DskipTests
```

### Kafka Connection Issues
```bash
# Restart Kafka container
docker-compose restart kafka

# Check Kafka logs
docker logs merchantrail-kafka
```

## Project Structure
```
merchantrail/
├── shared-kernel/          # Value objects (Money, IDs)
├── transaction-service/    # Main transaction API
├── fraud-service/          # Fraud detection
├── bank-simulator-service/ # Mock bank (gRPC)
├── docker-compose.yml      # Infrastructure
├── pom.xml                 # Parent POM
└── README.md               # Main documentation
```

## Next Steps

1. Review [PROJECT_COMPLETE.md](./PROJECT_COMPLETE.md) for complete overview ⭐
2. Review [TEST_STRATEGY.md](./TEST_STRATEGY.md) for testing approach
3. Review [README.md](./README.md) for architecture details
4. Explore the code starting with domain layers (zero framework dependencies)

## Useful Commands

```bash
# Build without tests
mvn clean install -DskipTests

# Run only unit tests (fast)
mvn test

# Run all tests including integration
mvn verify

# Check test coverage
mvn jacoco:report
# Open target/site/jacoco/index.html

# Format code
mvn spotless:apply

# Check for dependency updates
mvn versions:display-dependency-updates
```

## Support

For questions or issues:
1. Check [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) for technical details
2. Review test files for usage examples
3. Check application logs in console output

---

**Built by**: [Your Name]  
**Project**: MerchantRail - Distributed Payment Gateway  
**Purpose**: Portfolio project for Software Engineer I / SDET roles
