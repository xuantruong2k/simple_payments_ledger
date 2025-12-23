package com.ledger.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.Application;
import com.ledger.controller.AccountController;
import com.ledger.controller.TransactionController;
import com.ledger.handler.AccountHandler;
import com.ledger.handler.TransactionHandler;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.InMemoryAccountRepository;
import com.ledger.service.AccountService;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import okhttp3.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    private static final int TEST_PORT = 7777;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static Javalin app;
    private static OkHttpClient client;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void startServer() {
        objectMapper = new ObjectMapper();
        client = new OkHttpClient();

        AccountRepository accountRepository = new InMemoryAccountRepository();
        AccountService accountService = new AccountService(accountRepository);

        AccountHandler accountHandler = new AccountHandler(accountService);
        AccountController accountController = new AccountController(accountHandler);

        TransactionHandler transactionHandler = new TransactionHandler(accountService);
        TransactionController transactionController = new TransactionController(transactionHandler);

        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson());
        }).start(TEST_PORT);

        accountController.registerRoutes(app);
        transactionController.registerRoutes(app);

        // Give server time to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterAll
    static void stopServer() {
        if (app != null) {
            app.stop();
        }
    }

    // ==================== Account Creation Tests ====================

    @Test
    @Order(1)
    void testCreateAccountWithInitialBalance() throws IOException {
        String json = "{\"id\": \"ACC001\", \"initial_balance\": 1000.50}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(201, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("ACC001", responseMap.get("id"));
            assertEquals(1000.5, ((Number) responseMap.get("balance")).doubleValue(), 0.01);
        }
    }

    @Test
    @Order(2)
    void testCreateAccountWithDefaultBalance() throws IOException {
        String json = "{\"id\": \"ACC002\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(201, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("ACC002", responseMap.get("id"));
            assertEquals(0.0, ((Number) responseMap.get("balance")).doubleValue(), 0.01);
        }
    }

    @Test
    @Order(3)
    void testCreateAccountWithNegativeBalance() throws IOException {
        String json = "{\"id\": \"ACC_NEG\", \"initial_balance\": -100}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(400, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("BAD_REQUEST", responseMap.get("error"));
            assertTrue(responseMap.get("message").toString().contains("negative"));
        }
    }

    @Test
    @Order(4)
    void testCreateDuplicateAccount() throws IOException {
        String json = "{\"id\": \"ACC001\", \"initial_balance\": 500}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(409, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("CONFLICT", responseMap.get("error"));
            assertTrue(responseMap.get("message").toString().contains("already exists"));
        }
    }

    @Test
    @Order(5)
    void testCreateAccountWithMissingId() throws IOException {
        String json = "{\"initial_balance\": 100}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(400, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("BAD_REQUEST", responseMap.get("error"));
        }
    }

    // ==================== Account Retrieval Tests ====================

    @Test
    @Order(10)
    void testGetExistingAccount() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts/ACC001")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("ACC001", responseMap.get("id"));
            assertEquals(1000.5, ((Number) responseMap.get("balance")).doubleValue(), 0.01);
        }
    }

    @Test
    @Order(11)
    void testGetNonExistentAccount() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts/NONEXISTENT")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(404, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("NOT_FOUND", responseMap.get("error"));
            assertTrue(responseMap.get("message").toString().contains("not found"));
        }
    }

    // ==================== Transfer Tests ====================

    @Test
    @Order(20)
    void testSuccessfulTransfer() throws IOException {
        // Create two accounts for transfer
        createAccount("SENDER1", "500.00");
        createAccount("RECEIVER1", "200.00");

        String json = "{\"from_account_id\": \"SENDER1\", \"to_account_id\": \"RECEIVER1\", \"amount\": 150.00}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("SENDER1", responseMap.get("from_account_id"));
            assertEquals("RECEIVER1", responseMap.get("to_account_id"));
            assertEquals(150.0, ((Number) responseMap.get("amount")).doubleValue(), 0.01);
            assertEquals(350.0, ((Number) responseMap.get("from_account_balance")).doubleValue(), 0.01);
            assertEquals(350.0, ((Number) responseMap.get("to_account_balance")).doubleValue(), 0.01);
        }

        // Verify balances via GET
        verifyAccountBalance("SENDER1", 350.0);
        verifyAccountBalance("RECEIVER1", 350.0);
    }

    @Test
    @Order(21)
    void testTransferWithInsufficientFunds() throws IOException {
        createAccount("POOR", "50.00");
        createAccount("RICH", "1000.00");

        String json = "{\"from_account_id\": \"POOR\", \"to_account_id\": \"RICH\", \"amount\": 100.00}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(400, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("INSUFFICIENT_FUNDS", responseMap.get("error"));
            assertTrue(responseMap.get("message").toString().contains("Insufficient funds"));
        }

        // Verify balances unchanged
        verifyAccountBalance("POOR", 50.0);
        verifyAccountBalance("RICH", 1000.0);
    }

    @Test
    @Order(22)
    void testTransferWithNegativeAmount() throws IOException {
        createAccount("ACC_A", "500.00");
        createAccount("ACC_B", "200.00");

        String json = "{\"from_account_id\": \"ACC_A\", \"to_account_id\": \"ACC_B\", \"amount\": -50.00}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(400, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("BAD_REQUEST", responseMap.get("error"));
            assertTrue(responseMap.get("message").toString().contains("greater than zero"));
        }
    }

    @Test
    @Order(23)
    void testSelfTransfer() throws IOException {
        createAccount("SELF", "1000.00");

        String json = "{\"from_account_id\": \"SELF\", \"to_account_id\": \"SELF\", \"amount\": 100.00}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(400, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("BAD_REQUEST", responseMap.get("error"));
            assertTrue(responseMap.get("message").toString().contains("same account"));
        }

        // Verify balance unchanged
        verifyAccountBalance("SELF", 1000.0);
    }

    @Test
    @Order(24)
    void testTransferFromNonExistentAccount() throws IOException {
        createAccount("EXISTS", "500.00");

        String json = "{\"from_account_id\": \"NONEXISTENT\", \"to_account_id\": \"EXISTS\", \"amount\": 100.00}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(400, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("BAD_REQUEST", responseMap.get("error"));
            assertTrue(responseMap.get("message").toString().contains("not found"));
        }
    }

    @Test
    @Order(25)
    void testTransferMissingFromAccountId() throws IOException {
        String json = "{\"to_account_id\": \"ACC001\", \"amount\": 100.00}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(400, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("BAD_REQUEST", responseMap.get("error"));
            assertTrue(responseMap.get("message").toString().contains("from_account_id"));
        }
    }

    @Test
    @Order(26)
    void testTransferMissingAmount() throws IOException {
        String json = "{\"from_account_id\": \"ACC001\", \"to_account_id\": \"ACC002\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/transactions")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(400, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals("BAD_REQUEST", responseMap.get("error"));
            assertTrue(responseMap.get("message").toString().contains("amount"));
        }
    }

    // ==================== Helper Methods ====================

    private void createAccount(String id, String balance) throws IOException {
        String json = String.format("{\"id\": \"%s\", \"initial_balance\": %s}", id, balance);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertTrue(response.isSuccessful() || response.code() == 409); // 409 if already exists
        }
    }

    private void verifyAccountBalance(String accountId, double expectedBalance) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/accounts/" + accountId)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
            assertNotNull(response.body());

            String responseBody = response.body().string();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            assertEquals(expectedBalance, ((Number) responseMap.get("balance")).doubleValue(), 0.01);
        }
    }
}
