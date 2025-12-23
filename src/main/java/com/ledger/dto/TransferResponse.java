package com.ledger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class TransferResponse {
    @JsonProperty("from_account_id")
    private String fromAccountId;
    
    @JsonProperty("to_account_id")
    private String toAccountId;
    
    private BigDecimal amount;
    
    @JsonProperty("from_account_balance")
    private BigDecimal fromAccountBalance;
    
    @JsonProperty("to_account_balance")
    private BigDecimal toAccountBalance;

    public TransferResponse() {
    }

    public TransferResponse(String fromAccountId, String toAccountId, BigDecimal amount, 
                           BigDecimal fromAccountBalance, BigDecimal toAccountBalance) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.fromAccountBalance = fromAccountBalance;
        this.toAccountBalance = toAccountBalance;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(String fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(String toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFromAccountBalance() {
        return fromAccountBalance;
    }

    public void setFromAccountBalance(BigDecimal fromAccountBalance) {
        this.fromAccountBalance = fromAccountBalance;
    }

    public BigDecimal getToAccountBalance() {
        return toAccountBalance;
    }

    public void setToAccountBalance(BigDecimal toAccountBalance) {
        this.toAccountBalance = toAccountBalance;
    }
}
