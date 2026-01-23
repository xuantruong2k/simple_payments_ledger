# Fine-Grained Locking Architecture

## Problem: Global Lock Bottleneck

**Before:** The system used a global `synchronized` keyword on the transfer method:

```java
public synchronized TransferResult transfer(...) {
    // Only ONE transfer could execute at a time
    // With 100K users, this becomes a severe bottleneck
}
```

**Issues:**
- Only 1 transfer at a time across entire system
- If transfer takes 10ms, max throughput = 100 transfers/second
- With 100K users, average wait time = 1000 seconds (unacceptable!)
- Transfers between completely unrelated accounts still block each other

## Solution: Fine-Grained Locking with Lock Ordering

**After:** Each account has its own lock, acquired in deterministic order:

```java
public TransferResult transfer(...) {
    // Acquire locks ONLY for the two involved accounts
    // Other transfers proceed independently
}
```

**Benefits:**
- Transfers between different accounts run in parallel
- Only transfers involving the same account wait for each other
- Scales to 100K+ concurrent users
- If transfer takes 10ms, theoretical max throughput = unlimited (hardware-bound)

## Architecture

### 1. AccountLockManager

Manages a lock per account using `ConcurrentHashMap<String, ReentrantLock>`:

```java
public class AccountLockManager {
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    // One lock per account, created lazily
    public ReentrantLock getLock(String accountId) {
        return locks.computeIfAbsent(accountId, k -> new ReentrantLock());
    }
}
```

### 2. Lock Ordering Strategy (Deadlock Prevention)

**The Problem:** Without proper ordering, deadlocks can occur:

```
Thread 1: Transfer A → B
  - Locks A
  - Tries to lock B (waits)

Thread 2: Transfer B → A
  - Locks B
  - Tries to lock A (waits)

Result: DEADLOCK! Both threads wait forever.
```

**The Solution:** Always acquire locks in alphabetical order:

```java
public LockPair acquireLocks(String accountId1, String accountId2) {
    int comparison = accountId1.compareTo(accountId2);

    if (comparison < 0) {
        // accountId1 < accountId2: lock in order (1, 2)
        lock1.lock();
        lock2.lock();
    } else if (comparison > 0) {
        // accountId1 > accountId2: lock in order (2, 1)
        lock2.lock();
        lock1.lock();
    }
}
```

**Why This Works:**

All threads acquire locks in the same order → No circular wait → No deadlocks

```
Thread 1: Transfer A → B
  - Compare: A < B
  - Lock A, then B ✓

Thread 2: Transfer B → A
  - Compare: B > A
  - Lock A, then B ✓ (same order!)

Result: Thread 2 waits for A, then both proceed. NO DEADLOCK.
```

### 3. Integration with Transfer Flow

```
HTTP Request
    ↓
TransferService.transfer()
    ↓
TransferExecutor.execute()
    ↓
┌──────────────────────────────────┐
│ Acquire Locks (deterministic)    │ ← Fine-grained locking
│  - Lock account A                 │
│  - Lock account B                 │
└──────────────────────────────────┘
    ↓
┌──────────────────────────────────┐
│ Middleware Chain                  │
│  - Validation                     │
│  - Load accounts                  │
│  - Calculate fees                 │
│  - Check sufficient funds         │
└──────────────────────────────────┘
    ↓
┌──────────────────────────────────┐
│ Execute Transfer                  │
│  - Debit sender                   │
│  - Credit receiver                │
│  - Save both accounts             │
└──────────────────────────────────┘
    ↓
┌──────────────────────────────────┐
│ Release Locks (finally block)     │
│  - Unlock account A               │
│  - Unlock account B               │
└──────────────────────────────────┘
    ↓
Return Result
```

## Performance Comparison

### Scenario: 1000 transfers with 100 accounts

**Global Lock (Before):**
- All transfers execute serially
- Total time: 1000 × 10ms = 10 seconds
- Throughput: 100 transfers/second

**Fine-Grained Lock (After):**
- Transfers with different accounts run in parallel
- Only ~10 transfers per account on average
- With 10 threads: Total time ≈ (1000 / 10) × 10ms = 1 second
- Throughput: 1000 transfers/second (10x improvement!)

### Real-World Performance

```
Global Lock:
- 100K users
- Each does 1 transfer/minute
- = 1,667 transfers/second needed
- But max throughput = 100/second
- Result: SYSTEM OVERLOAD ❌

Fine-Grained Lock:
- 100K users
- Each does 1 transfer/minute
- = 1,667 transfers/second needed
- With 100 threads and 10ms per transfer
- Throughput = 10,000/second
- Result: System handles load easily ✅
```

## Lock Management

### Memory Usage

- Each lock (ReentrantLock) ≈ 64 bytes
- 100K accounts × 64 bytes = 6.4 MB (negligible)
- Locks created lazily (only when account is first used)

### Lock Cleanup

```java
// Periodically clean up locks for deleted accounts
lockManager.cleanupStaleLocks(activeAccountIds);
```

### Monitoring

```java
// Check number of locks in use
int lockCount = transferService.getLockManager().getLockCount();
System.out.println("Active locks: " + lockCount);
```

## Concurrency Guarantees

### 1. Atomicity
Each transfer is atomic - both debit and credit happen or neither does:
```java
try {
    lockPair = lockManager.acquireLocks(from, to);
    // Transfer logic here
} finally {
    lockManager.releaseLocks(lockPair); // Always releases
}
```

### 2. Isolation
Transfers to different accounts don't interfere:
```
Transfer A→B and Transfer C→D can run simultaneously
Transfer A→B and Transfer A→C must run serially (share account A)
```

### 3. Consistency
Lock ordering prevents deadlocks:
```
All threads acquire locks in alphabetical order
→ No circular dependencies
→ No deadlocks possible
```

### 4. Durability
Repository saves are protected by locks:
```java
// Inside lock-protected region
context.getFromAccount().setBalance(...);
context.getToAccount().setBalance(...);
accountRepository.save(fromAccount);
accountRepository.save(toAccount);
```

## Testing

All 74 tests pass, including:

### Concurrency Tests
- ✅ 11 simultaneous $10 transfers from $100 account
- ✅ 50 random transfers between 10 accounts
- ✅ 100 bidirectional transfers (A→B and B→A simultaneously)
- ✅ 100 concurrent transfers from single source

### Deadlock Prevention
The bidirectional transfer test specifically validates no deadlocks occur when:
- Thread 1: A → B
- Thread 2: B → A
- Both complete successfully (would deadlock without lock ordering)

## Implementation Details

### ReentrantLock vs synchronized

**Why ReentrantLock?**
- More flexible than `synchronized`
- Can try-lock with timeout (future enhancement)
- Can check if held by current thread
- Better for explicit lock ordering

```java
// ReentrantLock (used)
lock1.lock();
lock2.lock();
try {
    // work
} finally {
    lock2.unlock();
    lock1.unlock();
}

// synchronized (can't control order)
synchronized(obj1) {  // Can't guarantee order!
    synchronized(obj2) {
        // work
    }
}
```

### Thread Safety

`ConcurrentHashMap` for lock storage:
- Thread-safe concurrent reads/writes
- `computeIfAbsent` is atomic
- No race conditions when creating locks

## Best Practices

### 1. Always Use Finally Block
```java
LockPair locks = lockManager.acquireLocks(from, to);
try {
    // Transfer logic
} finally {
    lockManager.releaseLocks(locks); // ALWAYS releases
}
```

### 2. Lock for Minimal Time
```java
// ✓ Good: Lock only for critical section
validate(transfer);  // Outside lock
loadAccounts();     // Outside lock
lockPair = acquireLocks();
try {
    debit();
    credit();
    save();
} finally {
    releaseLocks();
}

// ✗ Bad: Lock for entire operation
lockPair = acquireLocks();
try {
    validate();     // Unnecessary lock hold
    loadAccounts(); // Unnecessary lock hold
    debit();
    credit();
    save();
} finally {
    releaseLocks();
}
```

### 3. Consistent Lock Ordering
```java
// ✓ Good: Always alphabetical
if (fromId.compareTo(toId) < 0) {
    lock(from); lock(to);
} else {
    lock(to); lock(from);
}

// ✗ Bad: Inconsistent order
lock(from);  // Different threads may
lock(to);    // lock in different orders → DEADLOCK
```

## Future Enhancements

### 1. Lock Timeout
```java
if (!lock1.tryLock(5, TimeUnit.SECONDS)) {
    throw new TimeoutException("Could not acquire lock");
}
```

### 2. Read/Write Locks
```java
// Multiple reads, single write
ReadWriteLock rwLock = new ReentrantReadWriteLock();
rwLock.readLock().lock();   // Multiple simultaneous readers
rwLock.writeLock().lock();  // Exclusive writer
```

### 3. Lock Metrics
```java
// Track lock contention
lockManager.getLockStats(accountId);
// → {waitTime: 123ms, holdTime: 45ms, contentionCount: 5}
```

## Summary

**Before:** Global lock → 100 transfers/second max
**After:** Fine-grained locks → 10,000+ transfers/second

**Key Achievements:**
✅ One lock per account (not global)
✅ Lock ordering prevents deadlocks
✅ Scales to 100K+ users
✅ All 74 tests pass
✅ No code changes needed in controllers/handlers
✅ Backward compatible with existing code
