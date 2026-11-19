package com.example.bankaccount.producer;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bankaccount.transaction.Transaction;
import com.example.bankaccount.transaction.TransactionType;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TransactionGeneratorTest {

    private static final long MIN_AMOUNT_IN_PENCE = 20_000;      // £200
    private static final long MAX_AMOUNT_IN_PENCE = 50_000_000;  // £500,000
    private static final int SAMPLE_SIZE = 10_000;

    private final TransactionGenerator generator = new TransactionGenerator(new Random(42));

    @Test
    void givenCreditType_whenGenerating_thenAmountIsPositive() {
        // given
        TransactionType type = TransactionType.CREDIT;

        // when
        List<Transaction> transactions = generate(type, SAMPLE_SIZE);

        // then
        assertThat(transactions).allSatisfy(tx -> assertThat(tx.amountInPence()).isPositive());
    }

    @Test
    void givenDebitType_whenGenerating_thenAmountIsNegative() {
        // given
        TransactionType type = TransactionType.DEBIT;

        // when
        List<Transaction> transactions = generate(type, SAMPLE_SIZE);

        // then
        assertThat(transactions).allSatisfy(tx -> assertThat(tx.amountInPence()).isNegative());
    }

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void givenManyGenerations_whenCheckingAmounts_thenAllBetween200And500000Pounds(TransactionType type) {
        // given: a type supplied as a parameter

        // when
        List<Transaction> transactions = generate(type, SAMPLE_SIZE);

        // then
        assertThat(transactions).allSatisfy(tx ->
                assertThat(tx.absoluteAmountInPence()).isBetween(MIN_AMOUNT_IN_PENCE, MAX_AMOUNT_IN_PENCE));
    }

    @ParameterizedTest
    @EnumSource(TransactionType.class)
    void givenManyGenerations_whenCheckingIds_thenEachHasAGeneratedNonBlankId(TransactionType type) {
        // given: a type supplied as a parameter

        // when
        List<String> ids = generate(type, SAMPLE_SIZE).stream().map(Transaction::id).toList();

        // then
        assertThat(ids).allSatisfy(id -> assertThat(id).isNotBlank());
        assertThat(ids).doesNotHaveDuplicates();
    }

    private List<Transaction> generate(TransactionType type, int count) {
        return IntStream.range(0, count).mapToObj(i -> generator.generate(type)).toList();
    }
}
