package com.ledger.service;

import com.ledger.model.Account;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.InMemoryAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for AccountService thread-safety.
 * Tests createAccount and updateBalance under concurrent access.
 */
public class AccountServiceConcurrencyTest {

    private AccountRepository accountRepository;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        accountService = new AccountService(accountRepository);
    }

    @Test
    void testConcurrentCreateAccountWithSameId() throws InterruptedException {
        int threadCount = 10;
        String accountId = "TEST_ACCOUNT";
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    accountService.createAccount(accountId, new BigDecimal("100.00"));
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    if (e.getMessage().contains("already exists")) {
                        failureCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        assertEquals(1, successCount.get());
        assertEquals(9, failureCount.get());
        assertTrue(accountRepository.existsById(accountId));
    }

    @Test
    void testConcurrentCreateDifferentAccounts() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int accountNum = i;
            executor.submit(() -> {
                try {
                    accountService.createAccount("ACC" + accountNum, new BigDecimal("1000.00"));
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        assertEquals(100, successCount.get());
        assertEquals(100, accountRepository.count());
    }

    @Test
    void testConcurrentUpdateBalance() throws InterruptedException {
        String accountId = "TEST_UPDATE";
        accountService.createAccount(accountId, BigDecimal.ZERO);
        
        int threadCount = 100;
        BigDecimal incrementAmount = new BigDecimal("10.00");
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // Use addToBalance for atomic read-modify-write
                    accountService.addToBalance(accountId, incrementAmount);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        Account finalAccount = accountService.getAccount(accountId).get();
        assertEquals(new BigDecimal("1000.00"), finalAccount.getBalance());
    }

    @Test
    void testNoLostUpdates() throws InterruptedException {
        String accountId = "STRESS_TEST";
        accountService.createAccount(accountId, BigDecimal.ZERO);
        
        int updateCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(updateCount);
        
        for (int i = 0; i < updateCount; i++) {
            executor.submit(() -> {
                try {
                    // Use addToBalance for atomic increment
                    accountService.addToBalance(accountId, new BigDecimal("1.00"));
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        Account finalAccount = accountService.getAccount(accountId).get();
        assertEquals(new BigDecimal("1000.00"), finalAccount.getBalance());
    }
}
