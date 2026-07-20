# Application Release Automation (ARA) Strategy

## Overview

MerchantRail implements Application Release Automation to ensure safe, reliable, and automated deployments with minimal manual intervention.

## Release Pipeline

### 1. Continuous Integration (GitHub Actions)

**Triggers**: Every push to main, every PR
**Stages**:
```
Lint → Unit Tests → Build → Integration Tests → Security Scans → Docker Build → Deploy
```

**Automation**:
- ✅ Automatic test execution
- ✅ Automatic Docker image building
- ✅ Automatic dependency checks
- ✅ Automatic security scans

### 2. Progressive Delivery

**Strategy**: Canary Deployment Pattern

**Flow**:
```
1. Deploy to 10% of instances
2. Run smoke tests
3. Monitor metrics for 5 minutes
4. If healthy: deploy to 50%
5. Monitor for 5 minutes
6. If healthy: deploy to 100%
7. If any failures: automatic rollback
```

**Implementation** (API Gateway Rate-Based Canary):
```yaml
# New version gets 10% of traffic
spring:
  cloud:
    gateway:
      routes:
        - id: transaction-service-canary
          uri: http://transaction-service-v2:8081
          predicates:
            - Path=/api/v1/transactions/**
            - Weight=transaction-service, 10
        
        - id: transaction-service-stable
          uri: http://transaction-service-v1:8081
          predicates:
            - Path=/api/v1/transactions/**
            - Weight=transaction-service, 90
```

### 3. Smoke Tests

**Post-Deployment Validation**:
```bash
#!/bin/bash
# smoke-test.sh

# Test health endpoints
curl -f http://transaction-service:8081/actuator/health || exit 1

# Test basic functionality
response=$(curl -X POST http://transaction-service:8081/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{"merchantId":"SMOKE001","amount":1.00,"currency":"USD","idempotencyKey":"smoke-test"}')

if echo "$response" | grep -q "transactionId"; then
  echo "Smoke test PASSED"
  exit 0
else
  echo "Smoke test FAILED"
  exit 1
fi
```

### 4. Rollback Strategy

**Automatic Rollback Triggers**:
- Health check failures
- Error rate > 5%
- p95 latency > 1000ms
- Smoke test failures

**Rollback Mechanism**:
```bash
# Kubernetes rollback
kubectl rollout undo deployment/transaction-service

# Docker Compose rollback
docker-compose down
docker-compose up -d --scale transaction-service=3
```

## Deployment Environments

### 1. Local Development
- Docker Compose
- All 8 services
- Manual deployment

### 2. CI/CD Pipeline (Automated)
- GitHub Actions
- Docker containers
- Automated tests + security scans

### 3. Kubernetes (Production-Ready)
```yaml
# k8s/transaction-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: transaction-service
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
      - name: transaction-service
        image: merchantrail/transaction-service:latest
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 30
```

## Metrics-Driven Deployment

**Health Metrics** (Prometheus):
- `up{job="transaction-service"}` - Service availability
- `http_server_requests_seconds_count{status="5xx"}` - Error rate
- `http_server_requests_seconds{quantile="0.95"}` - p95 latency

**Deployment Decision**:
```
IF error_rate < 1% AND p95_latency < 500ms AND availability = 100%
THEN proceed to next canary stage
ELSE rollback
```

## Blue-Green Deployment (Alternative)

**Setup**:
- Blue: Current production (v1.0)
- Green: New version (v1.1)

**Process**:
1. Deploy v1.1 to Green environment
2. Run full test suite on Green
3. Switch API Gateway to Green
4. Monitor for 15 minutes
5. If successful: decommission Blue
6. If failed: switch back to Blue (instant rollback)

## GitOps with Argo CD (Production)

**Repository Structure**:
```
merchantrail-gitops/
├── apps/
│   ├── transaction-service/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── configmap.yaml
│   ├── fraud-service/
│   └── ...
└── argocd/
    └── application.yaml
```

**Argo CD Configuration**:
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: merchantrail
spec:
  source:
    repoURL: https://github.com/yourusername/merchantrail-gitops
    path: apps
    targetRevision: main
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

**Deployment Flow**:
```
1. Developer merges PR
2. CI builds Docker image
3. CI updates GitOps repo (new image tag)
4. Argo CD detects change
5. Argo CD syncs to Kubernetes
6. Rolling update with health checks
```

## Feature Flags (Future Enhancement)

**Use Case**: Enable/disable features without redeployment

**Example**:
```java
if (featureFlags.isEnabled("NEW_FRAUD_ALGORITHM")) {
    return newFraudEngine.check(transaction);
} else {
    return legacyFraudEngine.check(transaction);
}
```

## Observability During Deployment

**Pre-Deployment**:
- Snapshot current metrics (error rate, latency, throughput)

**During Deployment**:
- Real-time dashboard monitoring
- Alert on anomalies
- Automatic rollback on threshold breaches

**Post-Deployment**:
- Compare metrics before/after
- Generate deployment report
- Update runbook if issues found

## CI/CD Tools Used

| Tool | Purpose | Status |
|------|---------|--------|
| GitHub Actions | CI/CD pipeline | ✅ Active |
| Docker | Containerization | ✅ Active |
| Docker Compose | Local orchestration | ✅ Active |
| Kubernetes | Production orchestration | ✅ Ready |
| Argo CD | GitOps | ✅ Configuration ready |
| Prometheus | Metrics | ✅ Active |
| Grafana | Dashboards | ✅ Active |

## Deployment Checklist

**Before Deployment**:
- [ ] All tests passing
- [ ] Code reviewed and approved
- [ ] Security scan completed
- [ ] Database migrations tested
- [ ] Rollback plan documented

**During Deployment**:
- [ ] Smoke tests executed
- [ ] Metrics monitored
- [ ] Logs checked for errors
- [ ] Team notified

**After Deployment**:
- [ ] Verify all services healthy
- [ ] Check error rates
- [ ] Validate transactions flowing
- [ ] Update documentation

## Disaster Recovery

**Recovery Time Objective (RTO)**: < 5 minutes  
**Recovery Point Objective (RPO)**: < 1 minute (Kafka event log)

**Backup Strategy**:
- Database: Automated daily backups + WAL archiving
- Configuration: GitOps repo (version controlled)
- Kafka: Replication factor 3, log retention 7 days

---

**Status**: ✅ ARA Infrastructure Complete  
**Next**: Integrate Argo CD for full GitOps workflow
