package com.ledger.handler;

import com.ledger.dto.ErrorResponse;
import com.ledger.dto.TransferRequest;
import com.ledger.dto.TransferResponse;
import com.ledger.service.AccountService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class TransactionHandler {
    private final AccountService accountService;

    public TransactionHandler(AccountService accountService) {
        this.accountService = accountService;
    }

    public void transfer(Context ctx) {
        try {
            TransferRequest request = ctx.bodyAsClass(TransferRequest.class);

            if (request.getFromAccountId() == null || request.getFromAccountId().trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("BAD_REQUEST", "from_account_id is required"));
                return;
            }

            if (request.getToAccountId() == null || request.getToAccountId().trim().isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("BAD_REQUEST", "to_account_id is required"));
                return;
            }

            if (request.getAmount() == null) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse("BAD_REQUEST", "amount is required"));
                return;
            }

            AccountService.TransferResult result = accountService.transfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount()
            );

            TransferResponse response = new TransferResponse(
                result.getFromAccount().getId(),
                result.getToAccount().getId(),
                result.getAmount(),
                result.getFromAccount().getBalance(),
                result.getToAccount().getBalance()
            );

            ctx.status(HttpStatus.OK)
               .json(response);

        } catch (AccountService.InsufficientFundsException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("INSUFFICIENT_FUNDS", e.getMessage()));
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
        }
    }
}
