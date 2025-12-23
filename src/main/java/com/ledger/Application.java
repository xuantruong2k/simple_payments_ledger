package com.ledger;

import com.ledger.controller.AccountController;
import com.ledger.controller.TransactionController;
import com.ledger.handler.AccountHandler;
import com.ledger.handler.TransactionHandler;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.InMemoryAccountRepository;
import com.ledger.service.AccountService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

public class Application {
    public static void main(String[] args) {
        AccountRepository accountRepository = new InMemoryAccountRepository();
        AccountService accountService = new AccountService(accountRepository);
        
        AccountHandler accountHandler = new AccountHandler(accountService);
        AccountController accountController = new AccountController(accountHandler);
        
        TransactionHandler transactionHandler = new TransactionHandler(accountService);
        TransactionController transactionController = new TransactionController(transactionHandler);

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson());
        }).start(8080);

        accountController.registerRoutes(app);
        transactionController.registerRoutes(app);

        System.out.println("Simple Payment Ledger API is running on http://localhost:8080");
    }
}
