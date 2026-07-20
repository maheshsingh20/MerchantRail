package dev.merchantrail.transaction.application.usecase;

import dev.merchantrail.transaction.application.port.in.GetTransactionsByMerchantQuery;
import dev.merchantrail.transaction.application.port.out.TransactionRepository;
import dev.merchantrail.transaction.domain.Transaction;

import java.util.List;

/**
 * Use case for retrieving all transactions for a merchant.
 */
public class GetTransactionsByMerchantUseCase {
    
    private final TransactionRepository transactionRepository;
    
    public GetTransactionsByMerchantUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    public List<Transaction> execute(GetTransactionsByMerchantQuery query) {
        return transactionRepository.findByMerchantId(query.merchantId());
    }
}
