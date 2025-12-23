package com.ledger.examples;

import com.ledger.middleware.*;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.InMemoryAccountRepository;
import com.ledger.service.AccountService;
import com.ledger.service.TransferService;
import com.ledger.transaction.TransferContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * Example: Adding transaction fees without breaking existing code
 * Run: mvn exec:java -Dexec.mainClass="com.ledger.examples.TransactionFeeExample"
 */
public class TransactionFeeExample {
    
    public static class CustomFeeMiddleware implements TransferMiddleware {
        private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.01");
        private static final BigDecimal FIXED_FEE = new BigDecimal("0.50");

        @Override
        public void process(TransferContext context, Runnable next) throws Exception {
            BigDecimal percentageFee = context.getAmount()
                .multiply(FEE_PERCENTAGE)
                .setScale(2, RoundingMode.HALF_UP);
            
            BigDecimal totalFee = percentageFee.add(FIXED_FEE);
            context.setFee(totalFee);
            
            System.out.println("Fee: $" + totalFee);
            next.run();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Transaction Fee Example ===\n");
        
        AccountRepository repository = new InMemoryAccountRepository();
        AccountService accountService = new AccountService(repository);
        
        accountService.createAccount("ALICE", new BigDecimal("1000.00"));
        accountService.createAccount("BOB", new BigDecimal("500.00"));
        
        System.out.println("Initial: Alice=$1000, Bob=$500\n");
        
        TransferService transferService = new TransferService(
            repository,
            Arrays.asList(
                new TransferValidationMiddleware(),
                new AccountLoadingMiddleware(repository),
                new CustomFeeMiddleware(),
                new SufficientFundsMiddleware()
            )
        );
        
        System.out.println("Transfer $100 from Alice to Bob...");
        TransferService.TransferResult result = transferService.transfer(
            "ALICE", "BOB", new BigDecimal("100.00")
        );
        
        System.out.println("\nResult:");
        System.out.println("  Amount: $" + result.getAmount());
        System.out.println("  Fee: $" + result.getFee());
        System.out.println("  Alice: $" + result.getFromAccount().getBalance());
        System.out.println("  Bob: $" + result.getToAccount().getBalance());
    }
}
