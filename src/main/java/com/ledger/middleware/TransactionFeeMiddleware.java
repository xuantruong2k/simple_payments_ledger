package com.ledger.middleware;

import com.ledger.transaction.TransferContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Middleware to calculate transaction fees.
 * This is a placeholder for future fee calculation logic.
 * Currently applies no fee, but can be easily extended.
 */
public class TransactionFeeMiddleware implements TransferMiddleware {
    private static final BigDecimal FEE_PERCENTAGE = BigDecimal.ZERO; // 0% fee (can be changed later)
    private static final BigDecimal FIXED_FEE = BigDecimal.ZERO; // No fixed fee (can be changed later)

    @Override
    public void process(TransferContext context, Runnable next) throws Exception {
        // Calculate fee (example: percentage-based + fixed)
        BigDecimal percentageFee = context.getAmount()
            .multiply(FEE_PERCENTAGE)
            .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal totalFee = percentageFee.add(FIXED_FEE);
        
        // Set fee in context
        context.setFee(totalFee);
        
        // Effective amount remains the same (fees are separate)
        // If you want to deduct fee from transfer: context.setEffectiveAmount(context.getAmount().subtract(totalFee));

        // Proceed to next middleware
        next.run();
    }
}
