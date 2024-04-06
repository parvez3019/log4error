package io.github.parvez3019.benchmarking;

import org.openjdk.jmh.annotations.*;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.All)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
@Threads(8)
public class SLF4jBenchmark {
    private static final int LOG_COUNT = 1000;
    private org.slf4j.Logger log4jLogger;

    @Setup
    public void setup() {
        log4jLogger = LoggerFactory.getLogger(org.slf4j.Logger.class);
    }

    @TearDown
    public void teardown() {
    }

    @Benchmark
    public void slf4jLoggerBenchmark() {
        for (int i = 0; i < LOG_COUNT; i++) {
            log4jLogger.info("Logging message " + i);
        }
    }
}
