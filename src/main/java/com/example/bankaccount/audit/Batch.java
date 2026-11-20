package com.example.bankaccount.audit;

import com.example.bankaccount.transaction.Transaction;
import java.util.List;

/**
 * A group of transactions sent to the audit system together.
 *
 * @param transactions      the transactions in this batch
 * @param totalValueInPence sum of the absolute value amounts of the transactions, in pence
 */
public record Batch(List<Transaction> transactions, long totalValueInPence) {

    public Batch {
        transactions = List.copyOf(transactions);
    }

    public int countOfTransactions() {
        return transactions.size();
    }
}
