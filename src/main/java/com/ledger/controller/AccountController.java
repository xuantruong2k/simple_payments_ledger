package com.ledger.controller;

import com.ledger.handler.AccountHandler;
import io.javalin.Javalin;

public class AccountController {
    private final AccountHandler accountHandler;

    public AccountController(AccountHandler accountHandler) {
        this.accountHandler = accountHandler;
    }

    public void registerRoutes(Javalin app) {
        app.post("/accounts", accountHandler::createAccount);
        app.get("/accounts/{account_id}", accountHandler::getAccount);
    }
}
