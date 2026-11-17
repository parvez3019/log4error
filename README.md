# log4error

**Fingers-crossed logging for Java.** Buffer INFO/DEBUG logs per request in memory, and write them only when an error occurs.

[![CI](https://github.com/parvez3019/log4error/actions/workflows/package-verify.yml/badge.svg)](https://github.com/parvez3019/log4error/actions/workflows/package-verify.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.parvez3019/log4error.svg)](https://central.sonatype.com/artifact/io.github.parvez3019/log4error)
[![javadoc](https://javadoc.io/badge2/io.github.parvez3019/log4error/javadoc.svg)](https://javadoc.io/doc/io.github.parvez3019/log4error)
[![Java Version](https://img.shields.io/badge/java-17%2B-blue.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

In production, teams often turn INFO logging off to cut cost and noise. When something breaks, all that's left is one ERROR line and a stack trace, without the request context you need to debug it.

**log4error** keeps each request's INFO/DEBUG lines in a small in-memory buffer:

- **Successful requests write nothing.** The buffer is discarded, so there's no I/O and no log-ingestion cost.
- **Failed requests write the full trail.** On `error()`, the buffered lines are flushed together with the error, tagged with the request ID.

**Contents:** [Before / after](#before--after) · [Quick start: Spring Boot](#quick-start-spring-boot) · [Quick start: other apps](#quick-start-plain-servlet-and-non-boot-apps) · [API](#api) · [Performance](#performance) · [When to use it](#when-to-use-it) · [Limits](#limits) · [Documentation](#documentation) · [Contributing](#contributing)

## Before / after

The same failing request, with INFO turned off in production.

**Plain SLF4J:**

```
10:33:15.376 ERROR [req-9b21] OrderController - Payment failed for order fail
java.lang.IllegalStateException: card declined (code 51)
```

**log4error.** Successful requests still write nothing; this request failed, so its context was flushed:

```
10:33:15.376 INFO  [req-9b21] i.g.p.Logger - Fetching order fail
10:33:15.376 INFO  [req-9b21] i.g.p.Logger - Order fail has 3 items, total=42.50 EUR
10:33:15.376 DEBUG [req-9b21] i.g.p.Logger - Cache miss for customer profile, loading from DB
10:33:15.376 INFO  [req-9b21] i.g.p.Logger - Calling payment provider for order fail
10:33:15.376 ERROR [req-9b21] i.g.p.Logger - Payment failed for order fail
java.lang.IllegalStateException: card declined (code 51)
```

That's real output from the [Spring Boot demo](examples/spring-boot-demo). Run it with `make demo`.

## Quick start: Spring Boot

**1. Add the starter.** It registers the request filter automatically.

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

```groovy
implementation 'io.github.parvez3019:log4error-spring-boot-starter:0.2.0'      // Gradle
implementation("io.github.parvez3019:log4error-spring-boot-starter:0.2.0")     // Gradle Kotlin DSL
```

**2. Log through `RequestLogger`.**

```java
import io.github.parvez3019.spring.RequestLogger;

RequestLogger.get().info("Processing order {}", orderId);    // buffered
RequestLogger.get().debug("Payment attempt {}", attempt);    // buffered

try {
    paymentService.charge(order);
} catch (Exception ex) {
    RequestLogger.get().error("Payment failed for order {}", orderId, ex); // flush + error
}
```

**3. Let log4error's lines through your logging backend.** ⚠️ **Required.**

The buffered lines are written through SLF4J at flush time, so your backend still applies its level filter to them. Keep your app at WARN if you like, but allow INFO/DEBUG for log4error's logger. Otherwise only the ERROR line is printed and the context is silently dropped.

```yaml
logging:
  level:
    root: WARN                 # your normal production level
    io.github.parvez3019: DEBUG  # let flushed INFO/DEBUG lines through
  pattern:
    console: "%d{HH:mm:ss.SSS} %-5level [%X{x-request-id:-}] %logger{20} - %msg%n"
```

This is safe: log4error's logger only writes when a request fails.

**4. Tune it (optional).** These are the defaults:

```yaml
log4error:
  enabled: true
  buffer-size: 500              # max buffered events per request; oldest are dropped
  request-id-header: x-request-id
  mdc-key: x-request-id
```

See the **[Spring Boot guide](docs/spring-boot.md)** for every property, replacing the filter, background threads, and testing.

## Quick start: plain servlet and non-Boot apps

Add the core library. It has a single dependency, `slf4j-api`:

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error</artifactId>
    <version>0.2.0</version>
</dependency>
```

Bind a `Logger` per request and always remove it. [`examples/LoggerFilterExample.java`](examples/LoggerFilterExample.java) is a copy-paste filter:

```java
requestLogInfoThreadLocal.set(Logger.of(OrderService.class)); // or new Logger()
try {
    filterChain.doFilter(request, response);
} finally {
    requestLogInfoThreadLocal.remove(); // prevents leaks on pooled threads
}
```

The backend rule from step 3 applies here too: allow INFO/DEBUG for the logger you pass to `Logger.of(...)`, or for `io.github.parvez3019` if you use `new Logger()`.

## API

| Method | Behavior |
|--------|----------|
| `info(msg, args...)` / `debug(msg, args...)` | Buffer the message. No I/O happens. |
| `error(msg, args...)` | Flush the buffer, log the ERROR, then clear the buffer |
| `pInfo` / `pDebug` / `pWarn` / `pError` | Write immediately through SLF4J (not buffered). Use for logs that must always appear. |
| `printInfoLogs()` | Flush the buffer without clearing it |
| `clearInfoLogStack()` | Discard the buffer |
| `Logger.of(Class)` / `Logger.of(org.slf4j.Logger)` | Create a logger whose flushed lines carry that logger name |
| `new Logger(int maxBufferSize)` | Create a logger with a custom buffer cap (default 500) |
| `RequestLogger.get()` *(starter)* | The current request's `Logger` |

Messages use SLF4J `{}` placeholders. A trailing `Throwable` on `info`/`debug` keeps its stack trace when flushed. For exact flush semantics, see **[How it works](docs/how-it-works.md)**.

## Performance

Benchmarks are JMH runs from `make benchmark` (JDK 24, AverageTime, 1 thread). Each operation logs **1,000** INFO messages with `{}` placeholders; SLF4J/Logback writes to `/dev/null`.

| Path | log4error | Plain SLF4J |
|------|-----------|-------------|
| **Happy path** (no error) | **5,431 ns** per 1,000 lines (~5.4 ns/line) | 1,373,980 ns (~1.4 µs/line) |
| **Error path** (flush 1,000 + error) | 1,357,384 ns | 1,379,847 ns |

On the happy path, buffering is about **250× cheaper** than writing. On the error path the cost is about the same as logging everything up front: you pay for I/O only when it matters.

<details>
<summary>Raw JMH output</summary>

```
Benchmark                           Mode  Cnt        Score        Error  Units
Log4ErrorBenchmark.errorPathFlush   avgt    5  1357383.947 ± 133696.465  ns/op
Log4ErrorBenchmark.happyPathBuffer  avgt    5     5430.910 ±    455.652  ns/op
SLF4jBenchmark.infoWrite            avgt    5  1373980.078 ±  61640.944  ns/op
SLF4jBenchmark.infoWriteThenError   avgt    5  1379847.224 ±  98604.677  ns/op
```

> Credit to Christian Hujer for noting that buffering improves the happy path: appending to a list rarely needs a syscall, while writing a log line always does.

</details>

## When to use it

**Good fit:** request/response services where most requests succeed, INFO volume is a real cost, and you debug failures from logs.

**Not a fit:** audit or security trails that must always be written (use `pInfo`/your normal logger), long-running batch jobs without a clear per-unit boundary, or fully reactive stacks (not supported yet).

| Alternative | Trade-off |
|-------------|-----------|
| **Keep INFO on** | You pay ingestion and storage for millions of successful requests nobody reads. |
| **Log sampling** | It keeps a random 1% of requests, so the failing request's context is usually the part that got dropped. |
| **Change log levels at runtime** | It only helps *after* you notice a problem, and the failure has to happen again. |
| **Sentry / APM breadcrumbs** | These work well if you already pay for them, but they live in another tool. log4error keeps the context in the logs you already ship. |

The pattern exists in other ecosystems: PHP Monolog's `FingersCrossedHandler`, Python's `logging.handlers.MemoryHandler`, and .NET's log buffering in `Microsoft.Extensions.Telemetry`. log4error brings it to SLF4J and Spring Boot.

## Limits

- **Thread-bound.** The buffer isn't propagated to `@Async`, executors, WebFlux, or child threads ([#20](https://github.com/parvez3019/log4error/issues/20), [#24](https://github.com/parvez3019/log4error/issues/24)).
- **Flush-time timestamps.** Flushed lines carry the time of the flush, not the time of the original call ([details](docs/how-it-works.md#timestamps-and-argument-formatting)).
- **One logger name per request with the starter.** Flushed lines appear under `io.github.parvez3019.Logger` ([#22](https://github.com/parvez3019/log4error/issues/22)).

## Documentation

| Guide | What's in it |
|-------|--------------|
| [Spring Boot guide](docs/spring-boot.md) | Starter properties, replacing the filter, background threads, testing |
| [How it works](docs/how-it-works.md) | Request lifecycle, flush semantics, buffer cap, memory, threading |
| [Troubleshooting & FAQ](docs/troubleshooting.md) | "Nothing printed on error", missing request IDs, async logs, and more |
| [Upgrade to 0.2.0](docs/upgrade-0.2.0.md) | Switching from a copied filter to the starter |
| [Upgrade to 0.1.0](docs/upgrade-0.1.0.md) | Breaking changes from 0.0.x |
| [AI prompt: integrate & migrate](docs/INTEGRATE_AND_MIGRATE_PROMPT.md) | A paste-ready agent prompt for adopting log4error |
| [Changelog](CHANGELOG.md) | Release history |
| [Medium deep dive](https://medium.com/@pha3019/log4error-java-library-for-reduced-info-level-logging-5f1c29867fc4) | Background and motivation |

## Contributing

Issues, ideas and PRs are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) and the [good first issues](https://github.com/parvez3019/log4error/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22). For questions, or to share how you use log4error, go to [Discussions](https://github.com/parvez3019/log4error/discussions).

```bash
make verify     # clean + unit + integration tests (all modules)
make demo       # run the Spring Boot demo on :8080
make benchmark  # JMH (optional)
```

Building requires JDK 17+ and Maven.

[![Star History Chart](https://api.star-history.com/svg?repos=parvez3019/log4error&type=Date)](https://star-history.com/#parvez3019/log4error&Date)

## License

Apache License 2.0. See [LICENSE](LICENSE).
