package com.example.bankaccount.audit;

import com.example.bankaccount.domain.Transaction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Batches transactions using a <b>Best-Fit Decreasing</b> bin-packing strategy: sort transactions in
 * descending order of absolute amount, then put each transaction in the open batch with the least remaining
 * capacity that can still hold it, opening a new batch only when none can.
 */
public class BestFitDecreasingStrategy implements BatchingStrategy {

    private final long batchLimitInPence;

    /**
     * @param batchLimitInPence maximum absolute value of a batch, e.g. 100_000_000 for £1,000,000
     */
    public BestFitDecreasingStrategy(long batchLimitInPence) {
        if (batchLimitInPence <= 0) {
            throw new IllegalArgumentException("batchLimitInPence must be positive but was " + batchLimitInPence);
        }
        this.batchLimitInPence = batchLimitInPence;
    }

    @Override
    public List<Batch> batch(List<Transaction> transactions) {
        // Largest first: big items are hardest to place, so place them while there's most room.
        List<Transaction> sorted = new ArrayList<>(transactions);
        sorted.sort(Comparator.comparingLong(Transaction::absoluteAmountInPence).reversed());

        // Sorted map of remaining capacity -> open batches with that capacity.
        TreeMap<Long, Deque<OpenBatch>> openBatchesByRemaining = new TreeMap<>();
        List<OpenBatch> allBatches = new ArrayList<>();

        for (Transaction transaction : sorted) {
            long amount = transaction.absoluteAmountInPence();

            // Best fit: the smallest remaining capacity that is still >= amount.
            Map.Entry<Long, Deque<OpenBatch>> bestFitEntry = openBatchesByRemaining.ceilingEntry(amount);

            OpenBatch bestFit;
            if (bestFitEntry == null) {
                bestFit = new OpenBatch(batchLimitInPence);  // nothing fits: open a new batch
                allBatches.add(bestFit);
            } else {
                // Remove it from the index before its remaining capacity changes.
                bestFit = bestFitEntry.getValue().poll();
                if (bestFitEntry.getValue().isEmpty()) {
                    openBatchesByRemaining.remove(bestFitEntry.getKey());
                }
            }

            bestFit.add(transaction, amount);
            openBatchesByRemaining
                    .computeIfAbsent(bestFit.remainingInPence(), key -> new ArrayDeque<>())
                    .add(bestFit);                                  // re-file under its new remaining capacity
        }

        return allBatches.stream().map(OpenBatch::toBatch).toList();
    }

    /** Working state for a batch being filled; converted to an immutable {@link Batch} at the end. */
    private static final class OpenBatch {

        private final List<Transaction> transactions = new ArrayList<>();
        private final long capacityInPence;
        private long remainingInPence;

        OpenBatch(long capacityInPence) {
            this.capacityInPence = capacityInPence;
            this.remainingInPence = capacityInPence;
        }

        long remainingInPence() {
            return remainingInPence;
        }

        void add(Transaction transaction, long amount) {
            transactions.add(transaction);
            remainingInPence -= amount;
        }

        Batch toBatch() {
            return new Batch(transactions, capacityInPence - remainingInPence);
        }
    }
}
