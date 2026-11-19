package com.example.bankaccount.audit;

import com.example.bankaccount.transaction.Transaction;
import java.util.List;

/**
 * Splits the transactions of one submission into batches.
 */
public interface BatchingStrategy {

    /**
     * @param transactions the transactions of one submission
     * @return batches that together contain every transaction exactly once
     */
    List<Batch> batch(List<Transaction> transactions);
}
