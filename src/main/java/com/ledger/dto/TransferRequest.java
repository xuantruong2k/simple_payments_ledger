package com.ledger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class TransferRequest {
    @JsonProperty("from_account_id")
    private String fromAccountId;
    
    @JsonProperty("to_account_id")
    private String toAccountId;
    
    private BigDecimal amount;

    public TransferRequest() {
    }

    public TransferRequest(String fromAccountId, String toAccountId, BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
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
}
