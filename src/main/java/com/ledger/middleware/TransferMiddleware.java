package com.ledger.middleware;

import com.ledger.transaction.TransferContext;

/**
 * Middleware interface for processing transfer transactions.
 * Middleware can be chained to add additional processing steps
 * (e.g., fee calculation, logging, auditing, fraud detection).
 */
@FunctionalInterface
public interface TransferMiddleware {
    /**
     * Process the transfer context.
     * Middleware can modify the context, perform validations,
     * or add additional data before the next middleware or executor runs.
     *
     * @param context The transfer context
     * @param next The next middleware in the chain
     * @throws Exception if processing fails
     */
    void process(TransferContext context, Runnable next) throws Exception;
}
