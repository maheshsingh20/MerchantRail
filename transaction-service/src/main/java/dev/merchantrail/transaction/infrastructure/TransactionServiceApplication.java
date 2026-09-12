package dev.merchantrail.transaction.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Spring Boot application class for Transaction Service.
 */
@SpringBootApplication(scanBasePackages = "dev.merchantrail.transaction")
@EntityScan(basePackages = "dev.merchantrail.transaction")
@EnableJpaRepositories(basePackages = "dev.merchantrail.transaction")
public class TransactionServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
