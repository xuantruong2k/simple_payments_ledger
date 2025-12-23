package com.ledger.repository;

import com.ledger.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);
    
    /**
     * Save multiple accounts atomically.
     * Either all accounts are saved or none are saved (all-or-nothing).
     * 
     * @param accounts Accounts to save
     * @throws IllegalArgumentException if any account is null or has null ID
     */
    void saveAll(Account... accounts);
    
    Optional<Account> findById(String id);
    
    List<Account> findAll();
    
    boolean existsById(String id);
    
    void deleteById(String id);
    
    long count();
}
