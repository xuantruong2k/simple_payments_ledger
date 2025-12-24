# prompt 1: I told AI to initial project
I am bulding a simple transaction ledger API in Java. I need to manage accounts with ID and balance. Create a Java project structure that follow best-practices. This project must use in-memory map to store data (user account) but hide that storage behind a 'Repository' interface so that I can easilly swap for using external database later. Please define the Account model with ID and balance

# prompt 2: I told AI to implement account logic and API handler (create / get)
implement the following RESTful routes:
1. POST /accounts to create a new account with an initial balance (default value is 0).
2. GET /accounts/{account_id} to fetch account detail.
Require the code structure to controller/handler pattern to separate the route definitions and the handling logic.

# prompt 3: told AI to make sure request 's param or body follows snake_case
update api request's parameter follows snake_case not camelCase

# prompt 4: implement the transfer logic
implement transfer function, take input fromAccountId, toAccountId and amount. The logic must ensure:
1. sender has enought funds (no negative balances allowed, else return 400-style error).
2. The debit and credit happen atomicity (they must succeed or failed together). Implement route POST /transactions to call that function, remember to validate the request 's body

# prompt 5: told AI to write test cases, these must cover both normal and edge-case
write comprehensive test suite, this need to verify the correctness of ledger under both normal and edge-case conditions. Generate the following tests:
1. Functional Unit Tests:
  1.1 Account creation: create account with correct initial balance and negative balance to verify api POST /accounts;
  1.2 Account retrieval: verify api GET /accounts/{accound_id};
  1.3 verify POST /transactions to correctly debits the sender and credits the receiver.
2. Business Logic and edge cases:
  2.1 insufficient funnds: transfer negative balanc, transfer amount which more then the sender's amount, make sure 400 bad request and no funds are move.
  2.2 Self-transfer: what happens if transfer money to same account.
3. Concurrency and Atomic Tests:
  3.1 Race conditions: simulate multiple concurent transfers from the same account, e.g if 11 simultaneous transfers of $10 from $100 account.
  3.2 Deadlock prevention: simulate 2 accounts transferring to each others and make sure the system doesn't freeze.
Remember to use a standard testing framework and ensure all tests are easy to run as part of building process

# prompt 6: The AI's first draft of the transfer function used a global lock. I recognized this wouldn't scale to 100k users, so I prompted it to switch to per-account mutexes and specifically asked it to handle potential deadlocks via ID-based ordering
the current implementation uses global lock which will bottlenect the API for 100K users. 1. Refactor this to use fine-grained locking (one mutext per account). 2. implement a lock-ordering strategy based on Account IDs to prevent deadlocks during transfers

# prompt 7: ensure the generated code by AI is easy to refactor later
Ensure the code is easy to refactor. If I want to add 'transaction fee' later, the logic go without breaking the core transfer. Refactor the trnasfer logic to use 'middleware' approach to make sure the business logic is decoupled from API routing

# prompt 8: AI's first create account is missing thread-safety, 2 accounts with same ID and different initial balance could override each-other. I also discovered that the transfer debit & credit was missing atomicity after change to use per-account locking instead of global locking. So I had to told the AI to check and update the logic
review whole project and make sure all logic are thread-safety if neccessary, specially create account, update account's balance, transfer money... And make sure atomictiy on transfer function, both debit and credit must succeed or fail together

# prompt 9: update documentation due to outdate after all the changes
review whole project and update all *.md document if neccessary