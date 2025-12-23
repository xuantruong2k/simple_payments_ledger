package com.ledger.locking;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manager for fine-grained account locks.
 * Provides one lock per account to avoid global synchronization bottleneck.
 * Thread-safe and scalable for high-concurrency scenarios (100K+ users).
 */
public class AccountLockManager {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Get or create a lock for the specified account ID.
     */
    public ReentrantLock getLock(String accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        return locks.computeIfAbsent(accountId, k -> new ReentrantLock());
    }

    /**
     * Acquire locks for two accounts in a consistent order to prevent deadlocks.
     * Lock Ordering Strategy: Always lock in alphabetical order of account IDs.
     */
    public LockPair acquireLocks(String accountId1, String accountId2) {
        if (accountId1 == null) {
            throw new IllegalArgumentException("From account ID is required");
        }
        if (accountId2 == null) {
            throw new IllegalArgumentException("To account ID is required");
        }
        
        int comparison = accountId1.compareTo(accountId2);
        
        if (comparison < 0) {
            ReentrantLock lock1 = getLock(accountId1);
            ReentrantLock lock2 = getLock(accountId2);
            lock1.lock();
            lock2.lock();
            return new LockPair(lock1, lock2);
        } else if (comparison > 0) {
            ReentrantLock lock2 = getLock(accountId2);
            ReentrantLock lock1 = getLock(accountId1);
            lock2.lock();
            lock1.lock();
            return new LockPair(lock1, lock2);
        } else {
            ReentrantLock lock = getLock(accountId1);
            lock.lock();
            return new LockPair(lock, null);
        }
    }

    /**
     * Release both locks in the pair.
     */
    public void releaseLocks(LockPair lockPair) {
        if (lockPair.lock1 != null && lockPair.lock1.isHeldByCurrentThread()) {
            lockPair.lock1.unlock();
        }
        if (lockPair.lock2 != null && lockPair.lock2.isHeldByCurrentThread()) {
            lockPair.lock2.unlock();
        }
    }

    /**
     * Container for a pair of locks.
     */
    public static class LockPair {
        private final ReentrantLock lock1;
        private final ReentrantLock lock2;

        public LockPair(ReentrantLock lock1, ReentrantLock lock2) {
            this.lock1 = lock1;
            this.lock2 = lock2;
        }
    }

    public int getLockCount() {
        return locks.size();
    }

    public void cleanupStaleLocks(java.util.Set<String> activeAccountIds) {
        locks.keySet().retainAll(activeAccountIds);
    }
}
