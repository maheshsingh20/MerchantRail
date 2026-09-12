# Pivotal Cloud Foundry (PCF) & VMware Tanzu Deployment Guide

This guide details the production deployment, autoscaling, and zero-downtime release strategy for **MerchantRail** across **VMware Tanzu Application Service (TAS) / Pivotal Cloud Foundry (PCF)** environments.

---

## 1. Cloud Foundry Architecture Overview

MerchantRail follows Twelve-Factor App principles on Cloud Foundry (PAAS), leveraging the native **GoRouter**, **Loggregator**, and **PCF Marketplace Services**:

```
                       ┌──────────────────────────────┐
                       │    PCF Cloud Foundry Router   │ (GoRouter / SSL Offload)
                       └──────────────┬───────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              │ (Route: *.apps.pcf)   │                       │
      ┌───────▼────────┐      ┌───────▼────────┐      ┌───────▼────────┐
      │ merchantrail-  │      │ merchantrail-  │      │ merchantrail-  │
      │ api-gateway    │      │ transaction    │      │ ledger         │
      │ (2 instances)  │      │ (3 instances)  │      │ (2 instances)  │
      └───────┬────────┘      └───────┬────────┘      └───────┬────────┘
              │                       │                       │
              └───────────────────────┼───────────────────────┘
                                      │ (Internal Container Networking)
                 ┌────────────────────┴────────────────────┐
                 ▼                                         ▼
      ┌─────────────────────┐                   ┌─────────────────────┐
      │  pcf-postgres-db    │                   │  pcf-kafka-stream   │
      │  (Managed Service)  │                   │  (Event Streaming)  │
      └─────────────────────┘                   └─────────────────────┘
```

---

## 2. Prerequisites & Marketplace Provisioning

Target your enterprise PCF foundation, organization, and space:

```bash
cf login -a https://api.sys.pcf.internal -u <username> -o payments-core -s transaction-stream
```

### Provisioning Backing Services from the PCF Marketplace

MerchantRail automatically binds to credentials provided by `VCAP_SERVICES`:

```bash
# 1. PostgreSQL Database Service
cf create-service p-postgresql standard pcf-postgres-db

# 2. Redis Distributed Cache & Idempotency Store
cf create-service p-redis standard-ha pcf-redis-cache

# 3. Kafka / Event Streams
cf create-service p-event-streams standard pcf-kafka-stream
```

---

## 3. Production Deployment Execution

### Step 1: Build Optimized Fat JARs

```bash
mvn clean package -DskipTests
```

### Step 2: Zero-Downtime Rolling Push

Deploy using the multi-application manifest with rolling updates to maintain 99.999% payment switching availability:

```bash
cf push -f manifest-pcf.yml --strategy rolling
```

During a rolling deployment:
1. Cloud Foundry spins up a new instance running the updated JAR with the active buildpack.
2. The health check (`/actuator/health`) probes until status returns `UP`.
3. The GoRouter begins sending live transaction traffic to the new instance while draining and terminating old instances sequentially.

---

## 4. Autoscaling Policy Configuration

Configure the PCF App Autoscaler for the `merchantrail-transaction-service` to dynamically absorb traffic spikes during peak card clearing windows:

```json
{
  "instance_min_count": 3,
  "instance_max_count": 12,
  "scaling_rules": [
    {
      "metric_type": "http_throughput",
      "stat_window_secs": 60,
      "breach_duration_secs": 120,
      "threshold": 1200,
      "operator": ">=",
      "adjustment": "+2"
    },
    {
      "metric_type": "response_time",
      "stat_window_secs": 60,
      "breach_duration_secs": 60,
      "threshold": 350,
      "operator": ">=",
      "adjustment": "+3"
    },
    {
      "metric_type": "cpu",
      "stat_window_secs": 120,
      "breach_duration_secs": 180,
      "threshold": 75,
      "operator": ">=",
      "adjustment": "+2"
    }
  ]
}
```

Apply the policy:
```bash
cf attach-autoscaling-policy merchantrail-transaction-service autoscaler-policy.json
```

---

## 5. Live Telemetry & Log Streaming

Stream logs from the PCF Loggregator firehose:

```bash
cf logs merchantrail-transaction-service --recent
```

Inspect health and dynamic metrics:
```bash
cf app merchantrail-transaction-service
cf app merchantrail-ledger-service
```
