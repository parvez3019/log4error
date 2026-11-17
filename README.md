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

## Before / after

The same failing request, with INFO turned off in production:

**Plain SLF4J**

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

The second block is real output from the [Spring Boot demo](examples/spring-boot-demo). Run it with `make demo`.

## Quick start: Spring Boot

**1. Add the starter.** It registers the request filter automatically.

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

```kotlin
implementation("io.github.parvez3019:log4error-spring-boot-starter:0.2.0")
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

**3. Configure it (optional).**

```yaml
log4error:
  enabled: true               # set false to turn the filter off
  buffer-size: 500            # max buffered events per request; oldest are dropped
  request-id-header: x-request-id
  mdc-key: x-request-id       # use %X{x-request-id} in your log pattern
  filter-order: -2147483638   # Ordered.HIGHEST_PRECEDENCE + 10
```

The starter works with any Spring Boot 3.x servlet app (Spring MVC on Tomcat, Jetty or Undertow). To replace the filter, declare your own `Log4ErrorFilter` bean.

## Quick start: plain servlet or non-Boot apps

Add the core library. It depends only on `slf4j-api`:

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error</artifactId>
    <version>0.2.0</version>
</dependency>
```

```groovy
implementation 'io.github.parvez3019:log4error:0.2.0'
```

Then bind a `Logger` per request. [`examples/LoggerFilterExample.java`](examples/LoggerFilterExample.java) is a copy-paste filter:

```java
requestLogInfoThreadLocal.set(new Logger());
try {
    filterChain.doFilter(request, response);
} finally {
    requestLogInfoThreadLocal.remove(); // prevent ThreadLocal leaks on pooled threads
}
```

To keep call-site logger names in flushed output, build the request logger with `Logger.of(MyService.class)`.

## How it works

1. A servlet filter creates a request-scoped `Logger` and stores it in a `ThreadLocal`.
2. `info()` / `debug()` append messages to an in-memory list. No console or file I/O happens.
3. `error()` prints the buffered logs, logs the error, then clears the buffer.
4. The filter removes the ThreadLocal when the request finishes.

```
Request start
    │
    ├─ info("fetched user {}", id)     → buffered
    ├─ debug("cache miss")             → buffered
    ├─ info("calling payment API")     → buffered
    │
    ├─ happy path  → buffer discarded, nothing written
    └─ error(...)  → flush buffer + error log → clear
```

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

## API

| Method | Behavior |
|--------|----------|
| `info(msg, args...)` | Buffer an INFO message |
| `debug(msg, args...)` | Buffer a DEBUG message |
| `error(msg, args...)` | Flush buffer → log ERROR → clear buffer |
| `pInfo` / `pDebug` / `pWarn` / `pError` | Pass-through to SLF4J (no buffering) |
| `printInfoLogs()` | Flush buffer without clearing |
| `clearInfoLogStack()` | Discard buffered logs |
| `Logger.of(Class)` / `Logger.of(org.slf4j.Logger)` | Factory with call-site logger identity |
| `RequestLogger.get()` *(starter)* | The current request's `Logger` |

The default buffer cap is **500** events. When it's exceeded, the oldest entries are dropped and a single WARN is logged. Messages use SLF4J `{}` placeholders, and a trailing throwable on a buffered call keeps its stack trace when flushed.

## Why not just…?

| Alternative | Trade-off |
|-------------|-----------|
| **Keep INFO on** | You pay ingestion and storage for millions of successful requests nobody reads. |
| **Log sampling** | It keeps a random 1% of requests, so the failing request's context is usually the part that got dropped. |
| **Change log levels at runtime** | It only helps *after* you notice a problem, and the failure has to happen again. |
| **Sentry / APM breadcrumbs** | These work well if you already pay for them, but they live in another tool. log4error keeps the context in the logs you already ship. |

The pattern exists in other ecosystems: PHP Monolog's `FingersCrossedHandler`, Python's `logging.handlers.MemoryHandler`, and .NET's log buffering in `Microsoft.Extensions.Telemetry`. log4error brings it to SLF4J and Spring Boot.

## Limits

The buffer is bound to the request thread. It is **not** propagated to `@Async` methods, WebFlux, or child threads ([help wanted](https://github.com/parvez3019/log4error/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22)).

## Docs

- [Changelog](CHANGELOG.md): history for each released tag
- [Upgrade to 0.1.0](docs/upgrade-0.1.0.md): breaking changes from 0.0.x
- [Spring Boot demo](examples/spring-boot-demo): runnable before/after demo
- [AI prompt: integrate & migrate](docs/INTEGRATE_AND_MIGRATE_PROMPT.md): a ready-to-paste agent prompt for adopting log4error from SLF4J, Log4j, Logback or `java.util.logging`
- [Medium deep dive](https://medium.com/@pha3019/log4error-java-library-for-reduced-info-level-logging-5f1c29867fc4)
- [Docs index](docs/README.md)

## Contributing

Issues, ideas and PRs are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) and the [good first issues](https://github.com/parvez3019/log4error/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22). For questions or to share how you use log4error, head to [Discussions](https://github.com/parvez3019/log4error/discussions).

```bash
make verify     # clean + unit + integration tests (all modules)
make demo       # run the Spring Boot demo on :8080
make benchmark  # JMH (optional)
make help
```

Building requires JDK 17+ and Maven.

[![Star History Chart](https://api.star-history.com/svg?repos=parvez3019/log4error&type=Date)](https://star-history.com/#parvez3019/log4error&Date)

## License

Apache License 2.0. See [LICENSE](LICENSE).
