# log4error

[![CI](https://github.com/parvez3019/log4error/actions/workflows/package-verify.yml/badge.svg)](https://github.com/parvez3019/log4error/actions/workflows/package-verify.yml)
[![Java Version](https://img.shields.io/badge/java-17-blue.svg)](https://adoptium.net/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.parvez3019/log4error.svg)](https://central.sonatype.com/artifact/io.github.parvez3019/log4error)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Collect INFO and DEBUG logs in memory — and flush them only when an error occurs.

In production, teams often disable INFO logging to cut cost and noise. That leaves you with an ERROR line and a stack trace when something breaks — and none of the request context that would help you debug it.

**log4error** keeps request-scoped INFO/DEBUG logs in a ThreadLocal buffer. Happy paths stay quiet (no I/O). On `error()`, the buffered context is printed with the error, then cleared.

> Deep dive: [Medium article](https://medium.com/@pha3019/log4error-java-library-for-reduced-info-level-logging-5f1c29867fc4)

## Stack

| Component | Details |
|-----------|---------|
| Language  | Java 17 |
| Build     | Maven |
| Logging   | SLF4J (any backend) |
| Integration | ThreadLocal + servlet filter (Spring example provided) |
| License   | Apache 2.0 |

## How it works

1. A servlet filter creates a request-scoped `Logger` and stores it in a `ThreadLocal`.
2. `info()` / `debug()` append messages to an in-memory list — no console or file I/O.
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

**Limits:** the buffer is not propagated to `@Async`, WebFlux, or child threads. Use it on the request thread only.

## Docs

- [Changelog](CHANGELOG.md) — history for each released tag
- [Upgrade to 0.1.0](docs/upgrade-0.1.0.md) — breaking changes and migration steps from 0.0.x
- [Docs index](docs/README.md)

## Installation

**Maven**

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle**

```groovy
implementation 'io.github.parvez3019:log4error:0.1.0'
```

**Gradle (Kotlin DSL)**

```kotlin
implementation("io.github.parvez3019:log4error:0.1.0")
```

The published JAR depends only on `slf4j-api`. Declare Spring Web / servlet yourself if you use the filter example.

[Maven Central](https://central.sonatype.com/artifact/io.github.parvez3019/log4error)

## Quick start

### 1. Register a request filter

Copy [`examples/LoggerFilterExample.java`](examples/LoggerFilterExample.java) into your app (requires `spring-web` + `jakarta.servlet-api`):

```java
requestLogInfoThreadLocal.set(new Logger());
try {
    filterChain.doFilter(request, response);
} finally {
    requestLogInfoThreadLocal.remove(); // prevent ThreadLocal leaks on pooled threads
}
```

### 2. Collect context, flush on error

```java
import static com.example.LoggerFilterExample.Logger;

Logger().info("Processing order {}", orderId);
Logger().debug("Payment attempt {}", attempt);

try {
    paymentService.charge(order);
} catch (Exception ex) {
    Logger().error("Payment failed for order {}", orderId, ex);
}
```

Preserve call-site logger names with `Logger.of(MyService.class)` when constructing the request logger.

### 3. Log immediately when needed

```java
Logger().pInfo("Always visible info");
Logger().pWarn("Always visible warn");
Logger().pDebug("Always visible debug");
Logger().pError("Always visible error");
```

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

Default buffer cap is **500** events; oldest entries are dropped (one WARN) when exceeded. Message formatting uses SLF4J `{}` placeholders; trailing throwables on buffered calls are preserved on flush.

## Migrating from 0.0.11

Full steps: **[Upgrade guide: 0.0.x → 0.1.0](docs/upgrade-0.1.0.md)**.

Highlights:

- Prefer the filter + ThreadLocal path (`@Autowired Logger` / `@RequestScope` is no longer supported on core types).
- Declare Spring/servlet yourself; they are no longer transitive.
- Copy the filter from `examples/` (no longer packaged in the JAR).

## AI prompt: integrate & migrate

Use the ready-to-paste agent prompt in [`docs/INTEGRATE_AND_MIGRATE_PROMPT.md`](docs/INTEGRATE_AND_MIGRATE_PROMPT.md) to add log4error and migrate from SLF4J, Log4j, Logback, or `java.util.logging`.

## Local development

Requires JDK 17+ and Maven.

```bash
make setup    # resolve dependencies
make test     # unit tests
make test-it  # integration tests
make verify   # clean + unit + IT
make package  # build JAR
make benchmark  # JMH (optional)
make help
```

## Performance

Happy-path logging avoids I/O: entries are appended to an in-memory list instead of written out. On error, buffered lines are flushed with the error.

JMH from `make benchmark` (JDK 24, AverageTime, 1 thread, `@Fork(0)`, 2×1s warmup + 5×1s measurement). Each op uses **1,000** INFO messages with `{}` placeholders; SLF4J/Logback writes to `/dev/null`.

### Happy path (no error)

Buffer only, then discard — vs writing every INFO through SLF4J.

| Benchmark | Score (batch of 1,000) | ≈ per log line |
|-----------|------------------------|----------------|
| **log4error** `happyPathBuffer` | **5,431 ± 456 ns/op** | **~5.4 ns** |
| **SLF4J** `infoWrite` | **1,373,980 ± 61,641 ns/op** | **~1.4 µs** |

≈ **250×** cheaper to buffer than to write on the happy path.

### Error path (flush on error)

Buffer 1,000 INFO lines then `error()` (flush buffer + error) — vs writing 1,000 INFO + 1 ERROR through SLF4J.

| Benchmark | Score (batch) | Notes |
|-----------|---------------|--------|
| **log4error** `errorPathFlush` | **1,357,384 ± 133,696 ns/op** | Buffer + flush 1,000 INFO + 1 ERROR |
| **SLF4J** `infoWriteThenError` | **1,379,847 ± 98,605 ns/op** | Write 1,000 INFO + 1 ERROR immediately |

On the error path, cost is in the same ballpark as logging everything up front — you pay for the I/O when it matters. The win is skipping that cost on successful requests.

### Raw JMH output

```
Benchmark                           Mode  Cnt        Score        Error  Units
Log4ErrorBenchmark.errorPathFlush   avgt    5  1357383.947 ± 133696.465  ns/op
Log4ErrorBenchmark.happyPathBuffer  avgt    5     5430.910 ±    455.652  ns/op
SLF4jBenchmark.infoWrite            avgt    5  1373980.078 ±  61640.944  ns/op
SLF4jBenchmark.infoWriteThenError   avgt    5  1379847.224 ±  98604.677  ns/op
```

Re-run locally with `make benchmark`.

> Credit to Christian Hujer for noting that buffering improves the happy path: appending to a list rarely needs a syscall; writing a log line always does.

## Build from source

```bash
make verify
# or
mvn clean verify
```

## License

Apache License 2.0 — see [LICENSE](LICENSE).
