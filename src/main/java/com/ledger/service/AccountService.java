package com.ledger.service;

import com.ledger.model.Account;
import com.ledger.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class AccountService {
    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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

    public synchronized TransferResult transfer(String fromAccountId, String toAccountId, BigDecimal amount) {
        if (fromAccountId == null || fromAccountId.trim().isEmpty()) {
            throw new IllegalArgumentException("From account ID is required");
        }
        if (toAccountId == null || toAccountId.trim().isEmpty()) {
            throw new IllegalArgumentException("To account ID is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("From account not found: " + fromAccountId));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new IllegalArgumentException("To account not found: " + toAccountId));

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account: " + fromAccountId);
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        return new TransferResult(fromAccount, toAccount, amount);
    }

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

