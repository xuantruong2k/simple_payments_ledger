Backend SDE Take-Home Assessment: Simple Payments Ledger API
Hello! Thank you for taking the time to complete this exercise.
The goal of this task is to give us a sense of your engineering judgment and problem-solving
process. In modern development, we use many tools—from IDEs to AI assistants—to build
solutions. This assignment is designed to see how you leverage those tools and then apply
your own systems design knowledge to create a robust foundation.
Note on AI Usage (Please Read)
You are encouraged to use any AI coding assistant you like. We are less interested in your
ability to write boilerplate code from scratch and more interested in:
1. 2. 3. How you decompose a problem into prompts.
How you evaluate and refine the code generated.
Your understanding of the tradeoffs in the solution.
You are 100% responsible for the correctness, quality, and design of the code you submit.
Time Commitment
Please spend no more than 3 hours on this task. We value your time. We are looking for a
solid starting point for our conversation, not a fully production-ready, polished system.
If you run out of time, please submit what you have. A partially completed submission with
thoughtful notes is far more valuable to us than an over-polished one.
Objective
Your task is to build a simple, in-memory transactional ledger API for Grab’s early days (approx.
100K users). This API will manage accounts with balances and allow for the transfer of funds
between them.
Using an in-memory data store (e.g., a map/dictionary) is required for simplicity of this take
home assignment. Do not use an external database but you may write the project so that
is easy to move to using DB as solution.
Core Requirements
1. API Endpoints:
○
POST /accounts : Creates a new account with an initial balance.
○
GET /accounts/{accountId} : Retrieves the details of a specific account.
○
POST /transactions : Creates a new transaction, transferring funds from one
account to another.
2. Business Logic:
○
Atomicity: A transaction must be atomic. The debit and credit must succeed or
fail together.
○
Funds Check: An account cannot have a negative balance. The API must
prevent any transaction that would result in a negative balance for the sending
account (return a 400 Bad Request ).
Submission
1. 2. 3. Your Code: In any common backend language.
README.md : This is the most important part of your submission. It must include:
○
Build & Run Instructions: Clear steps on how to build and run your application.
○
Design Rationale: Briefly explain 1-2 key design choices you made (e.g., your
data structure, your approach to concurrency/atomicity).
○
Verification: How did you verify your solution is correct?
○
Refactoring Safety: How have you structured your code so that another
engineer could safely refactor or add a new, related feature (like 'transaction
fees') without breaking the existing transfer logic?
prompt.md : (Mandatory if you used AI). This is a key part of your submission. Please
include the key prompts you used and any insights you had while refining the AI's output.
(e.g.,
"AI's first draft forgot thread-safety, so I had to...
").
This exercise will be the foundation for our technical conversation, where we'll start by reviewing
your solution and then discuss how to evolve it. Good luck!