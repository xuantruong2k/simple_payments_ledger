package com.ledger.demo;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstration that proves we're using Virtual Threads (Java 21).
 *
 * Run this with: mvn test-compile exec:java -Dexec.mainClass="com.ledger.demo.VirtualThreadDemo"
 */
public class VirtualThreadDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Virtual Thread Proof Demo ===\n");
        System.out.println("Java Version: " + Runtime.version());
        System.out.println();

        // Demo 1: Check thread type
        demonstrateVirtualThreadType();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 2: Create many virtual threads (would exhaust platform threads)
        demonstrateMassiveScalability();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // Demo 3: Show thread names
        demonstrateThreadNames();
    }

    private static void demonstrateVirtualThreadType() {
        System.out.println("Demo 1: Checking Thread Type");
        System.out.println("-".repeat(50));

        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

        virtualExecutor.submit(() -> {
            Thread currentThread = Thread.currentThread();
            System.out.println("Thread class: " + currentThread.getClass().getName());
            System.out.println("Thread name: " + currentThread.getName());
            System.out.println("Is virtual: " + currentThread.isVirtual());
            System.out.println("Thread ID: " + currentThread.threadId());
        });

        virtualExecutor.shutdown();
        try {
            virtualExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void demonstrateMassiveScalability() {
        System.out.println("Demo 2: Creating 10,000 Concurrent Virtual Threads");
        System.out.println("-".repeat(50));
        System.out.println("(This would exhaust OS threads with platform threads!)");
        System.out.println();

        int threadCount = 10_000;
        AtomicInteger counter = new AtomicInteger(0);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                // Simulate some work
                try {
                    Thread.sleep(10);
                    counter.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long duration = System.currentTimeMillis() - startTime;

        System.out.println("Created: " + threadCount + " threads");
        System.out.println("Completed: " + counter.get() + " tasks");
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Average: " + (duration * 1.0 / threadCount) + "ms per thread");
        System.out.println("\n✅ Virtual threads enable massive concurrency!");
    }

    private static void demonstrateThreadNames() {
        System.out.println("Demo 3: Virtual Thread Names");
        System.out.println("-".repeat(50));

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < 5; i++) {
            final int taskNum = i;
            executor.submit(() -> {
                Thread t = Thread.currentThread();
                System.out.printf("Task %d - Thread: %s (Virtual: %s)%n",
                    taskNum, t.getName(), t.isVirtual());
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n✅ All threads are virtual!");
    }
}
