package com.ledger.service;

import com.ledger.model.Account;
import com.ledger.repository.InMemoryAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {

    private AccountService accountService;
    private InMemoryAccountRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccountRepository();
        accountService = new AccountService(repository);
    }

    // ==================== Functional Tests ====================

    @Test
    void testSuccessfulTransfer() {
        accountService.createAccount("SENDER", new BigDecimal("1000.00"));
        accountService.createAccount("RECEIVER", new BigDecimal("500.00"));

        AccountService.TransferResult result = accountService.transfer(
            "SENDER", "RECEIVER", new BigDecimal("300.00"));

        assertNotNull(result);
        assertEquals(new BigDecimal("700.00"), result.getFromAccount().getBalance());
        assertEquals(new BigDecimal("800.00"), result.getToAccount().getBalance());
        assertEquals(new BigDecimal("300.00"), result.getAmount());
    }

    @Test
    void testTransferDebitsAndCreditsCorrectly() {
        accountService.createAccount("ACC1", new BigDecimal("1000.00"));
        accountService.createAccount("ACC2", new BigDecimal("500.00"));

        accountService.transfer("ACC1", "ACC2", new BigDecimal("250.00"));

        Account sender = accountService.getAccount("ACC1").get();
        Account receiver = accountService.getAccount("ACC2").get();

        assertEquals(new BigDecimal("750.00"), sender.getBalance());
        assertEquals(new BigDecimal("750.00"), receiver.getBalance());
    }

    @Test
    void testTransferAllFunds() {
        accountService.createAccount("SENDER", new BigDecimal("1000.00"));
        accountService.createAccount("RECEIVER", new BigDecimal("0"));

        accountService.transfer("SENDER", "RECEIVER", new BigDecimal("1000.00"));

        Account sender = accountService.getAccount("SENDER").get();
        Account receiver = accountService.getAccount("RECEIVER").get();

        assertEquals(new BigDecimal("0.00"), sender.getBalance());
        assertEquals(new BigDecimal("1000.00"), receiver.getBalance());
    }

    // ==================== Edge Cases and Business Logic ====================

    @Test
    void testInsufficientFunds() {
        accountService.createAccount("SENDER", new BigDecimal("100.00"));
        accountService.createAccount("RECEIVER", new BigDecimal("500.00"));

        Exception exception = assertThrows(AccountService.InsufficientFundsException.class, () -> {
            accountService.transfer("SENDER", "RECEIVER", new BigDecimal("200.00"));
        });

        assertTrue(exception.getMessage().contains("Insufficient funds"));

        // Verify balances unchanged
        Account sender = accountService.getAccount("SENDER").get();
        Account receiver = accountService.getAccount("RECEIVER").get();
        assertEquals(new BigDecimal("100.00"), sender.getBalance());
        assertEquals(new BigDecimal("500.00"), receiver.getBalance());
    }

    @Test
    void testTransferExactlyAvailableFunds() {
        accountService.createAccount("SENDER", new BigDecimal("100.00"));
        accountService.createAccount("RECEIVER", new BigDecimal("0"));

        accountService.transfer("SENDER", "RECEIVER", new BigDecimal("100.00"));

        Account sender = accountService.getAccount("SENDER").get();
        assertEquals(new BigDecimal("0.00"), sender.getBalance());
    }

    @Test
    void testTransferMoreThanAvailableFunds() {
        accountService.createAccount("SENDER", new BigDecimal("100.00"));
        accountService.createAccount("RECEIVER", new BigDecimal("500.00"));

        Exception exception = assertThrows(AccountService.InsufficientFundsException.class, () -> {
            accountService.transfer("SENDER", "RECEIVER", new BigDecimal("100.01"));
        });

        assertTrue(exception.getMessage().contains("Insufficient funds"));
    }

    @Test
    void testSelfTransferThrowsException() {
        accountService.createAccount("ACC001", new BigDecimal("1000.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("ACC001", "ACC001", new BigDecimal("100.00"));
        });

        assertEquals("Cannot transfer to the same account", exception.getMessage());

        // Verify balance unchanged
        Account account = accountService.getAccount("ACC001").get();
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
    }

    @Test
    void testTransferWithNegativeAmount() {
        accountService.createAccount("SENDER", new BigDecimal("1000.00"));
        accountService.createAccount("RECEIVER", new BigDecimal("500.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("SENDER", "RECEIVER", new BigDecimal("-100.00"));
        });

        assertEquals("Amount must be greater than zero", exception.getMessage());

        // Verify balances unchanged
        Account sender = accountService.getAccount("SENDER").get();
        Account receiver = accountService.getAccount("RECEIVER").get();
        assertEquals(new BigDecimal("1000.00"), sender.getBalance());
        assertEquals(new BigDecimal("500.00"), receiver.getBalance());
    }

    @Test
    void testTransferWithZeroAmount() {
        accountService.createAccount("SENDER", new BigDecimal("1000.00"));
        accountService.createAccount("RECEIVER", new BigDecimal("500.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("SENDER", "RECEIVER", BigDecimal.ZERO);
        });

        assertEquals("Amount must be greater than zero", exception.getMessage());
    }

    @Test
    void testTransferFromNonExistentAccount() {
        accountService.createAccount("RECEIVER", new BigDecimal("500.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("NONEXISTENT", "RECEIVER", new BigDecimal("100.00"));
        });

        assertTrue(exception.getMessage().contains("From account not found"));
    }

    @Test
    void testTransferToNonExistentAccount() {
        accountService.createAccount("SENDER", new BigDecimal("1000.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("SENDER", "NONEXISTENT", new BigDecimal("100.00"));
        });

        assertTrue(exception.getMessage().contains("To account not found"));
    }

    @Test
    void testTransferWithNullFromAccountId() {
        accountService.createAccount("RECEIVER", new BigDecimal("500.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer(null, "RECEIVER", new BigDecimal("100.00"));
        });

        assertEquals("From account ID is required", exception.getMessage());
    }

    @Test
    void testTransferWithNullToAccountId() {
        accountService.createAccount("SENDER", new BigDecimal("1000.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("SENDER", null, new BigDecimal("100.00"));
        });

        assertEquals("To account ID is required", exception.getMessage());
    }

    @Test
    void testTransferWithNullAmount() {
        accountService.createAccount("SENDER", new BigDecimal("1000.00"));
        accountService.createAccount("RECEIVER", new BigDecimal("500.00"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            accountService.transfer("SENDER", "RECEIVER", null);
        });

        assertEquals("Amount is required", exception.getMessage());
    }

    // ==================== Concurrency Tests ====================

    @Test
    void testConcurrentTransfersFromSameAccount() throws InterruptedException {
        // Create account with $100
        accountService.createAccount("SENDER", new BigDecimal("100.00"));

        // Create 11 receiver accounts
        for (int i = 1; i <= 11; i++) {
            accountService.createAccount("RECEIVER" + i, BigDecimal.ZERO);
        }

        int numberOfTransfers = 11;
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(numberOfTransfers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Attempt 11 simultaneous transfers of $10 from $100 account
        for (int i = 1; i <= numberOfTransfers; i++) {
            final String receiverId = "RECEIVER" + i;
            executorService.submit(() -> {
                try {
                    accountService.transfer("SENDER", receiverId, new BigDecimal("10.00"));
                    successCount.incrementAndGet();
                } catch (AccountService.InsufficientFundsException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // Only 10 transfers should succeed
        assertEquals(10, successCount.get(), "Expected exactly 10 successful transfers");
        assertEquals(1, failureCount.get(), "Expected exactly 1 failed transfer");

        // Verify sender balance is 0
        Account sender = accountService.getAccount("SENDER").get();
        assertEquals(0, sender.getBalance().compareTo(BigDecimal.ZERO), "Sender should have 0 balance");

        // Calculate total amount in receiver accounts
        BigDecimal totalReceived = BigDecimal.ZERO;
        for (int i = 1; i <= numberOfTransfers; i++) {
            Account receiver = accountService.getAccount("RECEIVER" + i).get();
            totalReceived = totalReceived.add(receiver.getBalance());
        }
        assertEquals(new BigDecimal("100.00"), totalReceived, "Total received should equal original sender balance");
    }

    @Test
    void testConcurrentTransfersBetweenMultipleAccounts() throws InterruptedException {
        // Create 10 accounts with $100 each
        int numberOfAccounts = 10;
        for (int i = 1; i <= numberOfAccounts; i++) {
            accountService.createAccount("ACC" + i, new BigDecimal("100.00"));
        }

        int numberOfTransfers = 50;
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(numberOfTransfers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Perform random transfers between accounts
        for (int i = 0; i < numberOfTransfers; i++) {
            final int fromIdx = (i % numberOfAccounts) + 1;
            final int toIdx = ((i + 1) % numberOfAccounts) + 1;

            executorService.submit(() -> {
                try {
                    accountService.transfer("ACC" + fromIdx, "ACC" + toIdx, new BigDecimal("10.00"));
                    successCount.incrementAndGet();
                } catch (AccountService.InsufficientFundsException e) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // Verify total balance is preserved
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (int i = 1; i <= numberOfAccounts; i++) {
            Account account = accountService.getAccount("ACC" + i).get();
            totalBalance = totalBalance.add(account.getBalance());
        }
        assertEquals(new BigDecimal("1000.00"), totalBalance, "Total system balance should be preserved");
    }

    @Test
    void testDeadlockPrevention() throws InterruptedException {
        accountService.createAccount("ACC1", new BigDecimal("1000.00"));
        accountService.createAccount("ACC2", new BigDecimal("1000.00"));

        int numberOfIterations = 100;
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(numberOfIterations * 2);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // ACC1 -> ACC2 transfers
        for (int i = 0; i < numberOfIterations; i++) {
            executorService.submit(() -> {
                try {
                    accountService.transfer("ACC1", "ACC2", new BigDecimal("1.00"));
                } catch (AccountService.InsufficientFundsException e) {
                    // Expected when funds run out
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // ACC2 -> ACC1 transfers (reverse direction)
        for (int i = 0; i < numberOfIterations; i++) {
            executorService.submit(() -> {
                try {
                    accountService.transfer("ACC2", "ACC1", new BigDecimal("1.00"));
                } catch (AccountService.InsufficientFundsException e) {
                    // Expected when funds run out
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all transfers to complete (should not deadlock)
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(completed, "All transfers should complete without deadlock");
        assertTrue(exceptions.isEmpty(), "No unexpected exceptions should occur");

        // Verify total balance is preserved
        Account acc1 = accountService.getAccount("ACC1").get();
        Account acc2 = accountService.getAccount("ACC2").get();
        BigDecimal totalBalance = acc1.getBalance().add(acc2.getBalance());
        assertEquals(new BigDecimal("2000.00"), totalBalance, "Total balance should be preserved");
    }

    @Test
    void testRaceConditionOnSingleAccount() throws InterruptedException {
        accountService.createAccount("SOURCE", new BigDecimal("1000.00"));

        // Create 100 target accounts
        for (int i = 1; i <= 100; i++) {
            accountService.createAccount("TARGET" + i, BigDecimal.ZERO);
        }

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // 100 threads trying to transfer $10 from SOURCE (which has $1000)
        for (int i = 1; i <= numberOfThreads; i++) {
            final String targetId = "TARGET" + i;
            executorService.submit(() -> {
                try {
                    accountService.transfer("SOURCE", targetId, new BigDecimal("10.00"));
                    successCount.incrementAndGet();
                } catch (AccountService.InsufficientFundsException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(100, successCount.get(), "All 100 transfers should succeed");
        assertEquals(0, failureCount.get(), "No transfers should fail");

        // Verify source account has 0 balance
        Account source = accountService.getAccount("SOURCE").get();
        assertEquals(0, source.getBalance().compareTo(BigDecimal.ZERO));

        // Verify total distributed amount
        BigDecimal totalDistributed = BigDecimal.ZERO;
        for (int i = 1; i <= numberOfThreads; i++) {
            Account target = accountService.getAccount("TARGET" + i).get();
            totalDistributed = totalDistributed.add(target.getBalance());
        }
        assertEquals(new BigDecimal("1000.00"), totalDistributed);
    }
}
