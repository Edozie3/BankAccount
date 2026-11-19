package com.example.bankaccount.producer;

import com.example.bankaccount.transaction.Transaction;
import com.example.bankaccount.transaction.TransactionType;
import java.util.Objects;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Creates random transactions with a generated id and an amount between £200 and £500,000 inclusive.
 */
public class TransactionGenerator {

    /** £200 in pence. */
    static final long MIN_AMOUNT_IN_PENCE = 20_000L;

    /** £500,000 in pence. */
    static final long MAX_AMOUNT_IN_PENCE = 50_000_000L;

    private final RandomGenerator random;

    /**
     * @param random source of randomness for amounts;
     */
    public TransactionGenerator(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /**
     * Generates one transaction of the given type.
     */
    public Transaction generate(TransactionType type) {
        String id = UUID.randomUUID().toString();
        // nextLong's upper bound is exclusive, so add one to include £500,000 itself.
        long pence = random.nextLong(MIN_AMOUNT_IN_PENCE, MAX_AMOUNT_IN_PENCE + 1);

        return switch (type) {
            case CREDIT -> Transaction.credit(id, pence);
            case DEBIT -> Transaction.debit(id, pence);
        };
    }
}
