package com.example.bankaccount.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * An immutable credit or debit against the account.
 *
 * <p>All monetary values are held as a whole number of <b>pence</b> (GBP minor units) in a {@code long}
 * so that arithmetic is exact and cheap.
 *
 * <ul>
 *   <li>Credits have a <b>positive</b> amount, e.g. £250.00 credit → {@code amountInPence = 25_000}</li>
 *   <li>Debits have a <b>negative</b> amount, e.g. £250.00 debit → {@code amountInPence = -25_000}</li>
 * </ul>
 *
 * @param id            unique identifier of the transaction
 * @param amountInPence signed amount in pence; positive for credits, negative for debits
 */
public record Transaction(String id, long amountInPence) {

    public Transaction {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (amountInPence == 0) {
            throw new IllegalArgumentException("amountInPence must not be zero");
        }
    }

    /**
     * Creates a credit.
     *
     * @param id    unique identifier
     * @param pence size of the credit in pence; must be positive
     */
    public static Transaction credit(String id, long pence) {
        requirePositive(pence);
        return new Transaction(id, pence);
    }

    /**
     * Creates a debit. The magnitude is passed as a positive number and stored as a negative amount.
     *
     * @param id    unique identifier
     * @param pence size of the debit in pence; must be positive
     */
    public static Transaction debit(String id, long pence) {
        requirePositive(pence);
        return new Transaction(id, -pence);
    }

    public boolean isCredit() {
        return amountInPence > 0;
    }

    public boolean isDebit() {
        return amountInPence < 0;
    }

    /**
     * The unsigned size of the transaction in pence. Used by the audit batching, where credits and
     * debits do not offset each other.
     */
    public long absoluteAmountInPence() {
        return Math.abs(amountInPence);
    }

    /**
     * The signed amount converted to pounds, e.g. {@code -25_000} pence → {@code -250.00}.
     * Intended for display and API boundaries only.
     */
    public BigDecimal amountInPounds() {
        return BigDecimal.valueOf(amountInPence, 2);
    }

    private static void requirePositive(long pence) {
        if (pence <= 0) {
            throw new IllegalArgumentException("pence must be positive but was " + pence);
        }
    }
}
