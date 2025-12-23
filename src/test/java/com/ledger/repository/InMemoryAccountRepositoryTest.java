package com.ledger.repository;

import com.ledger.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryAccountRepositoryTest {

    private InMemoryAccountRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccountRepository();
    }

    @Test
    void testSaveAndFindAccount() {
        Account account = new Account("ACC001", new BigDecimal("1000.00"));
        repository.save(account);

        Optional<Account> found = repository.findById("ACC001");
        assertTrue(found.isPresent());
        assertEquals("ACC001", found.get().getId());
        assertEquals(new BigDecimal("1000.00"), found.get().getBalance());
    }

    @Test
    void testUpdateExistingAccount() {
        Account account = new Account("ACC001", new BigDecimal("1000.00"));
        repository.save(account);

        account.setBalance(new BigDecimal("1500.00"));
        repository.save(account);

        Optional<Account> found = repository.findById("ACC001");
        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("1500.00"), found.get().getBalance());
    }

    @Test
    void testFindByIdReturnsEmptyForNonExistentAccount() {
        Optional<Account> found = repository.findById("NONEXISTENT");
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsById() {
        Account account = new Account("ACC001", new BigDecimal("1000.00"));
        repository.save(account);

        assertTrue(repository.existsById("ACC001"));
        assertFalse(repository.existsById("NONEXISTENT"));
    }

    @Test
    void testDeleteById() {
        Account account = new Account("ACC001", new BigDecimal("1000.00"));
        repository.save(account);

        assertTrue(repository.existsById("ACC001"));

        repository.deleteById("ACC001");

        assertFalse(repository.existsById("ACC001"));
    }

    @Test
    void testFindAll() {
        Account account1 = new Account("ACC001", new BigDecimal("1000.00"));
        Account account2 = new Account("ACC002", new BigDecimal("2000.00"));

        repository.save(account1);
        repository.save(account2);

        List<Account> accounts = repository.findAll();
        assertEquals(2, accounts.size());
    }

    @Test
    void testCount() {
        assertEquals(0, repository.count());

        repository.save(new Account("ACC001", new BigDecimal("1000.00")));
        assertEquals(1, repository.count());

        repository.save(new Account("ACC002", new BigDecimal("2000.00")));
        assertEquals(2, repository.count());
    }

    @Test
    void testSaveNullAccountThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            repository.save(null);
        });

        assertEquals("Account cannot be null", exception.getMessage());
    }
}
