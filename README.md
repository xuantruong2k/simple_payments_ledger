# Simple Payment Ledger API

A Java-based transaction ledger API for managing accounts with IDs and balances.

## Project Structure

```
simple_payment_ledger/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── ledger/
│   │               ├── model/           # Domain models
│   │               │   └── Account.java
│   │               ├── repository/      # Data access layer
│   │               │   ├── AccountRepository.java (interface)
│   │               │   └── InMemoryAccountRepository.java
│   │               ├── service/         # Business logic layer
│   │               │   └── AccountService.java
│   │               └── controller/      # API controllers (future)
│   └── test/
│       └── java/
│           └── com/
│               └── ledger/
├── pom.xml
└── README.md
```

## Architecture

### Repository Pattern
The project uses the **Repository Pattern** to abstract data storage:
- `AccountRepository` - Interface defining data operations
- `InMemoryAccountRepository` - In-memory implementation using ConcurrentHashMap
- Easy to swap implementations (e.g., JPA, JDBC, MongoDB) without changing business logic

### Layers
1. **Model Layer** - Domain entities (Account)
2. **Repository Layer** - Data access abstraction
3. **Service Layer** - Business logic and transaction management

## Account Model

The `Account` class includes:
- `id` (String) - Unique account identifier
- `balance` (BigDecimal) - Account balance (non-negative)
- Validation for null/empty IDs and negative balances
- Uses BigDecimal for precise monetary calculations

## Building and Running

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Build
```bash
mvn clean install
```

### Run Tests
```bash
mvn test
```

## Usage Example

```java
// Create repository
AccountRepository repository = new InMemoryAccountRepository();

// Create service
AccountService service = new AccountService(repository);

// Create account
Account account = service.createAccount("ACC001", new BigDecimal("1000.00"));

// Get account
Optional<Account> found = service.getAccount("ACC001");

// Update balance
service.updateBalance("ACC001", new BigDecimal("1500.00"));

// Get all accounts
List<Account> accounts = service.getAllAccounts();
```

## Future Enhancements
- REST API controllers
- Transaction management
- Database implementation of AccountRepository
- Authentication and authorization
