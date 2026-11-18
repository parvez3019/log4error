<p align="center">
  <img src="docs/assets/logo.svg" alt="log4error" width="360">
</p>

<p align="center">
  <b>Fingers-crossed logging for Java: keep INFO/DEBUG context for failed requests, write nothing for successful ones.</b>
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.github.parvez3019/log4error"><img src="https://img.shields.io/maven-central/v/io.github.parvez3019/log4error?label=Maven%20Central" alt="Maven Central"></a>
  <a href="https://github.com/parvez3019/log4error/actions/workflows/package-verify.yml"><img src="https://github.com/parvez3019/log4error/actions/workflows/package-verify.yml/badge.svg" alt="CI"></a>
  <a href="https://parvez3019.github.io/log4error/"><img src="https://img.shields.io/badge/docs-github.io-blue" alt="Docs"></a>
  <a href="https://javadoc.io/doc/io.github.parvez3019/log4error"><img src="https://javadoc.io/badge2/io.github.parvez3019/log4error/javadoc.svg" alt="javadoc"></a>
  <img src="https://img.shields.io/badge/java-17%2B-blue.svg" alt="Java 17+">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
  <a href="https://github.com/parvez3019/log4error/stargazers"><img src="https://img.shields.io/github/stars/parvez3019/log4error?style=social" alt="Stars"></a>
</p>

<p align="center">
  <a href="https://parvez3019.github.io/log4error/">Docs site</a> ·
  <a href="examples/spring-boot-demo">5-minute demo</a> ·
  <a href="docs/spring-boot.md">Spring Boot guide</a> ·
  <a href="docs/troubleshooting.md">FAQ</a> ·
  <a href="docs/INTEGRATE_AND_MIGRATE_PROMPT.md">AI prompt</a> ·
  <a href="CONTRIBUTING.md">Contributing</a>
</p>

---

Teams turn INFO logging off in production to cut cost and noise. Then a request fails, and all that's left is one ERROR line and a stack trace.

**log4error** buffers each request's INFO/DEBUG lines in memory. **Successful requests write nothing.** When a request fails, the whole trail is written next to the error, tagged with the request ID. It works through **SLF4J** with any backend, and ships a **Spring Boot starter**.

```java
RequestLogger.get().info("Fetching order {}", id);                  // buffered
RequestLogger.get().debug("Cache miss for customer profile");       // buffered
RequestLogger.get().info("Calling payment provider for order {}", id);
...
RequestLogger.get().error("Payment failed for order {}", id, ex);   // flush + error
```

```
10:33:15.376 INFO  [req-9b21] Fetching order 8812
10:33:15.376 DEBUG [req-9b21] Cache miss for customer profile
10:33:15.376 INFO  [req-9b21] Calling payment provider for order 8812
10:33:15.376 ERROR [req-9b21] Payment failed for order 8812
java.lang.IllegalStateException: card declined (code 51)
```

A successful request writes **none** of those lines. A failed one writes all of them.

## Why log4error?

- 🔇 **Quiet happy path:** successful requests write zero buffered lines, so you don't pay ingestion or storage for them.
- 🔎 **Full context on failure:** every INFO/DEBUG line from the failed request, in order, next to the error.
- 🏷️ **Request IDs built in:** read from `x-request-id` (or generated) and put in the MDC for every line.
- ⚡ **About 250× cheaper than writing:** buffering costs ~5 ns per line ([benchmarks](docs/performance.md)).
- 🌱 **Spring Boot starter:** one dependency, auto-configured filter, `log4error.*` properties.
- 🪶 **Tiny core:** a single dependency, `slf4j-api`. Works with Logback, Log4j2 or JUL.
- 🛡️ **Bounded memory:** capped per request (500 events by default); the oldest are dropped first.
- 🤖 **AI-ready:** a [paste-ready prompt](docs/INTEGRATE_AND_MIGRATE_PROMPT.md) for Claude Code, Cursor or Copilot to integrate log4error and migrate your log calls.

See [when to use it and how it compares](docs/comparison.md) with sampling, runtime log levels and APM breadcrumbs.

## Quickstart

**Try it (5 minutes).** You need Java 17+ and Maven.

```bash
git clone https://github.com/parvez3019/log4error.git
cd log4error
make demo
# in another terminal:
curl localhost:8080/orders/1      # succeeds: nothing is logged
curl localhost:8080/orders/fail   # fails: the full trail is logged
```

**Add it to your Spring Boot service:**

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error-spring-boot-starter</artifactId>
    <version>0.2.0</version> <!-- see the Maven Central badge for the latest -->
</dependency>
```

For Gradle, add the starter to the `dependencies` block in `build.gradle`:

```groovy
implementation 'io.github.parvez3019:log4error-spring-boot-starter:0.2.0'
```

| Requirement | Compatibility |
|-------------|---------------|
| Java | 17 or later |
| Spring Boot | 3.x servlet applications (tested with 3.3.5) |
| Logging API | SLF4J 2.x |
| Logging backend | Logback (tested), Log4j2 (untested) |

```yaml
# application.yml
logging:
  level:
    root: WARN                    # your normal production level
    io.github.parvez3019: DEBUG   # required: let flushed lines through
  pattern:
    console: "%d{HH:mm:ss.SSS} %-5level [%X{x-request-id:-}] %logger{20} - %msg%n"
```

Then log through `RequestLogger.get()` and call `.error(...)` where you handle failures. The `logging.level` line is **required**: flushed lines go through your backend's level filter, so without it a failed request prints only the ERROR. The [Spring Boot guide](docs/spring-boot.md) covers every property, error handlers, background threads and testing.

**Not on Spring Boot?** Add `io.github.parvez3019:log4error:0.2.0` and copy the [request filter example](examples/LoggerFilterExample.java). See [How it works](docs/how-it-works.md).

## Documentation

The full docs are published at **[parvez3019.github.io/log4error](https://parvez3019.github.io/log4error/)**, and are also readable here on GitHub:

| Topic | |
|-------|---|
| Spring Boot starter: properties, error handlers, background threads, testing | [docs/spring-boot.md](docs/spring-boot.md) |
| How it works: lifecycle, flush semantics, timestamps, memory, threading | [docs/how-it-works.md](docs/how-it-works.md) |
| Troubleshooting & FAQ | [docs/troubleshooting.md](docs/troubleshooting.md) |
| When to use it, alternatives, prior art | [docs/comparison.md](docs/comparison.md) |
| Benchmarks | [docs/performance.md](docs/performance.md) |
| Upgrading (0.2.0, 0.1.0) and changelog | [docs/upgrade-0.2.0.md](docs/upgrade-0.2.0.md) · [docs/upgrade-0.1.0.md](docs/upgrade-0.1.0.md) · [CHANGELOG.md](CHANGELOG.md) |
| AI prompt for coding agents | [docs/INTEGRATE_AND_MIGRATE_PROMPT.md](docs/INTEGRATE_AND_MIGRATE_PROMPT.md) |

## API at a glance

| Method | Behavior |
|--------|----------|
| `info` / `debug` | Buffer the line (no I/O) |
| `error` | Flush the buffer, write the ERROR, clear the buffer |
| `pInfo` / `pDebug` / `pWarn` / `pError` | Write immediately (not buffered) |
| `printInfoLogs()` / `clearInfoLogStack()` | Flush without clearing / discard |
| `Logger.of(Class)`, `new Logger(int)` | Custom logger name / buffer cap |
| `RequestLogger.get()` *(starter)* | The current request's logger |

**Limits:** the buffer is thread-bound, so it isn't shared with `@Async`, executors or WebFlux ([#20](https://github.com/parvez3019/log4error/issues/20), [#24](https://github.com/parvez3019/log4error/issues/24)). Flushed lines carry the flush timestamp.

## Tech stack

| Category | Technology |
|----------|------------|
| Language | Java 17+ |
| Logging API | SLF4J 2.x (any backend: Logback, Log4j2, JUL) |
| Framework | Spring Boot 3.x starter (servlet stack); plain servlet filter for everything else |
| Build | Maven (multi-module); Gradle-friendly artifacts on Maven Central |
| Tests | JUnit 5, Spring Boot Test, JMH benchmarks |

## Contributing

Issues, ideas and pull requests are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md), pick a [good first issue](https://github.com/parvez3019/log4error/labels/good%20first%20issue), or share how you use it in [Discussions](https://github.com/parvez3019/log4error/discussions).
If log4error saves you log volume or debugging time, a ⭐ helps other teams find it.

## License

[Apache License 2.0](LICENSE)
