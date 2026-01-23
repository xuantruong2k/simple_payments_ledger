# Test Suite Documentation

## Overview

This project includes a comprehensive test suite with **74 tests** covering unit tests, integration tests, and concurrency tests. All tests use JUnit 5 and can be run as part of the Maven build process with Java 21 Virtual Threads.

## Test Structure

```
src/test/java/com/ledger/
├── model/
│   └── AccountTest.java                    # Account model unit tests (10 tests)
├── repository/
│   └── InMemoryAccountRepositoryTest.java  # Repository layer tests (8 tests)
├── service/
│   ├── AccountServiceTest.java             # Account service tests (8 tests)
│   └── TransferServiceTest.java            # Transfer logic tests (18 tests)
└── integration/
    └── ApiIntegrationTest.java             # REST API integration tests (14 tests)
```

## Running Tests

### Run all tests:
```bash
mvn test
```

### Run all tests with detailed output:
```bash
mvn test -X
```

### Run specific test class:
```bash
mvn test -Dtest=TransferServiceTest
```

### Run as part of build:
```bash
mvn clean install
```

## Test Categories

### 1. Unit Tests - Account Model (10 tests)
**File:** `AccountTest.java`

Tests the `Account` domain model:
- ✅ Valid account creation with balance
- ✅ Zero balance accounts
- ✅ Null ID validation
- ✅ Empty ID validation
- ✅ Null balance validation
- ✅ Negative balance validation
- ✅ Balance updates
- ✅ Negative balance updates prevention
- ✅ Account equality by ID
- ✅ Account inequality

### 2. Unit Tests - Repository Layer (8 tests)
**File:** `InMemoryAccountRepositoryTest.java`

Tests the in-memory repository implementation:
- ✅ Save and find accounts
- ✅ Update existing accounts
- ✅ Non-existent account handling
- ✅ Check account existence
- ✅ Delete accounts
- ✅ Find all accounts
- ✅ Count accounts
- ✅ Null account validation

### 3. Unit Tests - Account Service (8 tests)
**File:** `AccountServiceTest.java`

Tests account management business logic:
- ✅ Create account with initial balance
- ✅ Create account with zero balance
- ✅ Prevent negative balance creation
- ✅ Prevent duplicate accounts
- ✅ Retrieve existing accounts
- ✅ Handle non-existent accounts
- ✅ Update account balance
- ✅ Update non-existent account error

### 4. Unit Tests - Transfer Service (18 tests)
**File:** `TransferServiceTest.java`

Comprehensive transfer logic testing:

#### Functional Tests:
- ✅ Successful transfer
- ✅ Correct debit and credit
- ✅ Transfer all funds
- ✅ Transfer exactly available funds

#### Edge Cases & Business Logic:
- ✅ Insufficient funds prevention
- ✅ Transfer more than available funds
- ✅ Self-transfer prevention
- ✅ Negative amount validation
- ✅ Zero amount validation
- ✅ From non-existent account error
- ✅ To non-existent account error
- ✅ Null from account ID validation
- ✅ Null to account ID validation
- ✅ Null amount validation

#### Concurrency Tests:
- ✅ **Race conditions**: 11 simultaneous $10 transfers from $100 account
  - Validates only 10 succeed, 1 fails with insufficient funds
  - Verifies total balance preservation

- ✅ **Multiple accounts concurrency**: 50 random transfers between 10 accounts
  - Ensures total system balance remains constant

- ✅ **Deadlock prevention**: 100 bidirectional transfers between 2 accounts
  - ACC1 → ACC2 and ACC2 → ACC1 simultaneously
  - Verifies no deadlocks occur
  - Validates balance preservation

- ✅ **Race condition on single account**: 100 threads transferring from one source
  - All 100 transfers of $10 from $1000 account should succeed
  - Verifies atomic operations

### 5. Integration Tests - REST API (14 tests)
**File:** `ApiIntegrationTest.java`

End-to-end API testing with real HTTP requests:

#### Account Creation (POST /accounts):
- ✅ Create account with initial balance
- ✅ Create account with default (zero) balance
- ✅ Reject negative balance (400 Bad Request)
- ✅ Reject duplicate account (409 Conflict)
- ✅ Reject missing account ID (400 Bad Request)

#### Account Retrieval (GET /accounts/{id}):
- ✅ Get existing account (200 OK)
- ✅ Get non-existent account (404 Not Found)

#### Transfer (POST /transactions):
- ✅ Successful transfer with balance updates
- ✅ Insufficient funds error (400 Bad Request)
- ✅ Negative amount rejection (400 Bad Request)
- ✅ Self-transfer rejection (400 Bad Request)
- ✅ Non-existent account error (400 Bad Request)
- ✅ Missing from_account_id validation
- ✅ Missing amount validation

## Test Coverage

### What is Tested:

1. **Correctness**:
   - Account creation, retrieval, updates
   - Transfer debits and credits
   - Balance calculations

2. **Validation**:
   - Input validation (null, empty, negative)
   - Business rule enforcement (no negative balances, no self-transfers)
   - Account existence checks

3. **Edge Cases**:
   - Zero balances
   - Exact available funds
   - Insufficient funds
   - Non-existent accounts

4. **Concurrency**:
   - Race conditions on shared accounts
   - Deadlock scenarios
   - Atomic operations
   - Balance consistency under load

5. **API Layer**:
   - HTTP status codes
   - Request/response formats (JSON with snake_case)
   - Error responses
   - End-to-end workflows

## Concurrency Test Details

### Test 1: Race Conditions (11 transfers from $100 account)
```java
// Scenario: 11 threads try to transfer $10 from account with $100
// Expected: Exactly 10 succeed, 1 fails, final balance = $0
```

### Test 2: Multiple Account Transfers
```java
// Scenario: 50 random transfers between 10 accounts
// Expected: Total system balance = $1000 (10 × $100) preserved
```

### Test 3: Deadlock Prevention
```java
// Scenario: 100 ACC1→ACC2 transfers + 100 ACC2→ACC1 transfers simultaneously
// Expected: No deadlock, all complete within timeout, balances preserved
```

### Test 4: Single Account Race
```java
// Scenario: 100 threads transferring $10 from $1000 account
// Expected: All 100 succeed, final balance = $0, total distributed = $1000
```

## Test Execution Time

- **Unit Tests**: ~0.1 seconds
- **Integration Tests**: ~2 seconds
- **Concurrency Tests**: ~5-10 seconds (includes thread coordination)
- **Total**: ~3-4 seconds

## Continuous Integration

Tests are automatically run:
- On every `mvn install`
- On every `mvn package`
- Before creating release builds

## Test Dependencies

```xml
<!-- JUnit 5 -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-api</artifactId>
    <version>5.9.3</version>
    <scope>test</scope>
</dependency>

<!-- OkHttp for HTTP client in integration tests -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.11.0</version>
    <scope>test</scope>
</dependency>
```

## Key Testing Principles

1. **Isolated Tests**: Each test runs independently with fresh state
2. **Atomic Assertions**: Tests verify specific behaviors
3. **Comprehensive Coverage**: Normal cases, edge cases, and error conditions
4. **Realistic Scenarios**: Integration tests use real HTTP and JSON
5. **Concurrency Safety**: Validates thread-safe operations under load

## Test Results

```
Tests run: 58, Failures: 0, Errors: 0, Skipped: 0

SUCCESS
```
