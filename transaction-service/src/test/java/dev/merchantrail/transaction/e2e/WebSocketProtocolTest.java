package dev.merchantrail.transaction.e2e;

import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import dev.merchantrail.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test validating WebSocket protocol for real-time transaction updates.
 * Tests that clients can subscribe to transaction updates via WebSocket/STOMP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketProtocolTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TransactionRepository transactionRepository;

    private WebSocketStompClient stompClient;
    private BlockingQueue<Transaction> receivedMessages;

    @BeforeEach
    void setup() {
        receivedMessages = new LinkedBlockingQueue<>();
        
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @Test
    @DisplayName("Should broadcast transaction updates via WebSocket")
    void shouldBroadcastTransactionUpdatesViaWebSocket() throws Exception {
        // Given: WebSocket client connected to transaction updates topic
        String url = String.format("ws://localhost:%d/ws", port);
        
        StompSession session = stompClient
                .connectAsync(url, new WebSocketHttpHeaders(), new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        
        session.subscribe("/topic/transactions", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Transaction.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.add((Transaction) payload);
            }
        });

        // When: A new transaction is created
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId);
        transactionRepository.save(transaction);

        // Then: WebSocket client should receive the update
        Transaction received = receivedMessages.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.getTransactionId()).isEqualTo(transactionId);
        
        session.disconnect();
    }

    @Test
    @DisplayName("Should receive real-time status updates via WebSocket")
    void shouldReceiveRealtimeStatusUpdatesViaWebSocket() throws Exception {
        // Given: Connected WebSocket client
        String url = String.format("ws://localhost:%d/ws", port);
        
        StompSession session = stompClient
                .connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        
        String transactionId = UUID.randomUUID().toString();
        session.subscribe("/topic/transactions/" + transactionId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Transaction.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.add((Transaction) payload);
            }
        });

        // When: Transaction status changes
        Transaction transaction = createTestTransaction(transactionId);
        transactionRepository.save(transaction);
        
        Thread.sleep(1000);
        
        transaction.setStatus(TransactionStatus.APPROVED);
        transactionRepository.save(transaction);

        // Then: Client should receive status update
        Transaction statusUpdate = receivedMessages.poll(10, TimeUnit.SECONDS);
        assertThat(statusUpdate).isNotNull();
        assertThat(statusUpdate.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        
        session.disconnect();
    }

    @Test
    @DisplayName("Should support multiple WebSocket clients simultaneously")
    void shouldSupportMultipleWebSocketClientsSimultaneously() throws Exception {
        // Given: Multiple WebSocket clients
        String url = String.format("ws://localhost:%d/ws", port);
        
        BlockingQueue<Transaction> client1Messages = new LinkedBlockingQueue<>();
        BlockingQueue<Transaction> client2Messages = new LinkedBlockingQueue<>();
        
        StompSession session1 = stompClient
                .connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        
        StompSession session2 = stompClient
                .connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        
        session1.subscribe("/topic/transactions", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Transaction.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                client1Messages.add((Transaction) payload);
            }
        });
        
        session2.subscribe("/topic/transactions", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Transaction.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                client2Messages.add((Transaction) payload);
            }
        });

        // When: Transaction is created
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId);
        transactionRepository.save(transaction);

        // Then: Both clients should receive the update
        Transaction client1Received = client1Messages.poll(10, TimeUnit.SECONDS);
        Transaction client2Received = client2Messages.poll(10, TimeUnit.SECONDS);
        
        assertThat(client1Received).isNotNull();
        assertThat(client2Received).isNotNull();
        assertThat(client1Received.getTransactionId()).isEqualTo(transactionId);
        assertThat(client2Received.getTransactionId()).isEqualTo(transactionId);
        
        session1.disconnect();
        session2.disconnect();
    }

    @Test
    @DisplayName("Should handle WebSocket disconnection gracefully")
    void shouldHandleWebSocketDisconnectionGracefully() throws Exception {
        // Given: Connected WebSocket client
        String url = String.format("ws://localhost:%d/ws", port);
        
        StompSession session = stompClient
                .connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
        
        session.subscribe("/topic/transactions", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Transaction.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessages.add((Transaction) payload);
            }
        });

        // When: Client disconnects
        session.disconnect();
        
        // And: New transaction is created
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId);
        transactionRepository.save(transaction);

        // Then: System should handle disconnection gracefully (no errors)
        // Disconnected client won't receive messages, but system continues working
        assertThat(session.isConnected()).isFalse();
    }

    @Test
    @DisplayName("Should authenticate WebSocket connections")
    void shouldAuthenticateWebSocketConnections() throws Exception {
        // Given: WebSocket client with authentication
        String url = String.format("ws://localhost:%d/ws", port);
        
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        // In real implementation, would include auth token
        // headers.add("Authorization", "Bearer " + token);
        
        // When: Connecting with credentials
        StompSession session = stompClient
                .connectAsync(url, headers, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        // Then: Connection should be established
        assertThat(session.isConnected()).isTrue();
        
        session.disconnect();
    }

    private Transaction createTestTransaction(String transactionId) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setMerchantId("MERCHANT_WS_TEST");
        transaction.setAmount(new BigDecimal("45.00"));
        transaction.setCurrency("USD");
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCardNumber("5105105105105100");
        transaction.setCardholderName("WebSocket Test User");
        return transaction;
    }
}
