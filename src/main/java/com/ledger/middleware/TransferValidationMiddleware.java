package com.ledger.middleware;

import com.ledger.transaction.TransferContext;

import java.math.BigDecimal;

/**
 * Validation middleware for transfer requests.
 * Validates all transfer parameters before execution.
 */
public class TransferValidationMiddleware implements TransferMiddleware {

    @Override
    public void process(TransferContext context, Runnable next) throws Exception {
        // Validate from account ID
        if (context.getFromAccountId() == null || context.getFromAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("From account ID is required");
        }

        // Validate to account ID
        if (context.getToAccountId() == null || context.getToAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("To account ID is required");
        }

        // Validate amount
        if (context.getAmount() == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        if (context.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Validate not same account
        if (context.getFromAccountId().equals(context.getToAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Proceed to next middleware
        next.run();
    }
}
