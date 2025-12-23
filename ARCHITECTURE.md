# Architecture & Extensibility Guide

## Overview

The Simple Payment Ledger uses a **middleware-based architecture** that decouples business logic from API routing and makes the system easy to extend without breaking core functionality.

## Architecture Layers

```
┌─────────────────────────────────────────────────────┐
│              HTTP Layer (Javalin)                   │
│  Controllers → Handlers → DTOs                      │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│           Business Logic Layer                      │
│  AccountService, TransferService                    │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│          Middleware Chain (Transfer)                │
│  Validation → Loading → Fee → Funds Check           │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│          Transfer Executor                          │
│  Executes actual debit/credit operations            │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│         Repository Layer                            │
│  InMemoryAccountRepository (swappable)              │
└─────────────────────────────────────────────────────┘
```

## Middleware Pattern

### Core Components

1. **TransferContext** - Immutable context object containing:
   - Original transfer parameters
   - Loaded account references
   - Calculated fees
   - Effective transfer amount

2. **TransferMiddleware** - Functional interface for processing steps:
   ```java
   @FunctionalInterface
   public interface TransferMiddleware {
       void process(TransferContext context, Runnable next) throws Exception;
   }
   ```

3. **TransferExecutor** - Orchestrates middleware chain and executes transfer

4. **TransferService** - Configures middleware chain and provides public API

### Middleware Chain

The transfer process flows through these middleware in order:

```
1. TransferValidationMiddleware
   ↓ Validates IDs, amount, prevents self-transfer
   
2. AccountLoadingMiddleware
   ↓ Loads sender and receiver accounts from repository
   
3. TransactionFeeMiddleware
   ↓ Calculates fees (currently 0%, easily configurable)
   
4. SufficientFundsMiddleware
   ↓ Checks if sender has enough funds (amount + fees)
   
5. TransferExecutor
   ↓ Executes debit from sender, credit to receiver
```

## Adding New Features

### Example 1: Adding Transaction Fees

To add a 1% transaction fee:

1. **Update `TransactionFeeMiddleware.java`:**

```java
public class TransactionFeeMiddleware implements TransferMiddleware {
    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.01"); // 1% fee
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.50"); // $0.50 fixed fee

    @Override
    public void process(TransferContext context, Runnable next) throws Exception {
        // Calculate percentage fee
        BigDecimal percentageFee = context.getAmount()
            .multiply(FEE_PERCENTAGE)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Total fee = percentage + fixed
        BigDecimal totalFee = percentageFee.add(FIXED_FEE);
        
        // Set fee in context
        context.setFee(totalFee);
        
        // Option A: Deduct fee from transfer amount
        // context.setEffectiveAmount(context.getAmount().subtract(totalFee));
        
        // Option B: Add fee on top of transfer (implemented)
        // Sender pays: amount + fee
        // Receiver gets: amount only
        
        next.run();
    }
}
```

2. **Update `TransferResponse.java` to include fee:**

```java
public class TransferResponse {
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private BigDecimal fee; // Add this field
    private BigDecimal fromAccountBalance;
    private BigDecimal toAccountBalance;
    
    // Add getter/setter for fee
}
```

3. **Update `TransactionHandler.java` to return fee:**

```java
TransferResponse response = new TransferResponse(
    result.getFromAccount().getId(),
    result.getToAccount().getId(),
    result.getAmount(),
    result.getFee(), // Pass fee from result
    result.getFromAccount().getBalance(),
    result.getToAccount().getBalance()
);
```

**That's it!** No changes needed to:
- Controller routing logic
- Core transfer execution
- Account management
- Repository layer
- Existing tests (they'll still pass)

### Example 2: Adding Audit Logging

Create a new middleware:

```java
public class AuditLoggingMiddleware implements TransferMiddleware {
    private final AuditLogger auditLogger;
    
    public AuditLoggingMiddleware(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }
    
    @Override
    public void process(TransferContext context, Runnable next) throws Exception {
        // Log before transfer
        auditLogger.log("TRANSFER_INITIATED", context);
        
        try {
            next.run();
            // Log successful transfer
            auditLogger.log("TRANSFER_SUCCESS", context);
        } catch (Exception e) {
            // Log failed transfer
            auditLogger.log("TRANSFER_FAILED", context, e);
            throw e;
        }
    }
}
```

Add to middleware chain in `TransferService`:

```java
List<TransferMiddleware> middlewares = Arrays.asList(
    new TransferValidationMiddleware(),
    new AccountLoadingMiddleware(accountRepository),
    new AuditLoggingMiddleware(auditLogger), // New middleware
    new TransactionFeeMiddleware(),
    new SufficientFundsMiddleware()
);
```

### Example 3: Adding Fraud Detection

```java
public class FraudDetectionMiddleware implements TransferMiddleware {
    private final FraudDetectionService fraudService;
    
    @Override
    public void process(TransferContext context, Runnable next) throws Exception {
        // Check for fraud
        FraudCheckResult result = fraudService.checkTransfer(
            context.getFromAccountId(),
            context.getToAccountId(),
            context.getAmount()
        );
        
        if (result.isSuspicious()) {
            throw new FraudDetectedException("Transfer blocked: " + result.getReason());
        }
        
        next.run();
    }
}
```

### Example 4: Adding Daily Limits

```java
public class DailyLimitMiddleware implements TransferMiddleware {
    private final LimitRepository limitRepository;
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("10000.00");
    
    @Override
    public void process(TransferContext context, Runnable next) throws Exception {
        BigDecimal todayTotal = limitRepository.getTodayTotal(context.getFromAccountId());
        BigDecimal newTotal = todayTotal.add(context.getAmount());
        
        if (newTotal.compareTo(DAILY_LIMIT) > 0) {
            throw new DailyLimitExceededException(
                "Daily limit of " + DAILY_LIMIT + " exceeded"
            );
        }
        
        next.run();
        
        // Update daily total after successful transfer
        limitRepository.updateTodayTotal(context.getFromAccountId(), newTotal);
    }
}
```

## Key Benefits

### 1. **Separation of Concerns**
- Routing logic in Controllers
- Request/response handling in Handlers
- Business logic in Services
- Processing steps in Middleware
- Data access in Repositories

### 2. **Easy Extensibility**
- Add new middleware without changing existing code
- Configure different middleware chains for different use cases
- Enable/disable features by adding/removing middleware

### 3. **Testability**
- Test each middleware independently
- Test service with custom middleware combinations
- Mock middleware for integration tests

### 4. **Backward Compatibility**
- Old code still works through `AccountService.transfer()`
- Internal refactoring doesn't affect API consumers
- All 58 existing tests pass without modification

### 5. **Maintainability**
- Each middleware has single responsibility
- Easy to locate and modify specific functionality
- Clear execution flow through middleware chain

## Configuration Examples

### Development Environment (No Fees)
```java
List<TransferMiddleware> devMiddlewares = Arrays.asList(
    new TransferValidationMiddleware(),
    new AccountLoadingMiddleware(accountRepository),
    // No fee middleware
    new SufficientFundsMiddleware()
);
```

### Production Environment (Full Features)
```java
List<TransferMiddleware> prodMiddlewares = Arrays.asList(
    new TransferValidationMiddleware(),
    new AccountLoadingMiddleware(accountRepository),
    new FraudDetectionMiddleware(fraudService),
    new DailyLimitMiddleware(limitRepository),
    new TransactionFeeMiddleware(), // Fees enabled
    new SufficientFundsMiddleware(),
    new AuditLoggingMiddleware(auditLogger)
);
```

### Testing Environment (Minimal)
```java
List<TransferMiddleware> testMiddlewares = Arrays.asList(
    new TransferValidationMiddleware(),
    new AccountLoadingMiddleware(accountRepository),
    new SufficientFundsMiddleware()
);
```

## Migration Path

### From Old Code to New
```java
// Old way (still works)
AccountService accountService = new AccountService(repository);
accountService.transfer(fromId, toId, amount);

// New way (more flexible)
TransferService transferService = new TransferService(repository);
transferService.transfer(fromId, toId, amount);

// Custom middleware chain
TransferService customService = new TransferService(repository, customMiddlewares);
```

## Lock Management

### Lock Manager Lifecycle

**Option 1: Per-Service Lock Manager**
```java
TransferService transferService = new TransferService(repository);
// Creates its own AccountLockManager instance
```

**Option 2: Shared Lock Manager (Recommended)**
```java
AccountService accountService = new AccountService(repository);
TransferService transferService = new TransferService(
    repository,
    customMiddlewares,
    accountService.getLockManager()  // Share lock manager
);
```

**When to share:**
- ✅ When `AccountService` and `TransferService` operate on same accounts
- ✅ When you need centralized lock monitoring
- ✅ In production environments

**When separate is okay:**
- ✅ In isolated test scenarios
- ✅ When services operate on completely different account sets
- ✅ For specific testing requirements

### Memory Usage

- Each lock (ReentrantLock) ≈ 64 bytes
- 100K accounts × 64 bytes = 6.4 MB (negligible)
- Locks created lazily (only when account is first used)
- **Shared lock manager reduces memory overhead**

## Integration Examples

### Example 1: Application Setup (from Application.java)

```java
public class Application {
    public static void main(String[] args) {
        // Initialize repository
        AccountRepository accountRepository = new InMemoryAccountRepository();

        // Initialize services with independent lock managers
        AccountService accountService = new AccountService(accountRepository);
        TransferService transferService = new TransferService(accountRepository);
        
        // Note: In production, consider sharing lock managers:
        // TransferService transferService = new TransferService(
        //     accountRepository,
        //     defaultMiddlewares,
        //     accountService.getLockManager()
        // );

        // ...rest of setup
    }
}
```

### Example 2: AccountService Internal Usage

```java
public class AccountService {
    private final TransferService transferService;
    private final AccountLockManager lockManager;

    public AccountService(AccountRepository accountRepository) {
        this.lockManager = new AccountLockManager();

        // Share the same lock manager with TransferService
        this.transferService = new TransferService(
            accountRepository,
            Arrays.asList(
                new TransferValidationMiddleware(),
                new AccountLoadingMiddleware(accountRepository),
                new TransactionFeeMiddleware(),
                new SufficientFundsMiddleware()
            ),
            this.lockManager  // Pass the shared lock manager
        );
    }
}
```
