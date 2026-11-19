package com.example.bankaccount.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bankaccount.transaction.Transaction;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsoleAuditPublisherTest {

    @Test
    void givenSubmission_whenPublished_thenPrintsEachBatchTotalInPoundsAndCount() {
        // given
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        ConsoleAuditPublisher publisher =
                new ConsoleAuditPublisher(new PrintStream(captured, true, StandardCharsets.UTF_8));
        Batch first = new Batch(List.of(
                Transaction.credit("c-1", 5_000_000),     // £50,000.00
                Transaction.debit("d-1", 2_500_025)),     // £25,000.25
                7_500_025);
        Batch second = new Batch(List.of(
                Transaction.credit("c-2", 9_800_000)),    // £98,000.00
                9_800_000);
        Submission submission = new Submission(List.of(first, second));

        // when
        publisher.publish(submission);

        // then
        String printed = captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        assertThat(printed).isEqualTo("""
                {
                  submission: {
                    batches: [
                      {
                        totalValueOfAllTransactions: 75000.25
                        countOfTransactions: 2
                      },
                      {
                        totalValueOfAllTransactions: 98000.00
                        countOfTransactions: 1
                      }
                    ]
                  }
                }
                """);
    }
}
