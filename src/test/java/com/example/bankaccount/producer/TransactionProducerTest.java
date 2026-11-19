package com.example.bankaccount.producer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankaccount.domain.Transaction;
import com.example.bankaccount.domain.TransactionType;
import com.example.bankaccount.service.BankAccountService;
import java.time.Duration;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

class TransactionProducerTest {

    private static final int TRANSACTIONS_PER_SECOND = 25;
    private static final Duration MAX_WAIT = Duration.ofSeconds(5);

    private TransactionProducer startedProducer;

    @AfterEach
    void stopProducer() {
        if (startedProducer != null) {
            startedProducer.stop();
        }
    }

    @Test
    void givenRunningProducer_whenTransactionGenerated_thenItIsPassedToProcessTransaction() {
        // given
        Transaction generated = Transaction.credit("tx-1", 25_000);
        TransactionGenerator generator = mock(TransactionGenerator.class);
        when(generator.generate(TransactionType.CREDIT)).thenReturn(generated);
        BankAccountService bankAccountService = mock(BankAccountService.class);
        TransactionProducer producer = new TransactionProducer(
                TransactionType.CREDIT, TRANSACTIONS_PER_SECOND, generator, bankAccountService);

        // when
        start(producer);

        // then
        verify(bankAccountService, timeout(MAX_WAIT.toMillis()).atLeast(3)).processTransaction(generated);
    }

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void givenProducerOfOneType_whenRunning_thenOnlyThatTypeIsProduced(TransactionType type) {
        // given
        RecordingBankAccountService bankAccountService = new RecordingBankAccountService();
        TransactionProducer producer = new TransactionProducer(
                type, TRANSACTIONS_PER_SECOND, new TransactionGenerator(new Random(42)), bankAccountService);

        // when
        start(producer);
        await().atMost(MAX_WAIT).until(() -> bankAccountService.count() >= 10);
        producer.stop();

        // then
        List<Transaction> produced = bankAccountService.transactions();
        if (type == TransactionType.CREDIT) {
            assertThat(produced).allSatisfy(tx -> assertThat(tx.isCredit()).isTrue());
        } else {
            assertThat(produced).allSatisfy(tx -> assertThat(tx.isDebit()).isTrue());
        }
    }

    // #12
    @Test
    void givenRateOf25PerSecond_whenStarted_thenScheduledEvery40Milliseconds() {
        // given
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        TransactionProducer producer = new TransactionProducer(TransactionType.CREDIT, TRANSACTIONS_PER_SECOND,
                mock(TransactionGenerator.class), mock(BankAccountService.class), scheduler);

        // when
        start(producer);

        // then
        ArgumentCaptor<Long> period = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> unit = ArgumentCaptor.forClass(TimeUnit.class);
        verify(scheduler).scheduleAtFixedRate(any(Runnable.class), anyLong(), period.capture(), unit.capture());
        assertThat(unit.getValue().toNanos(period.getValue())).isEqualTo(TimeUnit.MILLISECONDS.toNanos(40));
    }

    private void start(TransactionProducer producer) {
        startedProducer = producer;
        producer.start();
    }

    /**
     * Test double that records every transaction it receives. Thread-safe because producers call it from
     * their own threads.
     */
    private static final class RecordingBankAccountService implements BankAccountService {

        private final Queue<Transaction> received = new ConcurrentLinkedQueue<>();

        @Override
        public void processTransaction(Transaction transaction) {
            received.add(transaction);
        }

        @Override
        public double retrieveBalance() {
            return 0;
        }

        int count() {
            return received.size();
        }

        List<Transaction> transactions() {
            return List.copyOf(received);
        }
    }
}
