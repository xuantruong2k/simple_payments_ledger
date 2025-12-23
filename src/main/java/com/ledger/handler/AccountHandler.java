package com.ledger.handler;

import com.ledger.dto.AccountResponse;
import com.ledger.dto.CreateAccountRequest;
import com.ledger.dto.ErrorResponse;
import com.ledger.model.Account;
import com.ledger.service.AccountService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class AccountHandler {
    private final AccountService accountService;

    public AccountHandler(AccountService accountService) {
        this.accountService = accountService;
    }

    public void createAccount(Context ctx) {
        try {
            CreateAccountRequest request = ctx.bodyAsClass(CreateAccountRequest.class);
            
            if (request.getId() == null || request.getId().trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("BAD_REQUEST", "Account ID is required"));
                return;
            }

            Account account = accountService.createAccount(
                request.getId(),
                request.getInitialBalance()
            );

            ctx.status(HttpStatus.CREATED)
               .json(AccountResponse.fromAccount(account));
               
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT)
               .json(new ErrorResponse("CONFLICT", e.getMessage()));
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }

    public void getAccount(Context ctx) {
        try {
            String accountId = ctx.pathParam("account_id");
            
            Account account = accountService.getAccount(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

            ctx.status(HttpStatus.OK)
               .json(AccountResponse.fromAccount(account));
               
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.NOT_FOUND)
               .json(new ErrorResponse("NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }
}
