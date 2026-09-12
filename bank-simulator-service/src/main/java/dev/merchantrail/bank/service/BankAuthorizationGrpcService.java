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
 * gRPC service simulating multi-issuer bank authorizations using ISO 8583 message fields.
 * Models card network switching endpoints (Citibank, Chase, Barclays, HDFC).
 * Supports injectable latency and failure for chaos testing and STIP verification.
 */
@GrpcService
public class BankAuthorizationGrpcService extends BankAuthorizationServiceGrpc.BankAuthorizationServiceImplBase {
    
    private static final Logger log = LoggerFactory.getLogger(BankAuthorizationGrpcService.class);
    private static final Random random = new Random();
    
    // Chaos engineering & STIP verification knobs
    private long injectedLatencyMs = 0;
    private double failureRate = 0.0; // 0.0 to 1.0
    private boolean simulateIssuerTimeout = false;
    
    @Override
    public void authorize(AuthorizationRequest request, 
                         StreamObserver<AuthorizationResponse> responseObserver) {
        
        String pan = request.getPan();
        String bin = (pan != null && pan.length() >= 6) ? pan.substring(0, 6) : "UNKNOWN";
        String issuer = resolveIssuerName(bin);

        log.info("Received ISO 8583 auth request (0100): txn={}, pan={}, bin={}, issuer={}, amount={}", 
            request.getTransactionId(), maskPan(pan), bin, issuer, request.getAmount());
        
        try {
            // Simulate processing latency or network delays
            if (injectedLatencyMs > 0) {
                Thread.sleep(injectedLatencyMs);
            }

            // Simulate issuer link timeout for STIP testing
            if (simulateIssuerTimeout) {
                log.warn("Simulated network timeout connecting to issuer {}", issuer);
                responseObserver.onError(new RuntimeException("Issuer connection timeout (SLA > 2000ms)"));
                return;
            }
            
            // Simulate random failures for chaos testing
            if (failureRate > 0 && random.nextDouble() < failureRate) {
                log.warn("Simulated failure for transaction {} at issuer {}", request.getTransactionId(), issuer);
                responseObserver.onError(new RuntimeException("Simulated bank failure from " + issuer));
                return;
            }
            
            // Approval decision logic
            String responseCode = determineResponseCode(request);
            String authCode = responseCode.equals("00") ? generateAuthCode() : "";
            
            AuthorizationResponse response = AuthorizationResponse.newBuilder()
                .setMti("0110") // ISO 8583 Authorization Response MTI
                .setResponseCode(responseCode)
                .setAuthorizationCode(authCode)
                .setTransactionId(request.getTransactionId())
                .setTimestamp(Instant.now().toString())
                .build();
            
            log.info("Dispatched ISO 8583 auth response (0110): txn={}, issuer={}, code={}, authCode={}", 
                request.getTransactionId(), issuer, responseCode, authCode);
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseObserver.onError(e);
        }
    }
    
    private String determineResponseCode(AuthorizationRequest request) {
        if (request.getAmount() <= 0) {
            return "12"; // Invalid transaction
        }
        
        if (request.getAmount() > 500000) { // $5,000 limit
            return "61"; // Exceeds withdrawal limit
        }
        
        // 96% approval rate for valid switching requests
        if (random.nextDouble() < 0.96) {
            return "00"; // Approved
        } else {
            return "05"; // Do not honor (declined)
        }
    }
    
    private String generateAuthCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    private String resolveIssuerName(String bin) {
        if (bin.startsWith("51")) return "Citibank NA";
        if (bin.startsWith("52")) return "JPMorgan Chase";
        if (bin.startsWith("53")) return "Barclays Bank";
        if (bin.startsWith("54")) return "HSBC Global";
        if (bin.startsWith("55")) return "HDFC Bank";
        if (bin.startsWith("4")) return "Visa Net Issuer";
        return "Generic Issuer";
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 10) return "******";
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
    
    // Methods for chaos testing & STIP validation
    public void setInjectedLatency(long latencyMs) {
        this.injectedLatencyMs = latencyMs;
        log.info("Injected latency set to {}ms", latencyMs);
    }
    
    public void setFailureRate(double rate) {
        this.failureRate = Math.max(0.0, Math.min(1.0, rate));
        log.info("Failure rate set to {}%", failureRate * 100);
    }

    public void setSimulateIssuerTimeout(boolean simulateIssuerTimeout) {
        this.simulateIssuerTimeout = simulateIssuerTimeout;
        log.info("Simulate issuer timeout set to {}", simulateIssuerTimeout);
    }
    
    public void reset() {
        this.injectedLatencyMs = 0;
        this.failureRate = 0.0;
        this.simulateIssuerTimeout = false;
        log.info("Bank simulator reset to normal operation");
    }
}
