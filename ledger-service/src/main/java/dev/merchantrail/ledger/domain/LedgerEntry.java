package dev.merchantrail.ledger.domain;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Double-entry bookkeeping ledger entry.
 * Every transaction creates two balanced entries (debit + credit).
 */
public class LedgerEntry {
    
    private final String entryId;
    private final TransactionId transactionId;
    private final MerchantId merchantId;
    private final EntryType entryType;
    private final Money amount;
    private final String account;
    private final Instant createdAt;
    private LedgerEntryStatus status;
    
    private LedgerEntry(String entryId, TransactionId transactionId, MerchantId merchantId,
                       EntryType entryType, Money amount, String account, 
                       Instant createdAt, LedgerEntryStatus status) {
        this.entryId = entryId;
        this.transactionId = transactionId;
        this.merchantId = merchantId;
        this.entryType = entryType;
        this.amount = amount;
        this.account = account;
        this.createdAt = createdAt;
        this.status = status;
    }
    
    public static LedgerEntry createDebit(TransactionId transactionId, MerchantId merchantId,
                                         Money amount, String account) {
        validateAmount(amount);
        return new LedgerEntry(
            UUID.randomUUID().toString(),
            transactionId,
            merchantId,
            EntryType.DEBIT,
            amount,
            account,
            Instant.now(),
            LedgerEntryStatus.PENDING
        );
    }
    
    public static LedgerEntry createCredit(TransactionId transactionId, MerchantId merchantId,
                                          Money amount, String account) {
        validateAmount(amount);
        return new LedgerEntry(
            UUID.randomUUID().toString(),
            transactionId,
            merchantId,
            EntryType.CREDIT,
            amount,
            account,
            Instant.now(),
            LedgerEntryStatus.PENDING
        );
    }
    
    public static LedgerEntry reconstitute(String entryId, TransactionId transactionId,
                                          MerchantId merchantId, EntryType entryType,
                                          Money amount, String account,
                                          Instant createdAt, LedgerEntryStatus status) {
        return new LedgerEntry(entryId, transactionId, merchantId, entryType,
                             amount, account, createdAt, status);
    }
    
    public void settle() {
        if (status != LedgerEntryStatus.PENDING) {
            throw new IllegalStateException("Can only settle PENDING entries");
        }
        this.status = LedgerEntryStatus.SETTLED;
    }
    
    public void reverse() {
        this.status = LedgerEntryStatus.REVERSED;
    }
    
    private static void validateAmount(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Ledger entry amount must be positive");
        }
    }
    
    public long getSignedAmount() {
        long minorUnits = amount.getMinorUnits();
        return entryType == EntryType.DEBIT ? -minorUnits : minorUnits;
    }
    
    // Getters
    public String getEntryId() { return entryId; }
    public TransactionId getTransactionId() { return transactionId; }
    public MerchantId getMerchantId() { return merchantId; }
    public EntryType getEntryType() { return entryType; }
    public Money getAmount() { return amount; }
    public String getAccount() { return account; }
    public Instant getCreatedAt() { return createdAt; }
    public LedgerEntryStatus getStatus() { return status; }
    
    public boolean isPending() { return status == LedgerEntryStatus.PENDING; }
    public boolean isSettled() { return status == LedgerEntryStatus.SETTLED; }
    public boolean isReversed() { return status == LedgerEntryStatus.REVERSED; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LedgerEntry that = (LedgerEntry) o;
        return entryId.equals(that.entryId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(entryId);
    }
}
