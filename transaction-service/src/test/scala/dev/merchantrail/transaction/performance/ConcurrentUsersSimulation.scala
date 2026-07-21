package dev.merchantrail.transaction.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Concurrent users simulation - realistic user behavior patterns.
 * 
 * Simulates merchants checking transactions while new ones are submitted.
 */
class ConcurrentUsersSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8081")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Merchant submitting transactions
  val merchantScenario = scenario("Merchant Operations")
    .exec(
      http("Submit Transaction")
        .post("/api/v1/transactions")
        .body(StringBody(session => 
          s"""{
            "merchantId": "MERCH${session.userId % 50 + 1}",
            "amount": ${(Math.random() * 500 + 10).toInt},
            "currency": "USD",
            "idempotencyKey": "concurrent-${System.currentTimeMillis()}-${session.userId}"
          }"""
        )).asJson
        .check(status.is(201))
        .check(jsonPath("$.transactionId").saveAs("txnId"))
    )
    .pause(2.seconds)
    .exec(
      http("Check Status")
        .get("/api/v1/transactions/${txnId}")
        .check(status.is(200))
    )
    .pause(3.seconds)
    .exec(
      http("List My Transactions")
        .get("/api/v1/transactions?merchantId=MERCH${userId % 50 + 1}&size=10")
        .check(status.is(200))
    )

  // Admin checking system health
  val adminScenario = scenario("Admin Monitoring")
    .exec(
      http("Health Check")
        .get("/actuator/health")
        .check(status.is(200))
    )
    .pause(5.seconds)
    .exec(
      http("View All Transactions")
        .get("/api/v1/transactions?page=0&size=50")
        .check(status.is(200))
    )
    .pause(10.seconds)

  setUp(
    merchantScenario.inject(
      rampUsers(200).during(30.seconds),
      constantUsersPerSec(100).during(120.seconds)
    ),
    adminScenario.inject(
      rampUsers(10).during(30.seconds),
      constantUsersPerSec(5).during(120.seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(800),
     global.successfulRequests.percent.gt(98)
   )
}
