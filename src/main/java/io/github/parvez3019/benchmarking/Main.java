package io.github.parvez3019.benchmarking;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collection;

public class Main {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(
                Log4ErrorBenchmark.class.getSimpleName()
        ).build();
        new Runner(opt).run();
    }
}
