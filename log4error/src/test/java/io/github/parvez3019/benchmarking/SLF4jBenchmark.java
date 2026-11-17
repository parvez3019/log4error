package io.github.parvez3019.benchmarking;

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
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Baseline: write every INFO through SLF4J (Logback → /dev/null in tests).
 * Also measures INFO batch + one ERROR for comparison with log4error error-path flush.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(0)
@Threads(1)
public class SLF4jBenchmark {
    private static final int LOG_COUNT = 1000;
    private org.slf4j.Logger slf4jLogger;

    @Setup
    public void setup() {
        slf4jLogger = LoggerFactory.getLogger(SLF4jBenchmark.class);
    }

    @TearDown
    public void teardown() {
    }

    @Benchmark
    public void infoWrite() {
        for (int i = 0; i < LOG_COUNT; i++) {
            slf4jLogger.info("Logging message {}", i);
        }
    }

    @Benchmark
    public void infoWriteThenError() {
        for (int i = 0; i < LOG_COUNT; i++) {
            slf4jLogger.info("Logging message {}", i);
        }
        slf4jLogger.error("request failed {}", "benchmark");
    }
}
