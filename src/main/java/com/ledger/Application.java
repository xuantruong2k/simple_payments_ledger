package com.ledger;

import com.ledger.controller.AccountController;
import com.ledger.controller.TransactionController;
import com.ledger.handler.AccountHandler;
import com.ledger.handler.TransactionHandler;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.InMemoryAccountRepository;
import com.ledger.service.AccountService;
import com.ledger.service.TransferService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) {
        // Initialize repository
        AccountRepository accountRepository = new InMemoryAccountRepository();

        // Initialize services
        AccountService accountService = new AccountService(accountRepository);
        TransferService transferService = new TransferService(accountRepository);

        // Initialize handlers
        AccountHandler accountHandler = new AccountHandler(accountService);
        TransactionHandler transactionHandler = new TransactionHandler(transferService);

        // Initialize controllers
        AccountController accountController = new AccountController(accountHandler);
        TransactionController transactionController = new TransactionController(transactionHandler);

        // Start Javalin server
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson());
        }).start(7070);

        // Register routes
        accountController.registerRoutes(app);
        transactionController.registerRoutes(app);

        System.out.println("Simple Payment Ledger API is running on http://localhost:7070");
    }
}
