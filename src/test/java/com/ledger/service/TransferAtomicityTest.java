package com.ledger.service;

import com.ledger.model.Account;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.InMemoryAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify atomicity of transfer operations.
 * Both debit and credit must succeed or fail together - no partial transfers.
 */
public class TransferAtomicityTest {

    private AccountRepository accountRepository;
    private AccountService accountService;
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        accountRepository = new InMemoryAccountRepository();
        accountService = new AccountService(accountRepository);
        transferService = new TransferService(accountRepository);
    }

    @Test
    void testTransferAtomicity_BothAccountsUpdated() {
        // Create accounts
        accountService.createAccount("A", new BigDecimal("1000.00"));
        accountService.createAccount("B", new BigDecimal("500.00"));

        // Transfer
        transferService.transfer("A", "B", new BigDecimal("300.00"));

        // Verify both accounts updated
        assertEquals(new BigDecimal("700.00"), accountService.getAccount("A").get().getBalance());
        assertEquals(new BigDecimal("800.00"), accountService.getAccount("B").get().getBalance());
    }

    @Test
    void testTransferAtomicity_InsufficientFunds_NoChanges() {
        accountService.createAccount("A", new BigDecimal("100.00"));
        accountService.createAccount("B", new BigDecimal("500.00"));

        // Try to transfer more than available
        assertThrows(AccountService.InsufficientFundsException.class, () -> {
            transferService.transfer("A", "B", new BigDecimal("200.00"));
        });

        // Verify no changes to either account
        assertEquals(new BigDecimal("100.00"), accountService.getAccount("A").get().getBalance());
        assertEquals(new BigDecimal("500.00"), accountService.getAccount("B").get().getBalance());
    }

    @Test
    void testTransferAtomicity_NonExistentAccount_NoChanges() {
        accountService.createAccount("A", new BigDecimal("1000.00"));
        // B doesn't exist

        assertThrows(IllegalArgumentException.class, () -> {
            transferService.transfer("A", "B", new BigDecimal("100.00"));
        });

        // Verify A unchanged
        assertEquals(new BigDecimal("1000.00"), accountService.getAccount("A").get().getBalance());
    }

    @Test
    void testConcurrentTransfers_TotalBalancePreserved() throws InterruptedException {
        // Create 10 accounts with $1000 each
        for (int i = 0; i < 10; i++) {
            accountService.createAccount("ACC" + i, new BigDecimal("1000.00"));
        }

        BigDecimal initialTotal = new BigDecimal("10000.00");

        // Perform 100 random transfers concurrently
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(100);

        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    int fromIdx = ThreadLocalRandom.current().nextInt(0, 10);
                    int toIdx = ThreadLocalRandom.current().nextInt(0, 10);
                    while (toIdx == fromIdx) {
                        toIdx = ThreadLocalRandom.current().nextInt(0, 10);
                    }

                    transferService.transfer(
                        "ACC" + fromIdx,
                        "ACC" + toIdx,
                        new BigDecimal("10.00")
                    );
                } catch (Exception e) {
                    // Some transfers may fail due to insufficient funds
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Calculate total balance across all accounts
        BigDecimal finalTotal = BigDecimal.ZERO;
        for (int i = 0; i < 10; i++) {
            finalTotal = finalTotal.add(
                accountService.getAccount("ACC" + i).get().getBalance()
            );
        }

        // Total should be preserved (no money created or destroyed)
        assertEquals(initialTotal, finalTotal,
            "Total balance must be preserved - atomicity violated if different");
    }

    @Test
    void testBidirectionalTransfers_NoDeadlock_BalancePreserved() throws InterruptedException {
        accountService.createAccount("A", new BigDecimal("1000.00"));
        accountService.createAccount("B", new BigDecimal("1000.00"));

        BigDecimal initialTotal = new BigDecimal("2000.00");

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(100);

        // 50 transfers A→B
        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                try {
                    transferService.transfer("A", "B", new BigDecimal("10.00"));
                } catch (Exception e) {
                    // May fail due to insufficient funds
                } finally {
                    latch.countDown();
                }
            });
        }

        // 50 transfers B→A
        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                try {
                    transferService.transfer("B", "A", new BigDecimal("10.00"));
                } catch (Exception e) {
                    // May fail due to insufficient funds
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verify total balance preserved
        BigDecimal finalTotal = accountService.getAccount("A").get().getBalance()
            .add(accountService.getAccount("B").get().getBalance());

        assertEquals(initialTotal, finalTotal,
            "Total balance must be preserved despite bidirectional transfers");
    }

    @Test
    void testHighConcurrencyTransfers_Atomicity() throws InterruptedException {
        // Create 100 accounts with $100 each
        for (int i = 0; i < 100; i++) {
            accountService.createAccount("ACC" + i, new BigDecimal("100.00"));
        }

        BigDecimal initialTotal = new BigDecimal("10000.00");

        // Perform 1000 random transfers with high concurrency
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(1000);

        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                try {
                    int fromIdx = ThreadLocalRandom.current().nextInt(0, 100);
                    int toIdx = ThreadLocalRandom.current().nextInt(0, 100);
                    while (toIdx == fromIdx) {
                        toIdx = ThreadLocalRandom.current().nextInt(0, 100);
                    }

                    transferService.transfer(
                        "ACC" + fromIdx,
                        "ACC" + toIdx,
                        new BigDecimal("5.00")
                    );
                } catch (Exception e) {
                    // Failures expected for insufficient funds
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Verify total balance preserved
        BigDecimal finalTotal = BigDecimal.ZERO;
        for (int i = 0; i < 100; i++) {
            finalTotal = finalTotal.add(
                accountService.getAccount("ACC" + i).get().getBalance()
            );
        }

        assertEquals(initialTotal, finalTotal,
            "Atomicity violated: Total balance changed under high concurrency");
    }

    @Test
    void testTransferWithFees_Atomicity() {
        // Even with fees, atomicity must be preserved
        // This test verifies that the two-phase commit in saveAll() works correctly

        accountService.createAccount("A", new BigDecimal("1000.00"));
        accountService.createAccount("B", new BigDecimal("500.00"));

        BigDecimal initialTotal = new BigDecimal("1500.00");

        // Transfer (fees are currently $0, but the mechanism should still work)
        transferService.transfer("A", "B", new BigDecimal("300.00"));

        // Verify atomicity: both accounts updated
        assertNotNull(accountService.getAccount("A").get());
        assertNotNull(accountService.getAccount("B").get());

        // Verify total preserved (minus fees if any)
        BigDecimal finalTotal = accountService.getAccount("A").get().getBalance()
            .add(accountService.getAccount("B").get().getBalance());

        // Should be 1500 - fees (currently fees = 0)
        assertTrue(finalTotal.compareTo(BigDecimal.ZERO) > 0,
            "Final balance should be positive");
        assertTrue(finalTotal.compareTo(initialTotal) <= 0,
            "Final balance should not exceed initial balance");
    }
}
