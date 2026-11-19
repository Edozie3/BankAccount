package com.example.bankaccount.producer;

import com.example.bankaccount.domain.TransactionType;
import com.example.bankaccount.service.BankAccountService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.SplittableRandom;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * Owns the credit and debit producers.
 */
@Component
@DependsOn("auditWorker")
public class TransactionProducers {

    /** 25 credits per second plus 25 debits per second gives the required 50 per second in total. */
    static final int TRANSACTIONS_PER_SECOND_PER_PRODUCER = 25;

    private final List<TransactionProducer> producers;

    public TransactionProducers(BankAccountService bankAccountService) {
        this.producers = List.of(
                new TransactionProducer(TransactionType.CREDIT, TRANSACTIONS_PER_SECOND_PER_PRODUCER,
                        new TransactionGenerator(new SplittableRandom()), bankAccountService),
                new TransactionProducer(TransactionType.DEBIT, TRANSACTIONS_PER_SECOND_PER_PRODUCER,
                        new TransactionGenerator(new SplittableRandom()), bankAccountService));
    }

    @PostConstruct
    void start() {
        producers.forEach(TransactionProducer::start);
    }

    @PreDestroy
    void stop() {
        producers.forEach(TransactionProducer::stop);
    }
}
