package io.github.parvez3019.benchmarking;

import org.apache.logging.log4j.LogManager;
import org.openjdk.jmh.annotations.*;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
@Threads(8)
public class Log4jBenchmark {
    private static final int LOG_COUNT = 1000;
    private Logger log4jLogger;

    @Setup
    public void setup() {
        log4jLogger = LogManager.getLogger(Log4jBenchmark.class);
    }

    @TearDown
    public void teardown() {
    }

    @Benchmark
    public void log4jLoggerBenchmark() {
        for (int i = 0; i < LOG_COUNT; i++) {
            log4jLogger.info("Logging message " + i);
        }
    }
}
