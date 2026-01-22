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
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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

        // Start Javalin server with Virtual Thread support (Java 21)
        // Configure Jetty to use Virtual Threads for handling HTTP requests
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson());

            // Create a ThreadFactory that spawns virtual threads
            ThreadFactory virtualThreadFactory = Thread.ofVirtual().factory();

            // Configure Jetty thread pool with virtual thread factory
            // Virtual threads are lightweight (~1KB each), so we can safely use a large pool
            // 10,000 virtual threads = only ~10MB memory vs 10GB for platform threads
            config.jetty.threadPool = new QueuedThreadPool(10000, 10, 60000, -1,
                null, null, virtualThreadFactory);
        }).start(7070);

        // Register routes
        accountController.registerRoutes(app);
        transactionController.registerRoutes(app);

        System.out.println("Simple Payment Ledger API is running on http://localhost:7070");
        System.out.println("Using Java " + Runtime.version() + " with Virtual Threads enabled");
    }
}
