package com.ledger.repository;

import com.ledger.model.Account;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAccountRepository implements AccountRepository {
    private final Map<String, Account> storage = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        storage.put(account.getId(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Account> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean existsById(String id) {
        return id != null && storage.containsKey(id);
    }

    @Override
    public void deleteById(String id) {
        if (id != null) {
            storage.remove(id);
        }
    }

    @Override
    public long count() {
        return storage.size();
    }
}
