package dev.merchantrail.transaction.adapter.in.web;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.application.port.in.GetTransactionQuery;
import dev.merchantrail.transaction.application.port.in.GetTransactionsByMerchantQuery;
import dev.merchantrail.transaction.application.port.in.SubmitTransactionCommand;
import dev.merchantrail.transaction.application.usecase.GetTransactionUseCase;
import dev.merchantrail.transaction.application.usecase.GetTransactionsByMerchantUseCase;
import dev.merchantrail.transaction.application.usecase.SubmitTransactionUseCase;
import dev.merchantrail.transaction.domain.Transaction;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for transaction operations.
 * Thin adapter - delegates to use cases.
 */
@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    
    private final SubmitTransactionUseCase submitTransactionUseCase;
    private final GetTransactionUseCase getTransactionUseCase;
    private final GetTransactionsByMerchantUseCase getTransactionsByMerchantUseCase;
    
    public TransactionController(SubmitTransactionUseCase submitTransactionUseCase,
                                GetTransactionUseCase getTransactionUseCase,
                                GetTransactionsByMerchantUseCase getTransactionsByMerchantUseCase) {
        this.submitTransactionUseCase = submitTransactionUseCase;
        this.getTransactionUseCase = getTransactionUseCase;
        this.getTransactionsByMerchantUseCase = getTransactionsByMerchantUseCase;
    }
    
    @PostMapping
    public ResponseEntity<TransactionResponse> submitTransaction(
            @Valid @RequestBody SubmitTransactionRequest request) {
        
        SubmitTransactionCommand command = request.toCommand();
        Transaction transaction = submitTransactionUseCase.execute(command);
        TransactionResponse response = TransactionResponse.fromDomain(transaction);
        
        URI location = URI.create("/api/v1/transactions/" + transaction.getTransactionId().getValue());
        return ResponseEntity.created(location).body(response);
    }
    
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable String transactionId) {
        
        GetTransactionQuery query = new GetTransactionQuery(TransactionId.of(transactionId));
        Transaction transaction = getTransactionUseCase.execute(query);
        TransactionResponse response = TransactionResponse.fromDomain(transaction);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactionsByMerchant(
            @RequestParam String merchantId) {
        
        GetTransactionsByMerchantQuery query = 
            new GetTransactionsByMerchantQuery(MerchantId.of(merchantId));
        List<Transaction> transactions = getTransactionsByMerchantUseCase.execute(query);
        List<TransactionResponse> responses = transactions.stream()
            .map(TransactionResponse::fromDomain)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    @ExceptionHandler(GetTransactionUseCase.TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(
            GetTransactionUseCase.TransactionNotFoundException ex) {
        ErrorResponse error = new ErrorResponse("TRANSACTION_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
