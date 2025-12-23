package com.ledger.service;

import com.ledger.model.Account;
import com.ledger.repository.InMemoryAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    private AccountService accountService;
    private InMemoryAccountRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccountRepository();
        accountService = new AccountService(repository);
    }

    @Test
    void testCreateAccountWithInitialBalance() {
        Account account = accountService.createAccount("ACC001", new BigDecimal("1000.00"));

        assertNotNull(account);
        assertEquals("ACC001", account.getId());
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
    }

    @Test
    void testCreateAccountWithZeroBalance() {
        Account account = accountService.createAccount("ACC001", BigDecimal.ZERO);

        assertNotNull(account);
        assertEquals("ACC001", account.getId());
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    void testCreateAccountWithNegativeBalanceThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.createAccount("ACC001", new BigDecimal("-100"));
        });

        assertEquals("Balance cannot be negative", exception.getMessage());
    }

    @Test
    void testCreateDuplicateAccountThrowsException() {
        accountService.createAccount("ACC001", new BigDecimal("1000.00"));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            accountService.createAccount("ACC001", new BigDecimal("2000.00"));
        });

        assertEquals("Account with ID ACC001 already exists", exception.getMessage());
    }

    @Test
    void testGetExistingAccount() {
        accountService.createAccount("ACC001", new BigDecimal("1000.00"));

        Optional<Account> account = accountService.getAccount("ACC001");

        assertTrue(account.isPresent());
        assertEquals("ACC001", account.get().getId());
        assertEquals(new BigDecimal("1000.00"), account.get().getBalance());
    }

    @Test
    void testGetNonExistentAccount() {
        Optional<Account> account = accountService.getAccount("NONEXISTENT");
        assertFalse(account.isPresent());
    }

    @Test
    void testUpdateBalance() {
        accountService.createAccount("ACC001", new BigDecimal("1000.00"));

        Account updatedAccount = accountService.updateBalance("ACC001", new BigDecimal("1500.00"));

        assertEquals(new BigDecimal("1500.00"), updatedAccount.getBalance());

        Optional<Account> account = accountService.getAccount("ACC001");
        assertTrue(account.isPresent());
        assertEquals(new BigDecimal("1500.00"), account.get().getBalance());
    }

    @Test
    void testUpdateBalanceForNonExistentAccountThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.updateBalance("NONEXISTENT", new BigDecimal("1500.00"));
        });

        assertEquals("Account not found: NONEXISTENT", exception.getMessage());
    }
}
