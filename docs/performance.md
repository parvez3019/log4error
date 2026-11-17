---
description: "JMH benchmarks: buffering logs with log4error is about 250x cheaper than writing them through SLF4J on the happy path."
---

# Performance

Benchmarks are JMH runs from `make benchmark` (JDK 24, AverageTime, 1 thread, `@Fork(0)`, 2×1s warmup + 5×1s measurement). Each operation logs **1,000** INFO messages with `{}` placeholders; SLF4J/Logback writes to `/dev/null`.

| Path | log4error | Plain SLF4J |
|------|-----------|-------------|
| **Happy path** (no error) | **5,431 ns** per 1,000 lines (~5.4 ns/line) | 1,373,980 ns (~1.4 µs/line) |
| **Error path** (flush 1,000 + error) | 1,357,384 ns | 1,379,847 ns |

- **Happy path:** buffering is about **250× cheaper** than writing each line through SLF4J. Appending to a list rarely needs a syscall; writing a log line always does.
- **Error path:** about the same cost as logging everything up front. You pay for I/O only when a request fails.

The benchmark backend writes to `/dev/null`. With real appenders (files, network shippers, JSON encoding), writing costs more, so the happy-path saving is larger.

## Raw JMH output

```
Benchmark                           Mode  Cnt        Score        Error  Units
Log4ErrorBenchmark.errorPathFlush   avgt    5  1357383.947 ± 133696.465  ns/op
Log4ErrorBenchmark.happyPathBuffer  avgt    5     5430.910 ±    455.652  ns/op
SLF4jBenchmark.infoWrite            avgt    5  1373980.078 ±  61640.944  ns/op
SLF4jBenchmark.infoWriteThenError   avgt    5  1379847.224 ±  98604.677  ns/op
```

Re-run locally with `make benchmark`. The benchmark sources are in [`log4error/src/test/java/io/github/parvez3019/benchmarking`](https://github.com/parvez3019/log4error/blob/main/log4error/src/test/java/io/github/parvez3019/benchmarking).

> Credit to Christian Hujer for noting that buffering improves the happy path.
