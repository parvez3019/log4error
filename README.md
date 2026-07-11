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
| Logging   | SLF4J / Log4j 2 |
| Framework | Spring (request-scoped filter) |
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

## Installation

**Maven**

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error</artifactId>
    <version>0.0.11</version>
</dependency>
```

**Gradle**

```groovy
implementation 'io.github.parvez3019:log4error:0.0.11'
```

**Gradle (Kotlin DSL)**

```kotlin
implementation("io.github.parvez3019:log4error:0.0.11")
```

[Maven Central](https://central.sonatype.com/artifact/io.github.parvez3019/log4error)

## Quick start

### 1. Register a request filter

Initialize a `Logger` per request and expose it via a static accessor. See [`LoggerFilterExample`](src/main/java/io/github/parvez3019/example/LoggerFilterExample.java):

```java
@Component
@Order(1)
public class LoggerFilterExample extends OncePerRequestFilter {
    private static final LoggerThreadLocal requestLogInfoThreadLocal = new LoggerThreadLocal();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        requestLogInfoThreadLocal.set(new Logger());
        try {
            filterChain.doFilter(request, response);
        } finally {
            requestLogInfoThreadLocal.remove();
        }
    }

    public static Logger Logger() {
        return requestLogInfoThreadLocal.getLogger();
    }
}
```

### 2. Collect context, flush on error

```java
import static io.github.parvez3019.example.LoggerFilterExample.Logger;

Logger().info("Processing order {}", orderId);
Logger().debug("Payment attempt {}", attempt);

try {
    paymentService.charge(order);
} catch (Exception ex) {
    // Prints buffered info/debug logs, then the error
    Logger().error("Payment failed for order {}", orderId, ex);
}
```

### 3. Log immediately when needed

Use the `p*` methods to write straight to SLF4J without buffering:

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

Message formatting uses SLF4J `{}` placeholders.

## AI prompt: integrate & migrate

Use the ready-to-paste agent prompt in [`INTEGRATE_AND_MIGRATE_PROMPT.md`](INTEGRATE_AND_MIGRATE_PROMPT.md) to add log4error to a project and migrate from SLF4J, Log4j, Logback, or `java.util.logging`.

## Performance

Happy-path logging avoids I/O: entries are appended to an in-memory list instead of written out. That is typically cheaper than a system call per log line.

Rough microbenchmark (10 sets × 10,000 calls):

| Operation | Log4j | log4error |
|-----------|-------|-----------|
| INFO      | ~15 ns | ~38 ns (buffer only) |
| ERROR     | ~15 ns | ~42 ns (flush + error) |

Unhappy paths pay more because buffered context is flushed with the error. Formal profiling is still TBD.

> Credit to Christian Hujer for noting that buffering improves the happy path: appending to a list rarely needs a syscall; writing a log line always does.

## Build from source

```bash
mvn clean install
```

## License

Apache License 2.0 — see [LICENSE](LICENSE).
