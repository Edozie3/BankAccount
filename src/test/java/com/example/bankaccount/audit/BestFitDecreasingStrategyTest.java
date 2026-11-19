package com.example.bankaccount.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bankaccount.domain.Transaction;
import com.example.bankaccount.domain.TransactionType;
import com.example.bankaccount.producer.TransactionGenerator;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BestFitDecreasingStrategyTest {

    private static final long LIMIT_IN_PENCE = 100_000_000L;   // £1,000,000
    private static final long POUND = 100L;
    private static final int SUBMISSION_SIZE = 1000;
    private static final long SEED = 42L;

    private final BatchingStrategy strategy = new BestFitDecreasingStrategy(LIMIT_IN_PENCE);

    @Test
    void givenRandomSubmission_whenBatched_thenNoBatchExceedsLimit() {
        // given
        List<Transaction> submission = randomSubmission(SUBMISSION_SIZE, SEED);

        // when
        List<Batch> batches = strategy.batch(submission);

        // then
        assertThat(batches).allSatisfy(batch ->
                assertThat(batch.totalValueInPence()).isLessThanOrEqualTo(LIMIT_IN_PENCE));
    }

    @Test
    void givenCreditAndDebitOf600k_whenBatched_thenTheyAreNotPlacedTogether() {
        // given: if a debit offset a credit their signed sum would be £0 and they would share one batch
        Transaction credit = Transaction.credit("c-1", 600_000 * POUND);
        Transaction debit = Transaction.debit("d-1", 600_000 * POUND);

        // when
        List<Batch> batches = strategy.batch(List.of(credit, debit));

        // then: by absolute value they total £1,200,000, so they need two batches
        assertThat(batches).hasSize(2);
        assertThat(batches).allSatisfy(batch -> {
            assertThat(batch.countOfTransactions()).isEqualTo(1);
            assertThat(batch.totalValueInPence()).isEqualTo(600_000 * POUND);
        });
    }


    @Test
    void givenRandomSubmission_whenBatched_thenEveryTransactionAppearsInExactlyOneBatch() {
        // given
        List<Transaction> submission = randomSubmission(SUBMISSION_SIZE, SEED);

        // when
        List<Batch> batches = strategy.batch(submission);

        // then: flattening the batches gives back the input exactly - nothing lost, nothing duplicated
        List<Transaction> batched = batches.stream().flatMap(batch -> batch.transactions().stream()).toList();
        assertThat(batched).hasSize(SUBMISSION_SIZE);
        assertThat(batched).containsExactlyInAnyOrderElementsOf(submission);
    }

    /**
     * Checks the packing is optimal on a case small enough to verify by hand: the best possible result is
     * 2 batches, each pairing a £600,000 transaction with a £400,000 one.
     */
    @Test
    void givenHandCheckedCase_whenBatched_thenOptimalNumberOfBatches() {
        // given
        List<Transaction> transactions = List.of(
                Transaction.credit("c-1", 600_000 * POUND),
                Transaction.debit("d-1", 400_000 * POUND),
                Transaction.debit("d-2", 600_000 * POUND),
                Transaction.credit("c-2", 400_000 * POUND));

        // when
        List<Batch> batches = strategy.batch(transactions);

        // then
        assertThat(batches).hasSize(2);
        assertThat(batches).allSatisfy(batch ->
                assertThat(batch.totalValueInPence()).isEqualTo(LIMIT_IN_PENCE));
    }

    /**
     * No set of batches can hold the total in fewer than ⌈total ÷ limit⌉ batches, so matching that count means
     * the packing is optimal. The fixed seed keeps the data, and therefore the expected count, the same each run.
     */
    @Test
    void givenRandomSubmission_whenBatched_thenBatchCountEqualsLowerBound() {
        // given
        List<Transaction> submission = randomSubmission(SUBMISSION_SIZE, SEED);
        long totalInPence = submission.stream().mapToLong(Transaction::absoluteAmountInPence).sum();
        long lowerBound = (totalInPence + LIMIT_IN_PENCE - 1) / LIMIT_IN_PENCE;   // ceiling division

        // when
        List<Batch> batches = strategy.batch(submission);

        // then
        assertThat(batches).hasSize((int) lowerBound);
    }

    /**
     * A random submission of alternating credits and debits with amounts between £200 and £500,000).
     */
    private static List<Transaction> randomSubmission(int size, long seed) {
        TransactionGenerator generator = new TransactionGenerator(new Random(seed));
        return IntStream.range(0, size)
                .mapToObj(i -> generator.generate(i % 2 == 0 ? TransactionType.CREDIT : TransactionType.DEBIT))
                .toList();
    }
}
