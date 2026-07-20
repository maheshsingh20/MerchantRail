# MerchantRail

A distributed payment gateway system demonstrating enterprise-grade microservices architecture, event-driven design, and comprehensive test engineering.

## 🎯 Project Goals

This project is designed as a portfolio piece targeting Software Engineer I / SDET roles at payments companies. It demonstrates:

- **Microservices Architecture**: 8 loosely-coupled services with clean boundaries
- **Clean/Hexagonal Architecture**: Domain-driven design with ports & adapters pattern
- **Event-Driven Design**: Kafka-based choreography with saga pattern for distributed transactions
- **Multi-Protocol Support**: REST, gRPC, WebSocket, SFTP, ISO 8583, ISO 20022
- **Test Engineering Excellence**: Full test pyramid from unit to chaos testing
- **CI/CD & DevOps**: Automated pipelines, GitOps, progressive delivery
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

## 🎓 Learning Outcomes

### Architecture Patterns
- Clean/Hexagonal Architecture with ports & adapters
- Saga pattern for distributed transactions
- Outbox pattern for reliable event publishing
- Idempotency with Redis
- Double-entry bookkeeping

### Payment Domain
- ISO 8583 message format (bank authorization)
- ISO 20022 XML format (settlement files)
- Transaction lifecycle management
- Fraud detection patterns
- Settlement and reconciliation

### DevOps & Testing
- Full test pyramid implementation
- Chaos engineering with Toxiproxy
- GitOps with Argo CD
- Progressive delivery (canary deployments)
- Metrics-driven testing

## 📝 Documentation

- [TEST_STRATEGY.md](./TEST_STRATEGY.md) - Comprehensive testing approach
- [docs/architecture/](./docs/architecture/) - Architecture decision records
- [docs/sprints/](./docs/sprints/) - Sprint summaries and retrospectives
- [docs/protocols/](./docs/protocols/) - ISO 8583 & ISO 20022 implementation notes

## 🔧 Development Status

- [x] Phase 1: Transaction service core ✅ COMPLETE
- [x] Phase 2: Event-driven architecture & saga pattern ✅ COMPLETE
- [x] Phase 3: Bank simulator, gRPC, ISO 8583 ✅ COMPLETE
- [ ] Phase 4: Ledger service, settlement, remaining services (In Progress)
- [ ] Phase 5: Testing excellence & resilience
- [ ] Phase 6: Frontend, release automation & documentation

## 📄 License

MIT License - This is a portfolio/learning project

## 🤝 Contributing

This is a personal portfolio project, but feedback and suggestions are welcome via issues.

---

**Built by**: [Your Name]
**Target Role**: Software Engineer I / SDET (Payments)
**Last Updated**: Phase 1 - Sprint 1
