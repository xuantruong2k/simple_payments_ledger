package com.ledger.middleware;

import com.ledger.repository.AccountRepository;
import com.ledger.transaction.TransferContext;

/**
 * Middleware to load and verify account existence.
 * Loads both sender and receiver accounts from the repository.
 */
public class AccountLoadingMiddleware implements TransferMiddleware {
    private final AccountRepository accountRepository;

    public AccountLoadingMiddleware(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public void process(TransferContext context, Runnable next) throws Exception {
        // Load from account
        context.setFromAccount(
            accountRepository.findById(context.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "From account not found: " + context.getFromAccountId()))
        );

        // Load to account
        context.setToAccount(
            accountRepository.findById(context.getToAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "To account not found: " + context.getToAccountId()))
        );

        // Proceed to next middleware
        next.run();
    }
}
