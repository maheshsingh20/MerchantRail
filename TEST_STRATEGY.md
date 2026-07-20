# Test Strategy - MerchantRail

## Overview

MerchantRail implements a comprehensive test pyramid that validates correctness, resilience, performance, and security across all layers of the distributed payment system. This document explains our testing philosophy, the tools we use, and what each layer of testing catches.

## Testing Philosophy

1. **Fast Feedback**: Unit tests run in milliseconds, integration tests in seconds
2. **Independence**: Tests can run in any order, in parallel, without side effects
3. **Realistic**: Integration tests use real infrastructure (via Testcontainers), not mocks
4. **Coverage with Purpose**: >80% coverage on domain/application layers where business logic lives
5. **Chaos as Validation**: Resilience patterns (saga compensation) are proven under failure, not assumed

## Test Pyramid

```
                    ┌─────────────┐
                    │   Security  │ ← OWASP ZAP, Dependency-Check
                    │   Scanning  │
                    └─────────────┘
                   ┌───────────────┐
                   │  Chaos & Load │ ← Toxiproxy, Gatling
                   │    Testing    │
                   └───────────────┘
                 ┌───────────────────┐
                 │   End-to-End      │ ← RestAssured, WebSocket client
                 │   Protocol Tests  │
                 └───────────────────┘
              ┌─────────────────────────┐
              │  Integration Tests      │ ← Testcontainers (Postgres, Kafka, Redis)
              │  (Adapter Layer)        │
              └─────────────────────────┘
          ┌───────────────────────────────┐
          │    Contract Tests             │ ← Spring Cloud Contract / Pact
          │    (Service Boundaries)       │
          └───────────────────────────────┘
      ┌───────────────────────────────────────┐
      │         Unit Tests                    │ ← JUnit 5, Mockito, Spock
      │  (Domain & Application Layers)        │ ← Fast, isolated, comprehensive
      └───────────────────────────────────────┘
```

## Test Layers

### 1. Unit Tests (JUnit 5 & Mockito)

**What**: Tests for domain entities, value objects, and use cases in isolation.

**Scope**: `domain/` and `application/usecase/` packages only.

**No Infrastructure**: No Spring context, no database, no Kafka, no network calls.

**Tools**:
- JUnit 5 for test structure
- Mockito for mocking port interfaces
- AssertJ for fluent assertions

**Example Coverage**:
```java
// Domain layer
- Transaction state transitions (PENDING → APPROVED → SETTLED)
- Money value object (currency validation, arithmetic)
- Business rules (minimum amount, maximum amount, duplicate detection)

// Application layer (use cases)
- SubmitTransactionUseCase with mocked repository and event publisher
- GetTransactionUseCase with mocked repository
- Edge cases: null handling, validation failures
```

**Coverage Target**: >90% for domain and application layers.

**Why**: These tests run in <1 second total and catch 80% of bugs. They document business rules clearly.

---

### 2. Unit Tests (Spock Framework - Groovy)

**What**: Re-implementation of fraud-service rule engine tests using Spock/Groovy.

**Why Spock**: Demonstrates Groovy scripting capability (a preferred skill in the target JD). Spock's data-driven testing is ideal for rule engines with many input combinations.

**Example Test Structure**:
```groovy
def "fraud score calculation for various transaction patterns"() {
    given: "a transaction with specific characteristics"
    def transaction = new Transaction(amount, merchantId, timestamp)
    
    when: "fraud score is calculated"
    def score = fraudEngine.calculateScore(transaction)
    
    then: "score matches expected risk level"
    score == expectedScore
    
    where:
    amount | merchantId | timestamp           || expectedScore
    100    | "M001"     | now()              || 10
    10000  | "M001"     | now()              || 75  // high amount
    100    | "M999"     | now()              || 50  // new merchant
    100    | "M001"     | now().minus(1.sec) || 90  // velocity violation
}
```

**Coverage**: All fraud rule combinations exhaustively tested.

---

### 3. Integration Tests (Testcontainers)

**What**: Tests for adapter layer implementations against real infrastructure.

**Scope**: `adapter/out/persistence/`, `adapter/out/messaging/`, `adapter/out/client/`

**Tools**:
- Testcontainers for Postgres, Kafka, Redis
- Spring Boot Test with `@SpringBootTest`
- Awaitility for async assertions

**Example Coverage**:
```java
// Persistence adapter
- TransactionRepositoryImpl save/findById with real Postgres
- JPA entity mapping correctness
- Optimistic locking behavior

// Messaging adapter
- KafkaEventPublisher actually publishes to Kafka topic
- Consumer receives and deserializes event correctly
- Consumer idempotency (duplicate events handled)

// Outbox pattern
- Transaction + outbox entry written in same DB transaction
- Outbox poller publishes to Kafka and marks as published
```

**Why Real Infrastructure**: Mocking Postgres/Kafka hides integration bugs (serialization, schema mismatches, connection pooling issues). Testcontainers runs the real thing in Docker.

**Execution Time**: ~10-30 seconds (containers start once, reused across tests).

---

### 4. Contract Tests (Spring Cloud Contract)

**What**: Verify that service APIs (REST and Kafka events) don't break their consumers.

**Producer Side**: Define contract (e.g., transaction-service publishes `transaction.initiated` event with specific schema).

**Consumer Side**: Auto-generated test verifies consumer can parse the contract.

**Example Contract** (transaction-service as producer):
```groovy
Contract.make {
    description "transaction.initiated event published when transaction is submitted"
    label "transaction_initiated"
    input {
        triggeredBy("submitTransaction()")
    }
    outputMessage {
        sentTo "transaction.initiated"
        body([
            transactionId: $(producer(regex('[A-Z0-9]{16}')), consumer('TXN1234567890123')),
            merchantId: $(producer(regex('[A-Z0-9]{8}')), consumer('MERCH001')),
            amount: $(producer(regex('[0-9]+')), consumer('10000')),
            currency: "USD",
            status: "PENDING",
            timestamp: $(producer(anyIso8601DateTime()), consumer('2026-07-20T10:00:00Z'))
        ])
        headers {
            messagingContentType(applicationJson())
        }
    }
}
```

**CI Enforcement**: If fraud-service deploys a change that breaks the consumer contract, the producer's CI build fails before merge.

**Why**: Prevents runtime "deserialization failed" errors in production.

---

### 5. End-to-End Tests (RestAssured)

**What**: Full transaction lifecycle from merchant API call to final status.

**Example Flow**:
1. Merchant submits transaction via REST API
2. Test subscribes to WebSocket for live updates
3. Assert transaction moves through states: PENDING → FRAUD_CHECK → APPROVED → SETTLED
4. Assert ledger entries created (double-entry)
5. Assert merchant webhook callback received

**Tools**:
- RestAssured for HTTP assertions
- Java WebSocket client for live updates
- Awaitility for async polling

**Execution Time**: ~5-10 seconds per scenario.

**Why**: Validates the whole system works together, including all async hops.

---

### 6. Protocol Tests

**What**: Explicit tests for each protocol mentioned in the JD (HTTPS, sockets, SFTP, RPC).

**Coverage**:

#### HTTPS (RestAssured)
```java
given()
    .header("Authorization", "Bearer " + jwt)
    .contentType(ContentType.JSON)
    .body(transactionRequest)
.when()
    .post("/api/v1/transactions")
.then()
    .statusCode(201)
    .body("transactionId", notNullValue())
    .body("status", equalTo("PENDING"));
```

#### Raw Socket Test (Apache Commons Net or Java Socket)
```java
// Test low-level socket communication for bank-simulator health check
try (Socket socket = new Socket("localhost", 9091)) {
    OutputStream out = socket.getOutputStream();
    out.write("PING\n".getBytes());
    
    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    String response = in.readLine();
    assertEquals("PONG", response);
}
```

#### SFTP (Apache Commons Net)
```java
// Test ledger-service drops settlement file to SFTP server
JSch jsch = new JSch();
Session session = jsch.getSession("sftp-user", "localhost", 2222);
session.setPassword("sftp-pass");
session.connect();

ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
sftpChannel.connect();

Vector<ChannelSftp.LsEntry> files = sftpChannel.ls("/settlement");
assertTrue(files.stream().anyMatch(f -> f.getFilename().startsWith("settlement_2026")));

// Download and validate ISO 20022 XML structure
sftpChannel.get("/settlement/settlement_20260720.xml", "./test-output.xml");
Document doc = parseXML("./test-output.xml");
assertNotNull(doc.getElementsByTagName("pain.001.001.03"));
```

#### gRPC (grpc-java test stubs)
```java
// Test transaction-service calls bank-simulator via gRPC
ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
    .usePlaintext()
    .build();

BankAuthorizationServiceGrpc.BankAuthorizationServiceBlockingStub stub =
    BankAuthorizationServiceGrpc.newBlockingStub(channel);

AuthorizationRequest request = AuthorizationRequest.newBuilder()
    .setPan("4111111111111111")
    .setAmount(10000)
    .setMti("0100") // ISO 8583 MTI for auth request
    .build();

AuthorizationResponse response = stub.authorize(request);
assertEquals("00", response.getResponseCode()); // Approval
```

**Why**: Explicitly demonstrates experience with each protocol the JD requires.

---

### 7. Chaos Tests (Toxiproxy)

**What**: Inject latency, timeouts, and connection failures into bank-simulator and Kafka to verify saga compensation logic works under real failure conditions.

**Tools**:
- Toxiproxy (proxy between services, injects toxics)
- Testcontainers Toxiproxy module

**Scenarios**:

#### Bank Timeout → Saga Compensation
```java
@Test
void whenBankSimulatorTimesOut_sagaCompensatesAndReversesLedgerEntry() {
    // Given: Toxiproxy adds 10 second latency to bank-simulator
    toxiproxy.toxics()
        .latency("bank-latency", ToxicDirection.DOWNSTREAM, 10_000);
    
    // When: Transaction is submitted
    String txnId = submitTransaction(merchantId, 10000, "USD");
    
    // Then: Transaction times out and moves to REVERSED status
    await().atMost(15, SECONDS).until(() -> 
        getTransactionStatus(txnId).equals("REVERSED")
    );
    
    // And: Ledger entries are compensated (net balance = 0)
    List<LedgerEntry> entries = ledgerRepository.findByTransactionId(txnId);
    assertEquals(4, entries.size()); // 2 original + 2 reversal
    assertEquals(0, entries.stream().mapToLong(LedgerEntry::getAmount).sum());
}
```

#### Kafka Partition Failure → Outbox Pattern Recovery
```java
@Test
void whenKafkaIsDown_outboxPatternRetriesEventPublishing() {
    // Given: Toxiproxy blocks Kafka connection
    toxiproxy.toxics()
        .bandwidth("kafka-disconnect", ToxicDirection.DOWNSTREAM, 0);
    
    // When: Transaction is submitted
    String txnId = submitTransaction(merchantId, 5000, "USD");
    
    // Then: Transaction is saved to DB with outbox entry
    await().atMost(2, SECONDS).until(() -> 
        outboxRepository.findByTransactionId(txnId).isPresent()
    );
    
    // When: Kafka connection is restored
    toxiproxy.toxics().get("kafka-disconnect").remove();
    
    // Then: Outbox poller publishes event, consumers receive it
    await().atMost(10, SECONDS).until(() -> 
        fraudService.hasReceivedTransaction(txnId)
    );
}
```

**Why**: The saga pattern is the most complex part of the system. Without chaos testing, you can't prove it actually works under failure. This is the interview-differentiating test.

**Observability Assertion**: These tests also assert on Prometheus metrics:
```java
double errorRate = prometheusRegistry.get("transactions.failed").counter().count();
assertTrue(errorRate < 0.05); // <5% error rate even under chaos
```

---

### 8. Load & Performance Tests (Gatling)

**What**: Measure system throughput and latency characteristics under load.

**Scenario**: 500 concurrent merchants submitting transactions for 60 seconds.

**Gatling Script** (Scala DSL):
```scala
val submitTransaction = scenario("Submit Transaction")
  .exec(http("Create Transaction")
    .post("/api/v1/transactions")
    .header("Authorization", "Bearer ${jwt}")
    .body(StringBody("""{"merchantId":"${merchantId}","amount":10000,"currency":"USD"}"""))
    .check(status.is(201))
    .check(jsonPath("$.transactionId").saveAs("txnId"))
  )
  .pause(1)
  .exec(http("Check Status")
    .get("/api/v1/transactions/${txnId}")
    .check(status.is(200))
  )

setUp(
  submitTransaction.inject(
    rampUsers(500).during(30.seconds),
    constantUsersPerSec(500).during(60.seconds)
  )
).protocols(http.baseUrl("http://localhost:8080"))
```

**Success Criteria**:
- p95 latency < 500ms for transaction submission
- p99 latency < 1s
- 0% error rate at 500 concurrent users
- System identifies bottleneck (likely Kafka or Postgres connection pool)

**Why**: Demonstrates understanding of performance testing and capacity planning.

---

### 9. Security Tests

#### OWASP Dependency-Check
```bash
mvn org.owasp:dependency-check-maven:check
```

**Checks**: Known CVEs in dependencies (Spring Boot, Kafka clients, etc.).

**CI Gate**: Pipeline fails if high-severity vulnerabilities found.

#### OWASP ZAP Baseline Scan
```bash
docker run -v $(pwd):/zap/wrk:rw owasp/zap2docker-stable \
  zap-baseline.py -t http://host.docker.internal:8080 -r zap-report.html
```

**Checks**: XSS, SQL injection, insecure headers, CSRF vulnerabilities.

---

### 10. Chaos Engineering Tests (NEW)

**Tool**: Toxiproxy (network fault injection)

**Scenarios**:

#### Test 1: Database Latency Handling
```java
@Test
void shouldHandleDatabaseLatencyGracefully() {
    // Inject 2s latency to database
    proxy.toxics().latency("db-latency", ToxicDirection.DOWNSTREAM, 2000);
    
    Transaction result = submitTransactionUseCase.execute(command);
    
    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
}
```

#### Test 2: Network Partition Recovery
```java
@Test
void idempotencyShouldWorkWithRetriesAfterNetworkFailure() {
    Transaction first = submitTransactionUseCase.execute(command);
    
    // Simulate connection reset
    proxy.toxics().resetPeer("reset", ToxicDirection.DOWNSTREAM, 1000);
    
    // Retry with same idempotency key
    Transaction retry = submitTransactionUseCase.execute(command);
    
    // Should return same transaction (idempotency preserved)
    assertThat(retry.getTransactionId()).isEqualTo(first.getTransactionId());
}
```

#### Test 3: Timeout Behavior
```java
@Test
void shouldTimeoutOnExcessiveDatabaseLatency() {
    // Inject 30s latency (exceeds timeout)
    proxy.toxics().latency("extreme-latency", ToxicDirection.DOWNSTREAM, 30000);
    
    assertThatThrownBy(() -> submitTransactionUseCase.execute(command))
        .isInstanceOf(RuntimeException.class);
}
```

#### Test 4: Network Jitter Resilience
```java
@Test
void shouldMaintainConsistencyUnderNetworkJitter() {
    // Add random latency (jitter)
    proxy.toxics().latency("jitter", ToxicDirection.DOWNSTREAM, 500).setJitter(300);
    
    // Submit 5 transactions under jitter
    for (int i = 0; i < 5; i++) {
        Transaction result = submitTransactionUseCase.execute(command);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }
}
```

**Why**: Validates saga compensation logic works under real failure conditions (not just happy path).

---

#### OWASP ZAP Baseline Scan
```bash
docker run -v $(pwd):/zap/wrk:rw \
  owasp/zap2docker-stable zap-baseline.py \
  -t http://host.docker.internal:8080/api/v1 \
  -r zap-report.html
```

**Checks**: XSS, SQL injection, insecure headers, CSRF vulnerabilities.

#### OWASP Dependency-Check
```bash
mvn org.owasp:dependency-check-maven:check
```

**Checks**: Known CVEs in dependencies (Spring Boot, Kafka clients, etc.).

**CI Gate**: Pipeline fails if high-severity vulnerabilities found.

---

## Coverage Enforcement (JaCoCo)

**Maven Plugin Config**:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                        <includes>
                            <include>*.domain.*</include>
                            <include>*.application.*</include>
                        </includes>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Why Only Domain/Application**: Adapter code (controllers, JPA repos) is tested via integration tests. Unit test coverage metrics for adapters are misleading.

---

## Known Gaps & Future Improvements

### Current Gaps (Phase 1)
- [ ] Contract tests not yet implemented (Phase 2)
- [ ] Chaos tests not yet implemented (Phase 4)
- [ ] Load tests not yet implemented (Phase 4)
- [ ] Security scans not yet in CI (Phase 4)
- [ ] Mutation testing not implemented (stretch goal)

### Future Improvements
- **Mutation Testing**: Use PIT to verify test quality (not just coverage)
- **Property-Based Testing**: Use jqwik for domain value object tests
- **Visual Regression Testing**: Playwright for frontend (Phase 5)
- **API Fuzzing**: Use RESTler to generate malformed requests

---

## Running Tests

### Unit Tests Only
```bash
mvn test
```

### Integration Tests (Requires Docker)
```bash
mvn verify
```

### End-to-End Tests (Requires All Services Running)
```bash
docker-compose up -d
mvn verify -P e2e-tests
```

### Chaos Tests (Manual Workflow)
```bash
# Start services with Toxiproxy
docker-compose -f docker-compose.chaos.yml up -d

# Run chaos test suite
mvn verify -P chaos-tests
```

### Load Tests
```bash
# Start services
docker-compose up -d

# Run Gatling simulation
mvn gatling:test
```

### Security Scans
```bash
# Dependency check
mvn org.owasp:dependency-check-maven:check

# ZAP scan (requires services running)
docker run -v $(pwd):/zap/wrk:rw owasp/zap2docker-stable \
  zap-baseline.py -t http://host.docker.internal:8080 -r zap-report.html
```

---

## CI Pipeline Integration

All test layers except chaos/load run on every PR:

```yaml
# .github/workflows/ci.yml
1. Lint & Static Analysis
2. Unit Tests (JUnit + Spock)
3. Contract Tests
4. Build Docker Images
5. Integration Tests (Testcontainers)
6. Deploy to local K8s (kind)
7. E2E Smoke Tests
8. Coverage Gate (JaCoCo >80%)
9. Security Scan (Dependency-Check)
```

Chaos and load tests run on-demand (separate workflow) because they're slower and noisier.

---

## Metrics & Observability in Tests

Tests aren't just pass/fail — they also assert on system health metrics:

```java
@Test
void systemMaintainsHealthyMetricsUnderLoad() {
    // Generate load
    submitTransactions(1000);
    
    // Assert on Prometheus metrics
    double errorRate = metricsRegistry.get("http.server.requests")
        .tag("status", "5xx")
        .counter()
        .count() / totalRequests;
    
    assertTrue(errorRate < 0.01, "Error rate should be <1%");
    
    double p95Latency = metricsRegistry.get("http.server.requests")
        .timer()
        .percentile(0.95);
    
    assertTrue(p95Latency < 500, "p95 latency should be <500ms");
}
```

---

## Test Data Management

**Strategy**: Each test is responsible for its own test data setup and teardown.

**Approaches**:
- **Unit tests**: Pure objects, no persistence
- **Integration tests**: `@Transactional` with rollback, or explicit cleanup in `@AfterEach`
- **E2E tests**: Test-specific merchant IDs (e.g., `MERCHANT_TEST_001`) that don't conflict

**No Shared State**: Tests can run in parallel without interfering with each other.

---

## Conclusion

This test strategy ensures MerchantRail is not just a "it works on my machine" demo, but a production-ready system with proven correctness, resilience, and performance characteristics. Every layer of the test pyramid serves a specific purpose:

- **Unit tests**: Fast feedback on business logic
- **Integration tests**: Confidence in infrastructure integration
- **Contract tests**: Safety when evolving APIs
- **E2E tests**: End-user scenarios work
- **Protocol tests**: Explicit demonstration of protocol expertise
- **Chaos tests**: Resilience patterns actually work under failure
- **Load tests**: Understanding of performance characteristics
- **Security tests**: Basic vulnerability hygiene

This depth of testing is what differentiates a junior engineer who writes tests from an SDET who architects test strategies.

---

**Last Updated**: Sprint 1, Phase 1
**Next Update**: After Phase 2 (add contract test results)
