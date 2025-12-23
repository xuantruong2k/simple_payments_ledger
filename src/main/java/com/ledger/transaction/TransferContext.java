package com.ledger.transaction;

import com.ledger.model.Account;

import java.math.BigDecimal;

/**
 * Represents the context of a transfer transaction.
 * Contains all information needed for the transfer and any middleware processing.
 */
public class TransferContext {
    private final String fromAccountId;
    private final String toAccountId;
    private final BigDecimal amount;
    
    private Account fromAccount;
    private Account toAccount;
    private BigDecimal effectiveAmount;
    private BigDecimal fee;
    
    public TransferContext(String fromAccountId, String toAccountId, BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.effectiveAmount = amount;
        this.fee = BigDecimal.ZERO;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Account getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(Account fromAccount) {
        this.fromAccount = fromAccount;
    }

    public Account getToAccount() {
        return toAccount;
    }

    public void setToAccount(Account toAccount) {
        this.toAccount = toAccount;
    }

    public BigDecimal getEffectiveAmount() {
        return effectiveAmount;
    }

    public void setEffectiveAmount(BigDecimal effectiveAmount) {
        this.effectiveAmount = effectiveAmount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getTotalDebit() {
        return effectiveAmount.add(fee);
    }
}
