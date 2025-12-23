package com.ledger.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class CreateAccountRequest {
    private String id;
    
    @JsonProperty("initial_balance")
    private BigDecimal initialBalance;

    public CreateAccountRequest() {
    }

    public CreateAccountRequest(String id, BigDecimal initialBalance) {
        this.id = id;
        this.initialBalance = initialBalance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance != null ? initialBalance : BigDecimal.ZERO;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }
}
