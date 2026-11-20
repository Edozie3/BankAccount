package com.example.bankaccount.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransactionTest {

    @Test
    void givenPositivePence_whenCreatingCredit_thenAmountIsPositive() {
        // given
        long pence = 25_000;

        // when
        Transaction credit = Transaction.credit("tx-1", pence);

        // then
        assertThat(credit.amountInPence()).isEqualTo(25_000);
        assertThat(credit.isCredit()).isTrue();
        assertThat(credit.isDebit()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -25_000})
    void givenNonPositivePence_whenCreatingCredit_thenRejected(long pence) {
        // given: a non-positive magnitude supplied as a parameter

        // when
        Throwable thrown = catchThrowable(() -> Transaction.credit("tx-1", pence));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenPositivePence_whenCreatingDebit_thenAmountIsNegative() {
        // given
        long pence = 25_000;

        // when
        Transaction debit = Transaction.debit("tx-1", pence);

        // then
        assertThat(debit.amountInPence()).isEqualTo(-25_000);
        assertThat(debit.isDebit()).isTrue();
        assertThat(debit.isCredit()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -25_000})
    void givenNonPositivePence_whenCreatingDebit_thenRejected(long pence) {
        // given: a non-positive magnitude supplied as a parameter

        // when
        Throwable thrown = catchThrowable(() -> Transaction.debit("tx-1", pence));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNullId_whenCreatingTransaction_thenRejected() {
        // given
        String id = null;

        // when
        Throwable thrown = catchThrowable(() -> new Transaction(id, 100));

        // then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void givenBlankId_whenCreatingTransaction_thenRejected(String id) {
        // given: a blank id supplied as a parameter

        // when
        Throwable thrown = catchThrowable(() -> new Transaction(id, 100));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenCreditAndDebitOfSameSize_whenGettingAbsoluteAmount_thenBothArePositive() {
        // given
        Transaction credit = Transaction.credit("c", 50_000_000);
        Transaction debit = Transaction.debit("d", 50_000_000);

        // when
        long creditAbsolute = credit.absoluteAmountInPence();
        long debitAbsolute = debit.absoluteAmountInPence();

        // then
        assertThat(creditAbsolute).isEqualTo(50_000_000);
        assertThat(debitAbsolute).isEqualTo(50_000_000);
    }
}
