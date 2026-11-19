package com.example.bankaccount.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.example.bankaccount.domain.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BalanceTrackerTest {

    private final BlockingQueue<Transaction> auditQueue = new ArrayBlockingQueue<>(100_000);
    private final BalanceTracker balanceTracker = new BalanceTracker(auditQueue);

    @Test
    void givenNewAccount_whenRetrievingBalance_thenBalanceIsZero() {
        // given: a newly created balance tracker

        // when
        double balance = balanceTracker.retrieveBalance();

        // then
        assertThat(balance).isZero();
    }

    @Test
    void givenCredit_whenProcessed_thenBalanceIncreasesByAmount() {
        // given
        Transaction credit = Transaction.credit("c-1", 50_000); // £500.00

        // when
        balanceTracker.processTransaction(credit);

        // then
        assertThat(balanceTracker.retrieveBalance()).isEqualTo(500.00);
    }

    @Test
    void givenDebit_whenProcessed_thenBalanceDecreasesByAmount() {
        // given
        Transaction debit = Transaction.debit("d-1", 20_050); // £200.50

        // when
        balanceTracker.processTransaction(debit);

        // then
        assertThat(balanceTracker.retrieveBalance()).isEqualTo(-200.50);
    }

    @Test
    void givenMixedCreditsAndDebits_whenProcessed_thenBalanceIsTheirNetSum() {
        // given
        List<Transaction> transactions = List.of(
                Transaction.credit("c-1", 50_000),  // +£500.00
                Transaction.debit("d-1", 20_050),   // -£200.50
                Transaction.credit("c-2", 75));     // +£0.75

        // when
        transactions.forEach(balanceTracker::processTransaction);

        // then
        assertThat(balanceTracker.retrieveBalance()).isEqualTo(300.25);
    }

    @Test
    void givenManyThreadsProcessingConcurrently_whenAllFinished_thenBalanceEqualsExactSum() throws Exception {
        // given: each thread alternates £1.50 credits and £1.00 debits, so nets £0.50 per pair
        int threadCount = 8;
        int transactionsPerThread = 10_000;
        long expectedBalanceInPence = (long) threadCount * (transactionsPerThread / 2) * (150 - 100);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> workers = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            int threadIndex = t;
            workers.add(() -> {
                startGate.await();
                for (int i = 0; i < transactionsPerThread; i++) {
                    String id = threadIndex + "-" + i;
                    balanceTracker.processTransaction(i % 2 == 0
                            ? Transaction.credit(id, 150)
                            : Transaction.debit(id, 100));
                }
                return null;
            });
        }

        // when
        try {
            List<Future<Void>> results = new ArrayList<>();
            workers.forEach(worker -> results.add(executor.submit(worker)));
            startGate.countDown(); // release all threads at once to maximise contention
            for (Future<Void> result : results) {
                result.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        // then
        assertThat(balanceTracker.retrieveBalance()).isEqualTo(expectedBalanceInPence / 100.0);
    }

    @Test
    void givenTransaction_whenProcessed_thenItIsPlacedOnTheAuditQueue() {
        // given
        Transaction first = Transaction.credit("c-1", 50_000);
        Transaction second = Transaction.debit("d-1", 20_050);
        Transaction third = Transaction.credit("c-2", 75);

        // when
        balanceTracker.processTransaction(first);
        balanceTracker.processTransaction(second);
        balanceTracker.processTransaction(third);

        // then
        assertThat(auditQueue).containsExactly(first, second, third);
    }

    @Test
    @SuppressWarnings("unchecked")
    void givenQueuePutInterrupted_whenProcessing_thenBalanceUnchanged() throws InterruptedException {
        // given
        BlockingQueue<Transaction> queue = mock(BlockingQueue.class);
        doThrow(new InterruptedException()).when(queue).put(any());
        BalanceTracker tracker = new BalanceTracker(queue);

        // when
        Throwable thrown = catchThrowable(() -> tracker.processTransaction(Transaction.credit("c-1", 50_000)));
        Thread.interrupted();

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException.class);
        assertThat(tracker.retrieveBalance()).isZero();
    }
}
