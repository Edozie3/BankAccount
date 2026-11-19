package com.example.bankaccount.producer;

import com.example.bankaccount.transaction.TransactionType;
import com.example.bankaccount.service.BankAccountService;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Produces transactions of a single type on its own dedicated thread at a fixed rate and passes each
 * one to the {@link BankAccountService}.
 */
public class TransactionProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionProducer.class);
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final TransactionType type;
    private final long periodInNanos;
    private final TransactionGenerator generator;
    private final BankAccountService bankAccountService;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Creates a producer that runs on its own dedicated thread.
     */
    public TransactionProducer(TransactionType type, int transactionsPerSecond,
                               TransactionGenerator generator, BankAccountService bankAccountService) {
        this(type, transactionsPerSecond, generator, bankAccountService, getScheduler(type));
    }

    /**
     * Creates a producer that uses the supplied scheduler.
     */
    TransactionProducer(TransactionType type, int transactionsPerSecond,
                        TransactionGenerator generator, BankAccountService bankAccountService,
                        ScheduledExecutorService scheduler) {
        if (transactionsPerSecond <= 0) {
            throw new IllegalArgumentException(
                    "transactionsPerSecond must be positive but was " + transactionsPerSecond);
        }
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.periodInNanos = TimeUnit.SECONDS.toNanos(1)/transactionsPerSecond;
        this.generator = Objects.requireNonNull(generator, "generator must not be null");
        this.bankAccountService = Objects.requireNonNull(bankAccountService, "bankAccountService must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
    }

    /**
     * Starts producing transactions.
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::produceOne, 0, periodInNanos, TimeUnit.NANOSECONDS);
            log.info("Started {} producer", type);
        }
    }

    /**
     * Stops producing transactions and releases the producer's thread.
     */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void produceOne() {
        try {
            bankAccountService.processTransaction(generator.generate(type));
        } catch (RuntimeException e) {
            log.error("Failed to produce {} transaction", type, e);
        }
    }

    private static ScheduledExecutorService getScheduler(TransactionType type) {
        String threadName = type.name().toLowerCase(Locale.ROOT) + "-producer";
        return Executors.newSingleThreadScheduledExecutor(task -> new Thread(task, threadName));
    }
}
