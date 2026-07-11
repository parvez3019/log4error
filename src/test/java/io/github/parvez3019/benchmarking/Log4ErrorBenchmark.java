package io.github.parvez3019.benchmarking;

import io.github.parvez3019.Logger;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Happy path: buffer INFO only (no I/O), then discard.
 * Error path: buffer INFO, then {@code error()} which flushes buffer + writes the error.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(0)
@Threads(1)
public class Log4ErrorBenchmark {
    private static final int LOG_COUNT = 1000;
    private Logger logger;

    @Setup
    public void setup() {
        logger = new Logger(LOG_COUNT);
    }

    @TearDown
    public void teardown() {
        logger.clearInfoLogStack();
    }

    /**
     * Happy path: collect breadcrumbs in memory, discard without writing.
     */
    @Benchmark
    public void happyPathBuffer() {
        for (int i = 0; i < LOG_COUNT; i++) {
            logger.info("Logging message {}", i);
        }
        logger.clearInfoLogStack();
    }

    /**
     * Error path: collect breadcrumbs, then flush them with an error line.
     */
    @Benchmark
    public void errorPathFlush() {
        for (int i = 0; i < LOG_COUNT; i++) {
            logger.info("Logging message {}", i);
        }
        logger.error("request failed {}", "benchmark");
    }
}
