package dev.merchantrail.transaction.chaos;

import dev.merchantrail.transaction.application.port.out.TransactionRepository;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for chaos engineering tests using Toxiproxy.
 * 
 * Toxiproxy sits between the application and infrastructure services,
 * allowing injection of network failures, latency, and other toxics.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class ChaosTestBase {

    @Autowired
    protected TransactionRepository transactionRepository;

    protected static final Network network = Network.newNetwork();

    @Container
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withNetwork(network)
            .withNetworkAliases("postgres");

    @Container
    protected static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withNetwork(network)
            .withNetworkAliases("redis")
            .withExposedPorts(6379);

    @Container
    protected static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withNetwork(network)
            .withNetworkAliases("kafka");

    @Container
    protected static final ToxiproxyContainer toxiproxy = new ToxiproxyContainer(
            "ghcr.io/shopify/toxiproxy:2.5.0")
            .withNetwork(network);

    protected static Proxy dbProxy;
    protected static Proxy redisProxy;
    protected static Proxy kafkaProxy;
    protected ToxiproxyClient toxiproxyClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure proxied connections
        registry.add("spring.datasource.url", 
            () -> "jdbc:postgresql://" + toxiproxy.getHost() + ":" 
                + dbProxy.getOriginalProxyPort() + "/test");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.redis.host", toxiproxy::getHost);
        registry.add("spring.redis.port", () -> redisProxy.getOriginalProxyPort());

        registry.add("spring.kafka.bootstrap-servers", 
            () -> toxiproxy.getHost() + ":" + kafkaProxy.getOriginalProxyPort());
    }

    @BeforeEach
    void setupProxies() throws Exception {
        toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());

        // Create proxies for Postgres, Redis, and Kafka
        dbProxy = toxiproxyClient.createProxy("postgres", 
            "0.0.0.0:8666", "postgres:5432");
        redisProxy = toxiproxyClient.createProxy("redis", 
            "0.0.0.0:8667", "redis:6379");
        kafkaProxy = toxiproxyClient.createProxy("kafka",
            "0.0.0.0:8668", "kafka:9093");
    }

    @AfterEach
    void cleanupProxies() throws Exception {
        if (dbProxy != null) {
            dbProxy.delete();
        }
        if (redisProxy != null) {
            redisProxy.delete();
        }
        if (kafkaProxy != null) {
            kafkaProxy.delete();
        }
    }

    /**
     * Inject latency into database connections
     */
    protected void injectDatabaseLatency(long milliseconds) throws Exception {
        dbProxy.toxics()
            .latency("db-latency", ToxicDirection.DOWNSTREAM, milliseconds)
            .setJitter(100);
    }

    /**
     * Inject latency into Redis connections
     */
    protected void injectRedisLatency(long milliseconds) throws Exception {
        redisProxy.toxics()
            .latency("redis-latency", ToxicDirection.DOWNSTREAM, milliseconds);
    }

    /**
     * Simulate complete network partition (connection reset)
     */
    protected void simulateDatabasePartition() throws Exception {
        dbProxy.toxics()
            .resetPeer("db-reset", ToxicDirection.DOWNSTREAM, 1000);
    }

    /**
     * Simulate bandwidth limitation
     */
    protected void limitDatabaseBandwidth(long kilobytesPerSecond) throws Exception {
        dbProxy.toxics()
            .bandwidth("db-bandwidth", ToxicDirection.DOWNSTREAM, kilobytesPerSecond);
    }

    /**
     * Simulate connection timeout by adding extreme latency
     */
    protected void simulateDatabaseTimeout() throws Exception {
        injectDatabaseLatency(30000); // 30 second latency = timeout
    }
}
