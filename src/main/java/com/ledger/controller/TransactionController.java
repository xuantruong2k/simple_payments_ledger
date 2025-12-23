package com.ledger.controller;

import com.ledger.handler.TransactionHandler;
import io.javalin.Javalin;

public class TransactionController {
    private final TransactionHandler transactionHandler;

    public TransactionController(TransactionHandler transactionHandler) {
        this.transactionHandler = transactionHandler;
    }

    public void registerRoutes(Javalin app) {
        app.post("/transactions", transactionHandler::transfer);
    }
}
