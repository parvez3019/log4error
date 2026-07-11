package io.github.parvez3019.benchmarking;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Entry point for local JMH runs: {@code make benchmark}.
 */
public class Main {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Log4ErrorBenchmark.class.getSimpleName())
                .include(SLF4jBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
