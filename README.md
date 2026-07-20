# MerchantRail

A distributed payment gateway system built with enterprise-grade microservices architecture, event-driven design, and comprehensive test engineering practices.

## 🎯 Overview

MerchantRail is a production-ready payment processing platform that handles transaction authorization, fraud detection, settlement, and merchant management through a distributed microservices architecture.

**Key Capabilities:**
- **Microservices Architecture**: 8 loosely-coupled services with clean boundaries
- **Clean/Hexagonal Architecture**: Domain-driven design with ports & adapters pattern
- **Event-Driven Design**: Kafka-based choreography with saga pattern for distributed transactions
- **Multi-Protocol Support**: REST, gRPC, WebSocket, SFTP, ISO 8583, ISO 20022
- **Test Engineering Excellence**: Full test pyramid from unit to chaos testing
- **CI/CD & Automation**: Automated pipelines, security scanning, progressive delivery
- **Resilience Engineering**: Chaos testing, circuit breakers, compensation logic

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway                           │
│            (Rate Limiting, Routing, Auth)                    │
└──────────────┬──────────────────────────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
   ┌───▼────┐      ┌───▼──────┐
   │ Auth   │      │ Merchant │
   │Service │      │ Service  │
   └────────┘      └──────────┘
                        │
                   ┌────▼──────────┐
                   │ Transaction   │◄──── WebSocket
                   │   Service     │      (Live Updates)
                   │ (Saga Leader) │
                   └───┬───────────┘
                       │ Kafka Events
         ┌─────────────┼─────────────┬──────────────┐
         │             │             │              │
    ┌────▼────┐   ┌───▼────┐   ┌───▼─────┐   ┌────▼────────┐
    │ Fraud   │   │Ledger  │   │  Bank   │   │Notification │
    │Service  │   │Service │   │Simulator│   │  Service    │
    └─────────┘   └───┬────┘   └─────────┘   └─────────────┘
                      │
                      │ SFTP (Nightly)
                      ▼
                 [Settlement Files]
```

## 📦 Services

### Core Services
- **api-gateway**: Single entry point, Spring Cloud Gateway with rate limiting
- **auth-service**: JWT-based authentication, role management (MERCHANT, ADMIN, BANK)
- **merchant-service**: Merchant onboarding, API key management
- **transaction-service**: Transaction orchestration, saga coordinator, WebSocket updates

### Domain Services
- **fraud-service**: Rule-based fraud detection (velocity checks, amount thresholds)
- **ledger-service**: Double-entry bookkeeping, nightly settlement batch generation
- **bank-simulator-service**: Simulates issuing bank (ISO 8583), supports chaos injection
- **notification-service**: Webhook callbacks to merchants on transaction status changes

### Shared
- **shared-kernel**: Common value objects (Money, TransactionId, MerchantId)

## 🔄 Communication Protocols

- **REST (HTTPS)**: Merchant-facing API via gateway
- **Kafka**: Async event bus for service coordination
- **gRPC**: transaction-service ↔ bank-simulator-service
- **WebSocket**: Real-time transaction status updates
- **SFTP**: Settlement batch file delivery

## 🧪 Testing Strategy

See [TEST_STRATEGY.md](./TEST_STRATEGY.md) for comprehensive testing approach.

### Test Pyramid

| Layer | Tools | Coverage |
|-------|-------|----------|
| Unit | JUnit 5, Mockito, Spock | Domain logic, use cases |
| Contract | Spring Cloud Contract | Service API contracts |
| Integration | Testcontainers | Adapter implementations |
| E2E | RestAssured | Full transaction flows |
| Protocol | Custom clients | HTTPS, SFTP, gRPC, sockets |
| Chaos | Toxiproxy | Saga compensation under failure |
| Load | Gatling | Performance characteristics |
| Security | OWASP ZAP, Dependency-Check | Vulnerabilities |

**Coverage Target**: >80% for domain and application layers (enforced by JaCoCo)

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Node.js 18+ (for frontend)

### Local Development

```bash
# Start infrastructure (Postgres, Kafka, Redis, Prometheus, Grafana)
docker-compose up -d

# Build all services
mvn clean install

# Run transaction-service (Phase 1)
cd transaction-service
mvn spring-boot:run

# Run tests
mvn test                    # Unit tests
mvn verify                  # Integration tests with Testcontainers
```

### Kubernetes Deployment

```bash
# Start local cluster
kind create cluster --config infrastructure/k8s/kind-config.yaml

# Deploy services
kubectl apply -f infrastructure/k8s/

# Port forward to gateway
kubectl port-forward svc/api-gateway 8080:8080
```

## 📊 Observability

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Service metrics**: http://localhost:808X/actuator/prometheus

## 🔐 Security

- JWT-based authentication with role-based access control
- API key authentication for merchant API access
- Rate limiting at gateway level
- Regular dependency scanning (OWASP Dependency-Check)
- Security scanning (OWASP ZAP)

## 📈 Agile Delivery

This project follows Agile practices with 1-2 week sprints:

- **Sprint Planning**: See [docs/sprints/](./docs/sprints/)
- **Project Board**: GitHub Projects (To Do / In Progress / Done)
- **Conventional Commits**: Structured commit messages with ticket references

### Current Sprint: Sprint 1 - Foundation
Focus: transaction-service core implementation with clean architecture

## �️ Architecture Patterns

### Design Patterns
- **Clean/Hexagonal Architecture**: Ports & adapters pattern with domain isolation
- **Saga Pattern**: Distributed transaction choreography across services
- **Outbox Pattern**: Reliable event publishing with transactional guarantees
- **Idempotency**: Redis-based duplicate request prevention
- **Double-Entry Bookkeeping**: Financial transaction integrity

### Payment Industry Standards
- **ISO 8583**: Bank authorization message format (gRPC)
- **ISO 20022**: XML settlement file format (pain.001)
- **Transaction Lifecycle**: Complete authorization to settlement flow
- **Fraud Detection**: Rule-based velocity checks and pattern analysis
- **Settlement & Reconciliation**: Automated nightly batch processing

### Testing & Operations
- **Test Pyramid**: Unit → Integration → E2E → Chaos → Security
- **Chaos Engineering**: Toxiproxy-based failure injection
- **GitOps**: Declarative infrastructure with Argo CD
- **Progressive Delivery**: Canary deployments with automatic rollback
- **Observability**: Prometheus metrics + Grafana dashboards

## 📝 Documentation

- **[PROJECT_COMPLETE.md](./PROJECT_COMPLETE.md)** - ⭐ Complete system overview and architecture
- **[SECURITY_TESTING.md](./docs/SECURITY_TESTING.md)** - OWASP security testing approach
- **[ARA_STRATEGY.md](./docs/ARA_STRATEGY.md)** - Application Release Automation strategy
- **[QUICK_START.md](./QUICK_START.md)** - Local development setup guide
- [docs/sprints/](./docs/sprints/) - Development sprint documentation

## 🔧 System Status

- [x] **Core Platform**: Transaction service with clean architecture ✅
- [x] **Event-Driven Architecture**: Kafka-based saga pattern ✅
- [x] **Bank Integration**: gRPC bank simulator with ISO 8583 ✅  
- [x] **Complete System**: All 8 services operational with ISO 20022 settlement ✅
- [x] **Testing Infrastructure**: Unit, Integration, Chaos, Security testing ✅
- [x] **CI/CD Pipeline**: Automated builds, tests, and security scans ✅
- [ ] **Advanced Features**: Frontend dashboard, Kubernetes deployment (Roadmap)

## 📄 License

MIT License

## 🤝 Contributing

Contributions, issues, and feature requests are welcome. Feel free to check the [issues page](../../issues).

---

**Last Updated**: January 2024
