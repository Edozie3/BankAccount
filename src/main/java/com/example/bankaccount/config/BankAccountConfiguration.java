package com.example.bankaccount.config;

import com.example.bankaccount.audit.AuditPublisher;
import com.example.bankaccount.audit.AuditWorker;
import com.example.bankaccount.audit.BatchingStrategy;
import com.example.bankaccount.audit.BestFitDecreasingStrategy;
import com.example.bankaccount.audit.ConsoleAuditPublisher;
import com.example.bankaccount.transaction.Transaction;
import com.example.bankaccount.service.BalanceTracker;
import com.example.bankaccount.service.BankAccountService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Init the app components and injects dependencies:
 *
 * <pre>
 *   TransactionProducers ──► BankAccountService (BalanceTracker) ──► auditQueue ◄── AuditWorker
 *                                                                                     │
 *                                                              BatchingStrategy ◄─────┤
 *                                                              AuditPublisher   ◄─────┘
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(AuditProperties.class)
public class BankAccountConfiguration {

    @Bean
    BlockingQueue<Transaction> auditQueue(AuditProperties properties) {
        return new ArrayBlockingQueue<>(properties.queueCapacity());
    }

    @Bean
    BankAccountService bankAccountService(BlockingQueue<Transaction> auditQueue) {
        return new BalanceTracker(auditQueue);
    }

    @Bean
    BatchingStrategy batchingStrategy(AuditProperties properties) {
        return new BestFitDecreasingStrategy(properties.batchLimitInPence());
    }

    @Bean
    AuditPublisher auditPublisher() {
        return new ConsoleAuditPublisher(System.out);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    AuditWorker auditWorker(BlockingQueue<Transaction> auditQueue, AuditProperties properties,
                            BatchingStrategy batchingStrategy, AuditPublisher auditPublisher) {
        return new AuditWorker(auditQueue, properties.submissionSize(), batchingStrategy, auditPublisher);
    }
}
