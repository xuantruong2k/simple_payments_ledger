package com.ledger.transaction;

import com.ledger.middleware.TransferMiddleware;
import com.ledger.repository.AccountRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Executor that processes a transfer through a chain of middleware
 * and executes the final transfer operation.
 */
public class TransferExecutor {
    private final List<TransferMiddleware> middlewares;
    private final AccountRepository accountRepository;

    public TransferExecutor(AccountRepository accountRepository, List<TransferMiddleware> middlewares) {
        this.accountRepository = accountRepository;
        this.middlewares = new ArrayList<>(middlewares);
    }

    /**
     * Execute the transfer with all middleware processing.
     */
    public TransferResult execute(TransferContext context) throws Exception {
        // Build middleware chain
        Runnable chain = buildChain(context, 0);
        
        // Execute the chain
        chain.run();
        
        // Return result
        return new TransferResult(
            context.getFromAccount(),
            context.getToAccount(),
            context.getEffectiveAmount(),
            context.getFee()
        );
    }

    /**
     * Recursively build the middleware chain.
     */
    private Runnable buildChain(TransferContext context, int index) {
        if (index >= middlewares.size()) {
            // End of chain - execute the actual transfer
            return () -> executeTransfer(context);
        }

        TransferMiddleware middleware = middlewares.get(index);
        Runnable next = buildChain(context, index + 1);

        return () -> {
            try {
                middleware.process(context, next);
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException("Transfer processing failed", e);
            }
        };
    }

    /**
     * Execute the actual transfer operation (core business logic).
     * This is called after all middleware have processed.
     */
    private void executeTransfer(TransferContext context) {
        // Debit sender (amount + fee)
        context.getFromAccount().setBalance(
            context.getFromAccount().getBalance().subtract(context.getTotalDebit())
        );

        // Credit receiver (only the effective amount, not fees)
        context.getToAccount().setBalance(
            context.getToAccount().getBalance().add(context.getEffectiveAmount())
        );

        // Save both accounts
        accountRepository.save(context.getFromAccount());
        accountRepository.save(context.getToAccount());
    }

    /**
     * Result of a transfer operation.
     */
    public static class TransferResult {
        private final com.ledger.model.Account fromAccount;
        private final com.ledger.model.Account toAccount;
        private final java.math.BigDecimal amount;
        private final java.math.BigDecimal fee;

        public TransferResult(com.ledger.model.Account fromAccount, 
                            com.ledger.model.Account toAccount,
                            java.math.BigDecimal amount,
                            java.math.BigDecimal fee) {
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.amount = amount;
            this.fee = fee;
        }

        public com.ledger.model.Account getFromAccount() {
            return fromAccount;
        }

        public com.ledger.model.Account getToAccount() {
            return toAccount;
        }

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public java.math.BigDecimal getFee() {
            return fee;
        }
    }
}
