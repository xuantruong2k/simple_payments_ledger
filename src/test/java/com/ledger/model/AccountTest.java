package com.ledger.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void testCreateAccountWithValidData() {
        Account account = new Account("ACC001", new BigDecimal("1000.00"));
        
        assertEquals("ACC001", account.getId());
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
    }

    @Test
    void testCreateAccountWithZeroBalance() {
        Account account = new Account("ACC001", BigDecimal.ZERO);
        
        assertEquals("ACC001", account.getId());
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    void testCreateAccountWithNullIdThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Account(null, BigDecimal.ZERO);
        });
        
        assertEquals("Account ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void testCreateAccountWithEmptyIdThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Account("", BigDecimal.ZERO);
        });
        
        assertEquals("Account ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void testCreateAccountWithNullBalanceThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Account("ACC001", null);
        });
        
        assertEquals("Balance cannot be null", exception.getMessage());
    }

    @Test
    void testCreateAccountWithNegativeBalanceThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new Account("ACC001", new BigDecimal("-100"));
        });
        
        assertEquals("Balance cannot be negative", exception.getMessage());
    }

    @Test
    void testSetValidBalance() {
        Account account = new Account("ACC001", new BigDecimal("1000.00"));
        account.setBalance(new BigDecimal("1500.00"));
        
        assertEquals(new BigDecimal("1500.00"), account.getBalance());
    }

    @Test
    void testSetNegativeBalanceThrowsException() {
        Account account = new Account("ACC001", new BigDecimal("1000.00"));
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            account.setBalance(new BigDecimal("-100"));
        });
        
        assertEquals("Balance cannot be negative", exception.getMessage());
    }

    @Test
    void testAccountsWithSameIdAreEqual() {
        Account account1 = new Account("ACC001", new BigDecimal("1000.00"));
        Account account2 = new Account("ACC001", new BigDecimal("2000.00"));
        
        assertEquals(account1, account2);
    }

    @Test
    void testAccountsWithDifferentIdAreNotEqual() {
        Account account1 = new Account("ACC001", new BigDecimal("1000.00"));
        Account account2 = new Account("ACC002", new BigDecimal("1000.00"));
        
        assertNotEquals(account1, account2);
    }
}
