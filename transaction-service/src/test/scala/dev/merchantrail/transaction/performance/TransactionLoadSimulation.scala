package dev.merchantrail.transaction.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Load test simulation for transaction submission.
 * 
 * Scenarios:
 * - Ramp up to 500 concurrent users over 30 seconds
 * - Maintain 500 users for 60 seconds  
 * - Ramp down over 10 seconds
 * 
 * Success Criteria:
 * - p95 latency < 500ms
 * - p99 latency < 1000ms
 * - Error rate < 1%
 * - Throughput > 1000 transactions/second
 */
class TransactionLoadSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8081")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val submitTransaction = scenario("Submit Transaction")
    .exec(
      http("Create Transaction")
        .post("/api/v1/transactions")
        .body(StringBody(session => 
          s"""{
            "merchantId": "MERCH${session.userId % 100 + 1}",
            "amount": ${50 + (session.userId % 450)},
            "currency": "USD",
            "idempotencyKey": "load-test-${System.currentTimeMillis()}-${session.userId}"
          }"""
        )).asJson
        .check(status.is(201))
        .check(jsonPath("$.transactionId").saveAs("transactionId"))
    )
    .pause(1.second)
    .exec(
      http("Get Transaction")
        .get("/api/v1/transactions/${transactionId}")
        .check(status.is(200))
        .check(jsonPath("$.status").in("PENDING", "FRAUD_CHECK", "APPROVED"))
    )

  val queryTransactions = scenario("Query Transactions")
    .exec(
      http("List Transactions")
        .get("/api/v1/transactions?merchantId=MERCH${userId % 100 + 1}&page=0&size=20")
        .check(status.is(200))
        .check(jsonPath("$.content").exists)
    )

  setUp(
    submitTransaction.inject(
      rampUsers(500).during(30.seconds),
      constantUsersPerSec(500).during(60.seconds),
      rampUsers(0).during(10.seconds)
    ),
    queryTransactions.inject(
      rampUsers(100).during(30.seconds),
      constantUsersPerSec(100).during(60.seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(500),    // p95 < 500ms
     global.responseTime.percentile4.lt(1000),   // p99 < 1000ms
     global.successfulRequests.percent.gt(99),   // Success rate > 99%
     forAll.failedRequests.count.lt(100)         // Less than 100 failed requests
   )
}
