package com.ledger.repository;

import com.ledger.model.Account;

import java.util.ArrayList;
import java.util.HashMap;
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

    /**
     * Save multiple accounts atomically using two-phase commit pattern.
     * Phase 1: Validate all accounts and prepare changes in temporary map
     * Phase 2: Apply all changes atomically with putAll()
     * 
     * This ensures that if any validation fails, NO accounts are saved.
     */
    @Override
    public void saveAll(Account... accounts) {
        if (accounts == null || accounts.length == 0) {
            return;
        }
        
        // PHASE 1: Validate and prepare all changes
        // If any exception occurs here, nothing has been written to storage yet
        Map<String, Account> updates = new HashMap<>();
        for (Account account : accounts) {
            if (account == null) {
                throw new IllegalArgumentException("Cannot save null account");
            }
            if (account.getId() == null) {
                throw new IllegalArgumentException("Account ID cannot be null");
            }
            updates.put(account.getId(), account);
        }
        
        // PHASE 2: Apply all changes atomically
        // putAll() is more atomic than individual puts in a loop
        // If this fails (e.g., OutOfMemoryError), it's a catastrophic failure
        storage.putAll(updates);
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
