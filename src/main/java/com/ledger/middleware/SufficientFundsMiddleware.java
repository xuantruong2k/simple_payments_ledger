package com.ledger.middleware;

import com.ledger.service.AccountService;
import com.ledger.transaction.TransferContext;

/**
 * Middleware to check if sender has sufficient funds for the transfer.
 * Considers both the transfer amount and any fees.
 */
public class SufficientFundsMiddleware implements TransferMiddleware {

    @Override
    public void process(TransferContext context, Runnable next) throws Exception {
        // Check if sender has enough funds (amount + fees)
        if (context.getFromAccount().getBalance().compareTo(context.getTotalDebit()) < 0) {
            throw new AccountService.InsufficientFundsException(
                "Insufficient funds in account: " + context.getFromAccountId());
        }

        // Proceed to next middleware
        next.run();
    }
}
