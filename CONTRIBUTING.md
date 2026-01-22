# Contributing Guidelines

Thank you for your interest in contributing to Simple Payment Ledger! This document provides guidelines for maintaining code quality and consistency.

## Getting Started

### Prerequisites
- Java 21 or higher (for Virtual Threads support)
- Maven 3.6+
- Git

### Setup
```bash
git clone <repository-url>
cd simple_payment_ledger
mvn clean install
mvn test  # Verify all tests pass
```

## Development Workflow

1. **Fork & Branch** - Create a feature branch from `main`
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Code** - Implement your changes following guidelines below

3. **Test** - Add tests and ensure all tests pass
   ```bash
   mvn test
   ```

4. **Commit** - Use clear, descriptive commit messages
   ```bash
   git commit -m "Add fraud detection middleware"
   ```

5. **Pull Request** - Submit PR with description of changes

## Code Style Guidelines

### General Principles
- **Keep it simple** - Favor clarity over cleverness
- **Single responsibility** - Each class/method does one thing well
- **Immutability** - Use immutable objects where possible (e.g., Account.id)
- **Null safety** - Validate inputs, use Optional for nullable returns

### Java Conventions
- **Naming**: camelCase for methods/variables, PascalCase for classes, ALL_CAPS for constants
- **Indentation**: 4 spaces (no tabs)
- **Line length**: Max 120 characters
- **Braces**: Always use braces for if/while/for blocks
- **Comments**: Javadoc for public APIs, inline comments for complex logic

### Example
```java
/**
 * Validates transfer amount and account IDs.
 * Throws IllegalArgumentException if validation fails.
 */
public class TransferValidationMiddleware implements TransferMiddleware {
    @Override
    public void process(TransferContext context, Runnable next) throws Exception {
        if (context.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        next.run();
    }
}
```

## Architecture Guidelines

### Thread Safety (CRITICAL)
- **Never use global locks** - Use fine-grained per-account locking
- **Lock ordering** - Always acquire locks in alphabetical order (prevents deadlocks)
- **Finally blocks** - Always release locks in finally blocks
- **ConcurrentHashMap** - Use for shared state, not synchronized
- **Immutable where possible** - Account.id is final, balance is mutable but protected

### Middleware Pattern
When adding new transfer processing logic:
1. Create a class implementing `TransferMiddleware`
2. Implement `process(TransferContext context, Runnable next)`
3. Perform your logic, then call `next.run()` to continue chain
4. Register in `TransferService` constructor in correct order

```java
// Example: Add daily limit check
public class DailyLimitMiddleware implements TransferMiddleware {
    public void process(TransferContext context, Runnable next) throws Exception {
        // Your validation logic
        if (exceedsLimit(context)) {
            throw new IllegalStateException("Daily limit exceeded");
        }
        next.run(); // Must call to continue chain
    }
}
```

### Repository Pattern
- Keep repository interface generic (supports multiple implementations)
- Never leak storage details to services
- Use `saveAll()` for atomic batch operations
- Return `Optional<Account>` for nullable results

## Testing Requirements

### Test Coverage
All PRs must include tests:
- ✅ **Unit tests** - Test individual classes in isolation
- ✅ **Integration tests** - Test API endpoints end-to-end
- ✅ **Concurrency tests** - Test thread safety if touching shared state

### Test Guidelines
```java
@Test
void shouldRejectNegativeTransferAmount() {
    // Given
    TransferContext context = new TransferContext("A", "B", new BigDecimal("-100"));

    // When/Then
    assertThrows(IllegalArgumentException.class,
        () -> middleware.process(context, () -> {}));
}
```

### Running Tests
```bash
mvn test                              # All tests
mvn test -Dtest=TransferServiceTest  # Specific test class
mvn clean test                        # Clean build + test
```

### Test Requirements
- All tests must pass before PR submission
- New features require corresponding tests
- Bug fixes should include regression tests
- Concurrency changes require concurrency tests

## Pull Request Guidelines

### Before Submitting
- [ ] All tests pass (`mvn test`)
- [ ] Code follows style guidelines
- [ ] New tests added for new functionality
- [ ] Documentation updated (README, ARCHITECTURE.md, etc.)
- [ ] No commented-out code or debug statements
- [ ] Thread safety considered if touching shared state

### PR Description Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
Describe tests added/modified

## Checklist
- [ ] Tests pass
- [ ] Documentation updated
- [ ] Thread-safe (if applicable)
```

## Common Pitfalls to Avoid

### ❌ DON'T
```java
// Global lock - kills performance
public synchronized TransferResult transfer(...) { }

// No lock release on error
lock.lock();
processTransfer();
lock.unlock(); // Won't execute if exception thrown!

// Unsafe batch operation
for (Account account : accounts) {
    storage.put(account.getId(), account); // Not atomic!
}
```

### ✅ DO
```java
// Fine-grained locking
LockPair locks = lockManager.acquireLocks(fromId, toId);
try {
    processTransfer();
} finally {
    lockManager.releaseLocks(locks); // Always executes
}

// Atomic batch operation
Map<String, Account> updates = new HashMap<>();
for (Account account : accounts) {
    updates.put(account.getId(), account);
}
storage.putAll(updates); // Single atomic operation
```

## Adding New Features

### Middleware (Recommended Approach)
```java
// 1. Create middleware class
public class YourMiddleware implements TransferMiddleware {
    public void process(TransferContext context, Runnable next) {
        // Your logic here
        next.run();
    }
}

// 2. Register in TransferService constructor
List<TransferMiddleware> middlewares = Arrays.asList(
    new TransferValidationMiddleware(),
    new AccountLoadingMiddleware(accountRepository),
    new YourMiddleware(), // Add here
    new TransactionFeeMiddleware(),
    new SufficientFundsMiddleware()
);
```

### Storage Backend
```java
// 1. Implement AccountRepository interface
public class PostgreSQLAccountRepository implements AccountRepository {
    // Implement all methods using JDBC/JPA
}

// 2. Update Application.java
AccountRepository repo = new PostgreSQLAccountRepository();
```

## Code Review Focus Areas

Reviewers will pay special attention to:
1. **Thread safety** - Proper locking, no race conditions
2. **Atomicity** - Operations succeed/fail completely
3. **Error handling** - Proper exceptions, cleanup in finally blocks
4. **Test coverage** - Adequate tests for changes
5. **Performance** - No global locks or O(n²) algorithms
6. **API consistency** - Follows existing patterns

## Resources

- **[README.md](README.md)** - Project overview and setup
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Architecture details and patterns
- **[LOCKING.md](LOCKING.md)** - Thread safety and locking strategy
- **[TESTING.md](TESTING.md)** - Test suite documentation
- **[PRODUCT.md](PRODUCT.md)** - Product features and API specification

## Questions?

- Check existing code for examples
- Review related documentation
- Ask in PR comments for clarification

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.

---

**Happy coding!** Thank you for helping make Simple Payment Ledger better! 🚀
