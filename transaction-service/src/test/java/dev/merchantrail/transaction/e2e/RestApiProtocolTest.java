package dev.merchantrail.transaction.e2e;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end REST API protocol tests.
 * 
 * Validates complete HTTPS/REST communication patterns.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RestApiProtocolTest {

    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void shouldHandleCompleteRestLifecycle() {
        String idempotencyKey = "rest-test-" + System.currentTimeMillis();

        // POST - Create resource
        String transactionId = given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "merchantId": "MERCH001",
                    "amount": 100.50,
                    "currency": "USD",
                    "idempotencyKey": "%s"
                }
                """, idempotencyKey))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(201)
            .header("Location", notNullValue())
            .body("transactionId", notNullValue())
            .body("status", equalTo("PENDING"))
            .body("amount", equalTo(100.50f))
        .extract()
            .path("transactionId");

        // GET - Retrieve resource
        given()
        .when()
            .get("/api/v1/transactions/" + transactionId)
        .then()
            .statusCode(200)
            .body("transactionId", equalTo(transactionId))
            .body("merchantId", equalTo("MERCH001"));

        // GET - List resources
        given()
            .queryParam("merchantId", "MERCH001")
            .queryParam("page", 0)
            .queryParam("size", 20)
        .when()
            .get("/api/v1/transactions")
        .then()
            .statusCode(200)
            .body("content", hasSize(greaterThanOrEqualTo(1)))
            .body("page", equalTo(0))
            .body("size", equalTo(20));
    }

    @Test
    void shouldEnforceContentNegotiation() {
        // JSON request
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body("""
                {
                    "merchantId": "MERCH002",
                    "amount": 50.00,
                    "currency": "USD",
                    "idempotencyKey": "content-neg-test"
                }
                """)
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(201)
            .contentType(ContentType.JSON);

        // Unsupported media type
        given()
            .contentType(ContentType.XML)
            .body("<transaction></transaction>")
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(415); // Unsupported Media Type
    }

    @Test
    void shouldHandleHttpMethods() {
        String idempotencyKey = "http-methods-" + System.currentTimeMillis();

        // POST - Create
        String txnId = given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "merchantId": "MERCH003",
                    "amount": 75.00,
                    "currency": "USD",
                    "idempotencyKey": "%s"
                }
                """, idempotencyKey))
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(201)
        .extract()
            .path("transactionId");

        // GET - Read
        given()
        .when()
            .get("/api/v1/transactions/" + txnId)
        .then()
            .statusCode(200);

        // HEAD - Metadata only
        given()
        .when()
            .head("/api/v1/transactions/" + txnId)
        .then()
            .statusCode(200)
            .body(emptyOrNullString());

        // OPTIONS - Discover allowed methods
        given()
        .when()
            .options("/api/v1/transactions")
        .then()
            .statusCode(200);
    }

    @Test
    void shouldHandleErrorResponses() {
        // 400 Bad Request - Invalid input
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "merchantId": "",
                    "amount": -100,
                    "currency": "INVALID"
                }
                """)
        .when()
            .post("/api/v1/transactions")
        .then()
            .statusCode(400);

        // 404 Not Found
        given()
        .when()
            .get("/api/v1/transactions/NONEXISTENT")
        .then()
            .statusCode(404);

        // 409 Conflict - Duplicate idempotency key
        String key = "conflict-test-" + System.currentTimeMillis();
        String body = String.format("""
            {
                "merchantId": "MERCH004",
                "amount": 100.00,
                "currency": "USD",
                "idempotencyKey": "%s"
            }
            """, key);

        given().contentType(ContentType.JSON).body(body)
            .post("/api/v1/transactions")
            .then().statusCode(201);

        given().contentType(ContentType.JSON).body(body)
            .post("/api/v1/transactions")
            .then().statusCode(409);
    }
}
