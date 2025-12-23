package com.ledger.repository;

import com.ledger.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Account save(Account account);
    
    Optional<Account> findById(String id);
    
    List<Account> findAll();
    
    boolean existsById(String id);
    
    void deleteById(String id);
    
    long count();
}
