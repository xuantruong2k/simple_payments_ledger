package com.ledger.service;

import com.ledger.model.Account;
import com.ledger.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class AccountService {
    private final AccountRepository accountRepository;
    private final TransferService transferService;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
        this.transferService = new TransferService(accountRepository);
    }

    public Account createAccount(String id, BigDecimal initialBalance) {
        if (accountRepository.existsById(id)) {
            throw new IllegalStateException("Account with ID " + id + " already exists");
        }
        Account account = new Account(id, initialBalance);
        return accountRepository.save(account);
    }

    public Optional<Account> getAccount(String id) {
        return accountRepository.findById(id);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account updateBalance(String id, BigDecimal newBalance) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        account.setBalance(newBalance);
        return accountRepository.save(account);
    }

    public void deleteAccount(String id) {
        accountRepository.deleteById(id);
    }

    /**
     * Transfer money between accounts using the new middleware-based TransferService.
     * This method delegates to TransferService for extensibility.
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

