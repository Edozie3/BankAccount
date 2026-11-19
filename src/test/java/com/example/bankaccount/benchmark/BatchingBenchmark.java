package com.example.bankaccount.benchmark;

import com.example.bankaccount.audit.Batch;
import com.example.bankaccount.audit.BatchingStrategy;
import com.example.bankaccount.audit.BestFitDecreasingStrategy;
import com.example.bankaccount.transaction.Transaction;
import com.example.bankaccount.transaction.TransactionType;
import com.example.bankaccount.producer.TransactionGenerator;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Measures the average time to batch one submission at sizes growing by 10x, from the default 1,000 up to
 * 1,000,000.
 *
 * <p>Run with {@code mvn test-compile exec:exec}.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(3)
public class BatchingBenchmark {

    private static final long BATCH_LIMIT_IN_PENCE = 100_000_000L;   // £1,000,000
    private static final long SEED = 42L;

    @Param({"1000", "10000", "100000", "1000000"})
    private int submissionSize;

    private final BatchingStrategy strategy = new BestFitDecreasingStrategy(BATCH_LIMIT_IN_PENCE);
    private List<Transaction> submission;

    /** Builds the submission once per size, outside the measured time. */
    @Setup(Level.Trial)
    public void createSubmission() {
        TransactionGenerator generator = new TransactionGenerator(new SplittableRandom(SEED));
        submission = IntStream.range(0, submissionSize)
                .mapToObj(i -> generator.generate(i % 2 == 0 ? TransactionType.CREDIT : TransactionType.DEBIT))
                .toList();
    }

    @Benchmark
    public List<Batch> bestFitDecreasing() {
        return strategy.batch(submission);
    }

    public static void main(String[] args) throws RunnerException {
        new Runner(new OptionsBuilder().include(BatchingBenchmark.class.getSimpleName()).build()).run();
    }
}
