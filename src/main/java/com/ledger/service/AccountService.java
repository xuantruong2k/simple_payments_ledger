package com.ledger.service;

import com.ledger.locking.AccountLockManager;
import com.ledger.model.Account;
import com.ledger.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class AccountService {
    private final AccountRepository accountRepository;
    private final TransferService transferService;
    private final AccountLockManager lockManager;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.lockManager = new AccountLockManager();

        // Share the same lock manager with TransferService for coordination
        this.transferService = new TransferService(
            accountRepository,
            Arrays.asList(
                new com.ledger.middleware.TransferValidationMiddleware(),
                new com.ledger.middleware.AccountLoadingMiddleware(accountRepository),
                new com.ledger.middleware.TransactionFeeMiddleware(),
                new com.ledger.middleware.SufficientFundsMiddleware()
            ),
            this.lockManager
        );
    }

    /**
     * Create a new account with initial balance.
     * Thread-safe: Uses fine-grained locking per account ID.
     */
    public Account createAccount(String id, BigDecimal initialBalance) {
        ReentrantLock lock = lockManager.getLock(id);
        lock.lock();
        try {
            if (accountRepository.existsById(id)) {
                throw new IllegalStateException("Account with ID " + id + " already exists");
            }
            Account account = new Account(id, initialBalance);
            return accountRepository.save(account);
        } finally {
            lock.unlock();
        }
    }

    public Optional<Account> getAccount(String id) {
        return accountRepository.findById(id);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Update account balance.
     * Thread-safe: Uses fine-grained locking per account ID.
     * 
     * @param id Account ID
     * @param newBalance New balance (must be non-negative)
     * @return Updated account
     * @throws IllegalArgumentException if account not found or balance is negative
     */
    public Account updateBalance(String id, BigDecimal newBalance) {
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative: " + newBalance);
        }
        
        ReentrantLock lock = lockManager.getLock(id);
        lock.lock();
        try {
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
            account.setBalance(newBalance);
            return accountRepository.save(account);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add to account balance atomically.
     * Thread-safe: Performs read-modify-write under lock.
     * Use this for concurrent balance updates instead of read-then-updateBalance().
     * 
     * @param id Account ID
     * @param amount Amount to add (can be negative to subtract)
     * @return Updated account
     * @throws IllegalArgumentException if account not found
     * @throws InsufficientFundsException if resulting balance would be negative
     */
    public Account addToBalance(String id, BigDecimal amount) {
        ReentrantLock lock = lockManager.getLock(id);
        lock.lock();
        try {
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
            BigDecimal newBalance = account.getBalance().add(amount);
            
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new InsufficientFundsException(
                    "Insufficient funds in account " + id + 
                    ". Current: " + account.getBalance() + 
                    ", Requested: " + amount.abs() + 
                    ", Would result in: " + newBalance
                );
            }
            
            account.setBalance(newBalance);
            return accountRepository.save(account);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Delete an account.
     * Thread-safe: Uses fine-grained locking per account ID.
     */
    public void deleteAccount(String id) {
        ReentrantLock lock = lockManager.getLock(id);
        lock.lock();
        try {
            accountRepository.deleteById(id);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the lock manager (for testing/monitoring).
     */
    public AccountLockManager getLockManager() {
        return lockManager;
    }

    /**
     * Transfer money between accounts using the new middleware-based TransferService.
     * This method delegates to TransferService for extensibility.
     * Thread-safe: Uses shared lock manager with fine-grained locking.
     */
    public TransferResult transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        TransferService.TransferResult result = transferService.transfer(fromAccountId, toAccountId, amount);

        // Convert to legacy TransferResult for backward compatibility
        return new TransferResult(
            result.getFromAccount(),
            result.getToAccount(),
            result.getAmount()
        );
    }

    /**
     * Legacy TransferResult class for backward compatibility.
     * Does not include fee information.
     */
    public static class TransferResult {
        private final Account fromAccount;
        private final Account toAccount;
        private final BigDecimal amount;

        public TransferResult(Account fromAccount, Account toAccount, BigDecimal amount) {
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.amount = amount;
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
    }

    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }
}

