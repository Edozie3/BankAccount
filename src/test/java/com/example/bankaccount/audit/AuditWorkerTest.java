package com.example.bankaccount.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankaccount.transaction.Transaction;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditWorkerTest {

    private static final int SUBMISSION_SIZE = 1000;
    private static final long MAX_WAIT_MILLIS = 5_000;
    private static final long QUIET_PERIOD_MILLIS = 300;

    private final BlockingQueue<Transaction> auditQueue = new ArrayBlockingQueue<>(10_000);
    private final BatchingStrategy batchingStrategy = mock(BatchingStrategy.class);
    private final AuditPublisher auditPublisher = mock(AuditPublisher.class);
    private final AuditWorker auditWorker =
            new AuditWorker(auditQueue, SUBMISSION_SIZE, batchingStrategy, auditPublisher);

    @BeforeEach
    void stubBatchingStrategy() {
        when(batchingStrategy.batch(any())).thenReturn(List.of());
    }

    @AfterEach
    void stopWorker() {
        auditWorker.stop();
    }

    @Test
    @SuppressWarnings("unchecked")
    void givenExactly1000QueuedTransactions_whenWorkerRuns_thenOneSubmissionOf1000IsBatchedAndPublished() {
        // given
        List<Transaction> transactions = transactions(1000);
        auditQueue.addAll(transactions);

        // when
        auditWorker.start();

        // then
        ArgumentCaptor<List<Transaction>> batched = ArgumentCaptor.forClass(List.class);
        verify(batchingStrategy, timeout(MAX_WAIT_MILLIS)).batch(batched.capture());
        assertThat(batched.getValue()).containsExactlyElementsOf(transactions);
        verify(auditPublisher, timeout(MAX_WAIT_MILLIS)).publish(any());
    }

    @Test
    void given999QueuedTransactions_whenWorkerRuns_thenNothingIsPublished() {
        // given
        auditQueue.addAll(transactions(999));

        // when
        auditWorker.start();

        // then
        verify(auditPublisher, after(QUIET_PERIOD_MILLIS).never()).publish(any());
        verify(batchingStrategy, never()).batch(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void given2500QueuedTransactions_whenWorkerRuns_thenTwoSubmissionsAndNoTransactionLostOrRepeated() {
        // given
        List<Transaction> transactions = transactions(2500);
        auditQueue.addAll(transactions);

        // when
        auditWorker.start();

        // then: exactly two submissions, even after waiting, because the last 500 cannot fill a third
        ArgumentCaptor<List<Transaction>> batched = ArgumentCaptor.forClass(List.class);
        verify(batchingStrategy, timeout(MAX_WAIT_MILLIS).times(2)).batch(batched.capture());
        verify(batchingStrategy, after(QUIET_PERIOD_MILLIS).times(2)).batch(any());
        List<List<Transaction>> submissions = batched.getAllValues();
        assertThat(submissions.get(0)).containsExactlyElementsOf(transactions.subList(0, 1000));
        assertThat(submissions.get(1)).containsExactlyElementsOf(transactions.subList(1000, 2000));
    }

    @Test
    void givenBatchesFromStrategy_whenSubmitting_thenPublisherReceivesThoseBatches() {
        // given
        List<Transaction> transactions = transactions(1000);
        List<Batch> batches = List.of(
                new Batch(transactions.subList(0, 600), 1L),
                new Batch(transactions.subList(600, 1000), 2L));
        when(batchingStrategy.batch(any())).thenReturn(batches);
        auditQueue.addAll(transactions);

        // when
        auditWorker.start();

        // then
        verify(auditPublisher, timeout(MAX_WAIT_MILLIS)).publish(new Submission(batches));
    }

    private static List<Transaction> transactions(int toExclusive) {
        return IntStream.range(0, toExclusive)
                .mapToObj(i -> Transaction.credit("tx-" + i, 20_000 + i))
                .toList();
    }
}
