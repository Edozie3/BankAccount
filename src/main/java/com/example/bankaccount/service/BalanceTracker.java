package com.example.bankaccount.service;

import com.example.bankaccount.domain.Transaction;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks the account balance and hands every processed transaction to the audit queue.
 */
public class BalanceTracker implements BankAccountService {

    private static final double PENCE_PER_POUND = 100.0;

    private final BlockingQueue<Transaction> auditQueue;
    private final AtomicLong balanceInPence = new AtomicLong();

    /**
     * @param auditQueue bounded queue that the audit pipeline consumes transactions from
     */
    public BalanceTracker(BlockingQueue<Transaction> auditQueue) {
        this.auditQueue = Objects.requireNonNull(auditQueue, "auditQueue must not be null");
    }

    /**
     * Queues the transaction for audit, then applies it to the balance.
     */
    @Override
    public void processTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction must not be null");
        try {
            auditQueue.put(transaction);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while queueing transaction " + transaction.id(), e);
        }
        balanceInPence.addAndGet(transaction.amountInPence());
    }

    /**
     * The current balance in pounds.
     */
    @Override
    public double retrieveBalance() {
        return balanceInPence.get() / PENCE_PER_POUND;
    }
}
