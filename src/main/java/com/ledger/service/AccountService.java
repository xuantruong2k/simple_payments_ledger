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
}
