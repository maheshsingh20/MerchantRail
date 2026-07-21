package dev.merchantrail.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ledger Service - Double-entry bookkeeping and settlement
 */
@SpringBootApplication
public class LedgerServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
}
