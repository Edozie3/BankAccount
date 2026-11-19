package com.example.bankaccount.audit;

import com.example.bankaccount.domain.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes transactions off the audit queue. When it has collected exactly
 * {@code submissionSize} transactions it batches them and publishes the resulting submission.
 */
public class AuditWorker {

    private static final Logger log = LoggerFactory.getLogger(AuditWorker.class);
    private static final String THREAD_NAME = "audit-worker";
    private static final long SHUTDOWN_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(5);

    private final BlockingQueue<Transaction> auditQueue;
    private final int submissionSize;
    private final BatchingStrategy batchingStrategy;
    private final AuditPublisher auditPublisher;
    private Thread workerThread;

    public AuditWorker(BlockingQueue<Transaction> auditQueue, int submissionSize,
                       BatchingStrategy batchingStrategy, AuditPublisher auditPublisher) {
        if (submissionSize <= 0) {
            throw new IllegalArgumentException("submissionSize must be positive but was " + submissionSize);
        }
        this.auditQueue = Objects.requireNonNull(auditQueue, "auditQueue must not be null");
        this.submissionSize = submissionSize;
        this.batchingStrategy = Objects.requireNonNull(batchingStrategy, "batchingStrategy must not be null");
        this.auditPublisher = Objects.requireNonNull(auditPublisher, "auditPublisher must not be null");
    }

    /**
     * Starts the worker thread.
     */
    public synchronized void start() {
        if (workerThread == null) {
            workerThread = new Thread(this::run, THREAD_NAME);
            workerThread.start();
            log.info("Started audit worker with submission size {}", submissionSize);
        }
    }

    /**
     * Stops the worker thread.
     */
    public synchronized void stop() {
        if (workerThread == null) {
            return;
        }
        workerThread.interrupt();
        try {
            workerThread.join(SHUTDOWN_TIMEOUT_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        workerThread = null;
    }

    private void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                publish(collectSubmission());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Audit worker stopped; {} transactions left unsubmitted in the queue", auditQueue.size());
    }

    /**
     * Blocks until exactly {@code submissionSize} transactions have been taken from the queue.
     */
    private List<Transaction> collectSubmission() throws InterruptedException {
        List<Transaction> transactions = new ArrayList<>(submissionSize);
        while (transactions.size() < submissionSize) {
            transactions.add(auditQueue.take());
            auditQueue.drainTo(transactions, submissionSize - transactions.size());
        }
        return transactions;
    }

    private void publish(List<Transaction> transactions) {
        try {
            auditPublisher.publish(new Submission(batchingStrategy.batch(transactions)));
        } catch (RuntimeException e) {
            log.error("Failed to publish audit submission of {} transactions", transactions.size(), e);
        }
    }
}
