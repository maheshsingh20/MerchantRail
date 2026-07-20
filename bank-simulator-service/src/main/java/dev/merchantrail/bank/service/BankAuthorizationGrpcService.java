package dev.merchantrail.bank.service;

import dev.merchantrail.bank.grpc.AuthorizationRequest;
import dev.merchantrail.bank.grpc.AuthorizationResponse;
import dev.merchantrail.bank.grpc.BankAuthorizationServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Random;

/**
 * gRPC service simulating bank authorization using simplified ISO 8583.
 * Supports injectable latency and failure for chaos testing.
 */
@GrpcService
public class BankAuthorizationGrpcService extends BankAuthorizationServiceGrpc.BankAuthorizationServiceImplBase {
    
    private static final Logger log = LoggerFactory.getLogger(BankAuthorizationGrpcService.class);
    private static final Random random = new Random();
    
    // Chaos engineering knobs
    private long injectedLatencyMs = 0;
    private double failureRate = 0.0; // 0.0 to 1.0
    
    @Override
    public void authorize(AuthorizationRequest request, 
                         StreamObserver<AuthorizationResponse> responseObserver) {
        
        log.info("Received authorization request: txn={}, amount={}", 
            request.getTransactionId(), request.getAmount());
        
        try {
            // Simulate processing latency
            if (injectedLatencyMs > 0) {
                Thread.sleep(injectedLatencyMs);
            }
            
            // Simulate random failures for chaos testing
            if (failureRate > 0 && random.nextDouble() < failureRate) {
                log.warn("Simulated failure for transaction {}", request.getTransactionId());
                responseObserver.onError(new RuntimeException("Simulated bank failure"));
                return;
            }
            
            // Simple approval logic
            String responseCode = determineResponseCode(request);
            String authCode = responseCode.equals("00") ? generateAuthCode() : "";
            
            AuthorizationResponse response = AuthorizationResponse.newBuilder()
                .setMti("0110") // Response MTI
                .setResponseCode(responseCode)
                .setAuthorizationCode(authCode)
                .setTransactionId(request.getTransactionId())
                .setTimestamp(Instant.now().toString())
                .build();
            
            log.info("Authorization response: txn={}, code={}", 
                request.getTransactionId(), responseCode);
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseObserver.onError(e);
        }
    }
    
    private String determineResponseCode(AuthorizationRequest request) {
        // Validation rules
        if (request.getAmount() <= 0) {
            return "12"; // Invalid transaction
        }
        
        if (request.getAmount() > 500000) { // $5000 in cents
            return "61"; // Exceeds withdrawal limit
        }
        
        // 95% approval rate for valid requests
        if (random.nextDouble() < 0.95) {
            return "00"; // Approved
        } else {
            return "05"; // Do not honor (declined)
        }
    }
    
    private String generateAuthCode() {
        return String.format("%06d", random.nextInt(1000000));
    }
    
    // Methods for chaos testing
    public void setInjectedLatency(long latencyMs) {
        this.injectedLatencyMs = latencyMs;
        log.info("Injected latency set to {}ms", latencyMs);
    }
    
    public void setFailureRate(double rate) {
        this.failureRate = Math.max(0.0, Math.min(1.0, rate));
        log.info("Failure rate set to {}%", failureRate * 100);
    }
    
    public void reset() {
        this.injectedLatencyMs = 0;
        this.failureRate = 0.0;
        log.info("Bank simulator reset to normal operation");
    }
}
