package io.github.parvez3019.benchmark;

import io.github.parvez3019.Logger;
import org.apache.logging.log4j.LogManager;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1) // You can adjust the number of forks as needed
@Threads(4) // Number of threads to run in parallel
public class LoggerBenchmark {
    private static final int LOG_COUNT = 1000;
        private Logger logger;
//    private Logger log4jLogger;

    @Setup
    public void setup() {
        logger = new Logger();
//        log4jLogger = LogManager.getLogger(LoggerBenchmark.class);
    }

    @TearDown
    public void teardown() {
        // Cleanup resources if needed
    }

    @Benchmark
    public void customLoggerBenchmark() {
        for (int i = 0; i < LOG_COUNT; i++) {
            logger.info("Logging message " + i);
        }
    }

//    @Benchmark
//    public void log4jLoggerBenchmark() {
//        for (int i = 0; i < LOG_COUNT; i++) {
//            log4jLogger.info("Logging message " + i);
//        }
//    }
}
