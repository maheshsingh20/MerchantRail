# Sprint 1 - Foundation & Transaction Service Core

**Duration**: Week 1  
**Status**: In Progress  
**Sprint Goal**: Establish project foundation with transaction-service implementing clean/hexagonal architecture, comprehensive unit and integration tests, and basic CI pipeline.

## Sprint Planning

### Sprint Objectives
1. Set up monorepo structure with shared-kernel
2. Implement transaction-service with complete clean/hexagonal architecture
3. Achieve >80% test coverage on domain and application layers
4. Set up CI pipeline with automated testing
5. Establish Agile workflow artifacts (this document, GitHub Projects board)

### User Stories / Tasks

#### Epic: Project Foundation
- [x] Initialize Git repository
- [x] Create README.md with system overview and architecture diagram
- [x] Create TEST_STRATEGY.md documenting test pyramid approach
- [x] Set up .gitignore for Java/Maven/Docker/IDE files
- [x] Create sprint documentation structure
- [ ] Set up GitHub Projects board (To Do / In Progress / Done)
- [ ] Create conventional commit message template

#### Epic: Shared Kernel
- [ ] Create shared-kernel module
- [ ] Implement Money value object with currency support
- [ ] Implement TransactionId value object with validation
- [ ] Implement MerchantId value object with validation
- [ ] Unit tests for all value objects (immutability, validation, equality)

#### Epic: Transaction Service - Domain Layer
- [ ] Create domain module structure (domain/, application/, adapter/, infrastructure/)
- [ ] Implement Transaction entity with state machine (PENDING → APPROVED → SETTLED → REVERSED)
- [ ] Implement TransactionStatus enum with valid state transitions
- [ ] Implement business rules: minimum amount, maximum amount, currency validation
- [ ] Implement IdempotencyKey value object
- [ ] Unit tests for Transaction entity (100% coverage target)
- [ ] Unit tests for state transition logic
- [ ] Unit tests for business rule validation

#### Epic: Transaction Service - Application Layer
- [ ] Define input ports (SubmitTransactionCommand, GetTransactionQuery)
- [ ] Define output ports (TransactionRepository, EventPublisher, IdempotencyService)
- [ ] Implement SubmitTransactionUseCase with idempotency check
- [ ] Implement GetTransactionUseCase
- [ ] Implement GetTransactionsByMerchantUseCase
- [ ] Unit tests with mocked ports (Mockito)
- [ ] Test idempotency logic (duplicate submission returns existing transaction)
- [ ] Test validation failures (negative amount, invalid currency)

#### Epic: Transaction Service - Adapter Layer (Persistence)
- [ ] Create JPA entities (TransactionJpaEntity, IdempotencyKeyJpaEntity)
- [ ] Implement TransactionRepositoryImpl (adapter for output port)
- [ ] Create Spring Data JPA repository interfaces
- [ ] Integration test with Testcontainers Postgres
- [ ] Test save and findById operations
- [ ] Test optimistic locking behavior

#### Epic: Transaction Service - Adapter Layer (Messaging)
- [ ] Create Kafka event DTOs (TransactionInitiatedEvent)
- [ ] Implement KafkaEventPublisher (adapter for output port)
- [ ] Integration test with Testcontainers Kafka
- [ ] Test event is published with correct schema
- [ ] Test serialization/deserialization

#### Epic: Transaction Service - Adapter Layer (Web)
- [ ] Create REST controller (TransactionController)
- [ ] Create DTOs (SubmitTransactionRequest, TransactionResponse)
- [ ] Implement POST /api/v1/transactions endpoint
- [ ] Implement GET /api/v1/transactions/{id} endpoint
- [ ] Implement GET /api/v1/merchants/{merchantId}/transactions endpoint
- [ ] Integration test with @SpringBootTest and MockMvc
- [ ] Test 201 Created response with Location header
- [ ] Test 400 Bad Request for invalid input
- [ ] Test 409 Conflict for duplicate idempotency key

#### Epic: Transaction Service - Infrastructure
- [ ] Spring Boot main application class
- [ ] application.yml configuration (Postgres, Kafka, Redis)
- [ ] Bean wiring configuration (connect use cases to adapters)
- [ ] Health check endpoint (/actuator/health)
- [ ] Prometheus metrics endpoint (/actuator/prometheus)
- [ ] Dockerfile for containerization
- [ ] Docker Compose for local development (Postgres, Kafka, Redis only)

#### Epic: Redis Idempotency Service
- [ ] Create RedisIdempotencyService (adapter for output port)
- [ ] Implement check and store operations with TTL
- [ ] Integration test with Testcontainers Redis
- [ ] Test concurrent duplicate submissions (race condition handling)

#### Epic: CI/CD Pipeline
- [ ] Create .github/workflows/ci.yml
- [ ] Stage 1: Lint (Checkstyle or SpotBugs)
- [ ] Stage 2: Unit tests
- [ ] Stage 3: Build
- [ ] Stage 4: Integration tests (Testcontainers)
- [ ] Stage 5: JaCoCo coverage report and gate (>80% for domain/application)
- [ ] Badge in README showing build status

#### Epic: Documentation
- [ ] Document transaction-service architecture in README
- [ ] Add sequence diagram for transaction submission flow
- [ ] Document how to run locally
- [ ] Document how to run tests

## Sprint Metrics

### Velocity
- **Planned Story Points**: 34
- **Completed Story Points**: 4 (in progress)
- **Completion Rate**: 12%

### Test Metrics (Target)
- **Unit Test Count**: 50+
- **Integration Test Count**: 15+
- **Code Coverage (domain/application)**: >80%
- **Build Time**: <5 minutes
- **Test Execution Time**: <30 seconds (unit), <2 minutes (integration)

## Daily Progress

### Day 1 (Current)
**Completed**:
- ✅ Git repository initialized
- ✅ README.md with architecture overview
- ✅ TEST_STRATEGY.md with comprehensive pyramid explanation
- ✅ .gitignore configured
- ✅ Sprint 1 documentation created

**In Progress**:
- 🔄 Shared-kernel module setup
- 🔄 Transaction-service domain layer

**Blockers**: None

---

### Day 2
**Planned**:
- Complete shared-kernel value objects with tests
- Complete transaction-service domain layer with tests
- Start application layer use cases

---

### Day 3
**Planned**:
- Complete application layer with unit tests
- Start persistence adapter

---

### Day 4
**Planned**:
- Complete persistence and messaging adapters
- Integration tests with Testcontainers

---

### Day 5
**Planned**:
- Complete web adapter (REST controllers)
- Complete infrastructure setup
- CI pipeline configuration

---

### Day 6-7
**Planned**:
- End-to-end local testing
- Documentation polish
- Sprint retrospective

## Decisions Made

### ADR-001: Use Hexagonal Architecture
**Decision**: Every service will follow hexagonal/ports-and-adapters architecture.

**Rationale**: 
- Keeps business logic (domain/application) independent of frameworks
- Enables testing without infrastructure
- Demonstrates architectural discipline expected at payments companies

**Consequences**: More upfront structure, but easier testing and future changes.

---

### ADR-002: Separate JPA Entities from Domain Entities
**Decision**: Domain entities have no JPA annotations. Adapter layer has separate JPA entities that map to/from domain entities.

**Rationale**:
- Domain layer stays framework-agnostic
- Can change persistence technology without touching business logic
- Clear separation of concerns

**Consequences**: Requires mapping logic, but worth the architectural purity.

---

### ADR-003: Use Testcontainers for Integration Tests
**Decision**: Integration tests use real Postgres/Kafka/Redis via Testcontainers, not mocks or H2.

**Rationale**:
- Catches real integration issues (serialization, SQL dialect, connection pooling)
- More confidence in production behavior
- Industry standard approach

**Consequences**: Requires Docker, slightly slower CI, but worth the confidence.

---

### ADR-004: Coverage Gate Only on Domain/Application Layers
**Decision**: JaCoCo enforces >80% coverage on domain/ and application/ packages only, not adapters.

**Rationale**:
- Business logic lives in domain/application
- Adapters are tested via integration tests
- Unit test coverage metrics on adapters are misleading (mocking JPA/Kafka defeats the purpose)

**Consequences**: More realistic coverage metrics that reflect actual business logic coverage.

## Retrospective (End of Sprint)

*To be filled at end of sprint*

### What Went Well
- 

### What Didn't Go Well
- 

### Action Items for Next Sprint
- 

---

**Next Sprint Preview**: Sprint 2 will add fraud-service (with Spock tests), implement Kafka event flow, add saga pattern with compensating transactions, and implement outbox pattern for reliable event publishing.
