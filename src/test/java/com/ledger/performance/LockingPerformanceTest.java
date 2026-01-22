package com.ledger.performance;

import com.ledger.repository.AccountRepository;
import com.ledger.repository.InMemoryAccountRepository;
import com.ledger.service.AccountService;
import com.ledger.service.TransferService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance tests demonstrating fine-grained locking benefits.
 * Run with: mvn test -Dtest=LockingPerformanceTest
 */
public class LockingPerformanceTest {

    @Test
    void demonstrateConcurrentTransferPerformance() throws InterruptedException {
        System.out.println("\n=== Virtual Threads Performance Demo (Java 21) ===\n");

        // Setup: 100 accounts with $10,000 each
        AccountRepository repository = new InMemoryAccountRepository();
        AccountService accountService = new AccountService(repository);
        TransferService transferService = new TransferService(repository);

        int accountCount = 100;
        for (int i = 1; i <= accountCount; i++) {
            accountService.createAccount("ACC" + i, new BigDecimal("10000"));
        }

        // Test: 1000 random transfers using virtual threads
        int transferCount = 1000;

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(transferCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < transferCount; i++) {
            final int fromIdx = ThreadLocalRandom.current().nextInt(1, accountCount + 1);
            int toIdx = ThreadLocalRandom.current().nextInt(1, accountCount + 1);
            while (toIdx == fromIdx) {
                toIdx = ThreadLocalRandom.current().nextInt(1, accountCount + 1);
            }
            final int finalToIdx = toIdx;

            executor.submit(() -> {
                try {
                    transferService.transfer(
                        "ACC" + fromIdx,
                        "ACC" + finalToIdx,
                        new BigDecimal("10.00")
                    );
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        long duration = endTime - startTime;
        double throughput = (transferCount * 1000.0) / duration;

        System.out.println("Results:");
        System.out.println("  Transfers: " + transferCount);
        System.out.println("  Execution: Virtual Threads (Java 21)");
        System.out.println("  Accounts: " + accountCount);
        System.out.println("  Duration: " + duration + "ms");
        System.out.println("  Throughput: " + String.format("%.0f", throughput) + " transfers/second");
        System.out.println("  Successful: " + successCount.get());
        System.out.println("  Failed: " + failureCount.get());
        System.out.println("  Locks created: " + transferService.getLockManager().getLockCount());

        // Verify total balance preserved
        BigDecimal totalBalance = BigDecimal.ZERO;
        for (int i = 1; i <= accountCount; i++) {
            totalBalance = totalBalance.add(
                accountService.getAccount("ACC" + i).get().getBalance()
            );
        }

        System.out.println("\nBalance Verification:");
        System.out.println("  Initial total: $" + (accountCount * 10000));
        System.out.println("  Final total: $" + totalBalance);
        System.out.println("  Preserved: " +
            totalBalance.equals(new BigDecimal(accountCount * 10000)));
    }
}
