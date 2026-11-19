package com.example.bankaccount.audit;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Publishes submissions by printing the details of each batch to the console.
 */
public class ConsoleAuditPublisher implements AuditPublisher {

    private static final String NEW_LINE = System.lineSeparator();

    private final PrintStream out;

    /**
     * @param out where to print; {@code System.out} in the application, a captured stream in tests
     */
    public ConsoleAuditPublisher(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out must not be null");
    }

    @Override
    public void publish(Submission submission) {
        out.print(format(submission));
        out.flush();
    }

    private static String format(Submission submission) {
        StringBuilder text = new StringBuilder()
                .append("{").append(NEW_LINE)
                .append("  submission: {").append(NEW_LINE)
                .append("    batches: [").append(NEW_LINE);

        List<Batch> batches = submission.batches();
        for (int i = 0; i < batches.size(); i++) {
            Batch batch = batches.get(i);
            text.append("      {").append(NEW_LINE)
                .append("        totalValueOfAllTransactions: ").append(inPounds(batch.totalValueInPence()))
                .append(NEW_LINE)
                .append("        countOfTransactions: ").append(batch.countOfTransactions()).append(NEW_LINE)
                .append("      }").append(i < batches.size() - 1 ? "," : "").append(NEW_LINE);
        }

        return text.append("    ]").append(NEW_LINE)
                .append("  }").append(NEW_LINE)
                .append("}").append(NEW_LINE)
                .toString();
    }

    private static String inPounds(long pence) {
        return BigDecimal.valueOf(pence, 2).toPlainString();
    }
}
