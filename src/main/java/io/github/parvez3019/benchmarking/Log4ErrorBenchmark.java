package io.github.parvez3019.benchmarking;

import io.github.parvez3019.Logger;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
@Threads(8)
public class Log4ErrorBenchmark {
    private static final int LOG_COUNT = 1000;
    private Logger logger;

    @Setup
    public void setup() {
        logger = new Logger();
    }

    @TearDown
    public void teardown() {
    }

    @Benchmark
    public void customLoggerBenchmark() {
        for (int i = 0; i < LOG_COUNT; i++) {
            logger.info("Logging message " + i);
        }
    }
}
