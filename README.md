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

## REST API

### Starting the Application

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.ledger.Application"
```

The API will start on `http://localhost:7070`

### Available Endpoints

#### Create Account
```bash
POST /accounts
Content-Type: application/json

{
  "id": "ACC001",
  "initial_balance": 1000.50  # Optional, defaults to 0
}

# Response (201 Created)
{
  "id": "ACC001",
  "balance": 1000.50
}
```

#### Get Account
```bash
GET /accounts/{account_id}

# Response (200 OK)
{
  "id": "ACC001",
  "balance": 1000.50
}

# Error Response (404 Not Found)
{
  "error": "NOT_FOUND",
  "message": "Account not found: ACC001"
}
```

#### Transfer Money
```bash
POST /transactions
Content-Type: application/json

{
  "from_account_id": "ACC001",
  "to_account_id": "ACC002",
  "amount": 300.00
}

# Response (200 OK)
{
  "from_account_id": "ACC001",
  "to_account_id": "ACC002",
  "amount": 300.00,
  "from_account_balance": 700.50,
  "to_account_balance": 1300.00
}

# Error Response (400 Bad Request - Insufficient Funds)
{
  "error": "INSUFFICIENT_FUNDS",
  "message": "Insufficient funds in account: ACC001"
}
```

### Transfer Logic
The transfer function ensures:
- **Atomic operations**: Both debit and credit succeed or fail together (using synchronized method)
- **No negative balances**: Sender must have sufficient funds before transfer
- **Validation**: All fields are validated (account IDs exist, amount > 0, different accounts)

### Error Responses
- `400 BAD_REQUEST` - Invalid input data, insufficient funds
- `404 NOT_FOUND` - Account not found
- `409 CONFLICT` - Account already exists
- `500 INTERNAL_ERROR` - Server error

## Architecture

The project follows the **Controller/Handler** pattern:
- **Controller** - Defines routes and maps them to handlers
- **Handler** - Contains the business logic for each endpoint
- **Service** - Business logic layer
- **Repository** - Data access abstraction

### Code Structure
```
controller/
  └── AccountController.java    # Route definitions
handler/
  └── AccountHandler.java       # Request/response handling logic
dto/
  ├── CreateAccountRequest.java # Request DTOs
  ├── AccountResponse.java      # Response DTOs
  └── ErrorResponse.java        # Error DTOs
```

## Usage Example (Programmatic)

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
- Transaction management
- Database implementation of AccountRepository
- Authentication and authorization
- Rate limiting
