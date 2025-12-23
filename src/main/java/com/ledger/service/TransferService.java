package com.ledger.service;

import com.ledger.middleware.*;
import com.ledger.model.Account;
import com.ledger.repository.AccountRepository;
import com.ledger.transaction.TransferContext;
import com.ledger.transaction.TransferExecutor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Service for handling money transfers between accounts.
 * Uses a middleware pattern to allow easy extension with additional features
 * like transaction fees, logging, fraud detection, etc.
 */
public class TransferService {
    private final TransferExecutor transferExecutor;

    public TransferService(AccountRepository accountRepository) {
        // Configure the middleware chain
        // Order matters: validation → loading → fee calculation → funds check → execution
        List<TransferMiddleware> middlewares = Arrays.asList(
            new TransferValidationMiddleware(),
            new AccountLoadingMiddleware(accountRepository),
            new TransactionFeeMiddleware(),
            new SufficientFundsMiddleware()
        );

        this.transferExecutor = new TransferExecutor(accountRepository, middlewares);
    }

    /**
     * Constructor with custom middleware chain for testing or custom configurations.
     */
    public TransferService(AccountRepository accountRepository, List<TransferMiddleware> middlewares) {
        this.transferExecutor = new TransferExecutor(accountRepository, middlewares);
    }

    /**
     * Transfer money from one account to another.
     * The transfer goes through the configured middleware chain.
     *
     * @param fromAccountId Source account ID
     * @param toAccountId Destination account ID
     * @param amount Amount to transfer
     * @return TransferResult containing updated accounts and transfer details
     */
    public synchronized TransferResult transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        try {
            TransferContext context = new TransferContext(fromAccountId, toAccountId, amount);
            TransferExecutor.TransferResult executorResult = transferExecutor.execute(context);
            
            return new TransferResult(
                executorResult.getFromAccount(),
                executorResult.getToAccount(),
                executorResult.getAmount(),
                executorResult.getFee()
            );
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Transfer failed", e);
        }
    }

    /**
     * Result of a transfer operation.
     */
    public static class TransferResult {
        private final Account fromAccount;
        private final Account toAccount;
        private final BigDecimal amount;
        private final BigDecimal fee;

        public TransferResult(Account fromAccount, Account toAccount, BigDecimal amount, BigDecimal fee) {
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.amount = amount;
            this.fee = fee;
        }

        public Account getFromAccount() {
            return fromAccount;
        }

        public Account getToAccount() {
            return toAccount;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BigDecimal getFee() {
            return fee;
        }
    }
}
