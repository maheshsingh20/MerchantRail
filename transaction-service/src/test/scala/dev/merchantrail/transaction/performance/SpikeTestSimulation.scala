package dev.merchantrail.transaction.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Spike test - sudden load increase to test system resilience.
 * 
 * Simulates Black Friday / flash sale scenarios.
 */
class SpikeTestSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8081")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val submitTransaction = scenario("Spike Load")
    .exec(
      http("Submit Transaction")
        .post("/api/v1/transactions")
        .body(StringBody(session => 
          s"""{
            "merchantId": "MERCH${session.userId % 100 + 1}",
            "amount": ${(Math.random() * 1000 + 50).toInt},
            "currency": "USD",
            "idempotencyKey": "spike-${System.currentTimeMillis()}-${session.userId}"
          }"""
        )).asJson
        .check(status.in(201, 429, 503)) // Accept rate limiting or service unavailable
    )

  setUp(
    submitTransaction.inject(
      nothingFor(5.seconds),          // Baseline
      rampUsers(50).during(10.seconds),  // Normal load
      nothingFor(10.seconds),
      rampUsers(1000).during(10.seconds), // SPIKE! 1000 users in 10 seconds
      nothingFor(30.seconds),
      rampUsers(50).during(10.seconds)   // Back to normal
    )
  ).protocols(httpProtocol)
   .assertions(
     // During spike, some requests may be rate-limited (429)
     global.successfulRequests.percent.gt(80),
     // But system should recover
     global.responseTime.percentile4.lt(2000)
   )
}
