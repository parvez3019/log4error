---
title: Fingers-crossed logging for Java and Spring Boot
description: log4error buffers INFO/DEBUG logs per request and writes them only when an error occurs. Full debug context for failed requests, zero log volume for successful ones. SLF4J, Spring Boot starter, Java 17+.
hide:
  - navigation
---

<p align="center">
  <img src="assets/logo.svg#only-light" alt="log4error" width="360">
  <img src="assets/logo-dark.svg#only-dark" alt="log4error" width="360">
</p>

# Fingers-crossed logging for Java

Teams turn INFO logging off in production to cut cost and noise. Then a request fails, and all that's left is one ERROR line and a stack trace.

**log4error** buffers each request's INFO/DEBUG lines in memory. **Successful requests write nothing.** When a request fails, the whole trail is written next to the error, tagged with the request ID. It works through **SLF4J** with any backend (Logback, Log4j2, JUL), and ships a **Spring Boot starter**.

[Get started with Spring Boot](spring-boot.md){ .md-button .md-button--primary }
[How it works](how-it-works.md){ .md-button }
[GitHub](https://github.com/parvez3019/log4error){ .md-button }

## What it looks like

```java
RequestLogger.get().info("Fetching order {}", id);                  // buffered
RequestLogger.get().debug("Cache miss for customer profile");       // buffered
RequestLogger.get().info("Calling payment provider for order {}", id);
...
RequestLogger.get().error("Payment failed for order {}", id, ex);   // flush + error
```

A **successful** request writes none of those lines. A **failed** request writes all of them:

```text
10:33:15.376 INFO  [req-9b21] Fetching order 8812
10:33:15.376 DEBUG [req-9b21] Cache miss for customer profile
10:33:15.376 INFO  [req-9b21] Calling payment provider for order 8812
10:33:15.376 ERROR [req-9b21] Payment failed for order 8812
java.lang.IllegalStateException: card declined (code 51)
```

## Why log4error?

- **Quiet happy path.** Successful requests write zero buffered lines, so you don't pay log ingestion or storage for them.
- **Full context on failure.** Every INFO/DEBUG line from the failed request, in order, next to the error.
- **Request IDs built in.** Read from `x-request-id` (or generated) and put in the MDC.
- **About 250× cheaper than writing.** Buffering costs ~5 ns per line. See [Performance](performance.md).
- **Spring Boot starter.** One dependency, auto-configured filter, `log4error.*` properties.
- **Tiny core.** A single dependency, `slf4j-api`.
- **Bounded memory.** Capped per request (500 events by default); the oldest are dropped first.

## Quick start

=== "Spring Boot (Maven)"

    ```xml
    <dependency>
        <groupId>io.github.parvez3019</groupId>
        <artifactId>log4error-spring-boot-starter</artifactId>
        <version>0.2.0</version>
    </dependency>
    ```

=== "Spring Boot (Gradle)"

    ```groovy
    implementation 'io.github.parvez3019:log4error-spring-boot-starter:0.2.0'
    ```

=== "Core only"

    ```xml
    <dependency>
        <groupId>io.github.parvez3019</groupId>
        <artifactId>log4error</artifactId>
        <version>0.2.0</version>
    </dependency>
    ```

    Then bind a `Logger` per request with a servlet filter. See [How it works](how-it-works.md).

Let flushed lines through your logging backend. This is **required**:

```yaml
logging:
  level:
    root: WARN                    # your normal production level
    io.github.parvez3019: DEBUG   # let flushed lines through
  pattern:
    console: "%d{HH:mm:ss.SSS} %-5level [%X{x-request-id:-}] %logger{20} - %msg%n"
```

!!! warning "Why the logging level matters"
    Flushed lines go through your backend's level filter at flush time. If `io.github.parvez3019` is at WARN, a failed request prints only the ERROR, and the context is silently dropped. See [Troubleshooting](troubleshooting.md#only-the-error-line-is-printed-not-the-buffered-context).

## Try the demo in 5 minutes

You need Java 17+ and Maven.

```bash
git clone https://github.com/parvez3019/log4error.git
cd log4error
make demo
# in another terminal:
curl localhost:8080/orders/1      # succeeds: nothing is logged
curl localhost:8080/orders/fail   # fails: the full trail is logged
```

## Learn more

- [Spring Boot guide](spring-boot.md): properties, error handlers, background threads, testing
- [How it works](how-it-works.md): lifecycle, flush semantics, timestamps, memory, threading
- [When to use it and how it compares](comparison.md): sampling, runtime log levels, APM breadcrumbs, prior art
- [Troubleshooting & FAQ](troubleshooting.md)
