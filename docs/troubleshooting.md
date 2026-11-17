---
description: "Fix common log4error issues: only the ERROR is printed, missing request IDs, buffer-cap warnings, backend and WebFlux support."
---

# Troubleshooting & FAQ

## Only the ERROR line is printed, not the buffered context

The most common cause is the **logging level**. Flushed lines go through SLF4J at flush time, and your backend drops them if the flushing logger is set to WARN or higher. Allow INFO/DEBUG for it:

```yaml
logging.level.io.github.parvez3019: DEBUG   # new Logger() / starter
logging.level.com.example.OrderService: DEBUG   # if you use Logger.of(OrderService.class)
```

Other causes:
- **You called `pError` instead of `error`.** `pError` writes the ERROR without flushing.
- **The `info` calls ran on another thread** (`@Async`, an executor), which has its own buffer. See [Threading](how-it-works.md#threading).

## Successful requests are writing INFO lines

- You're using `pInfo` or a normal `LoggerFactory` logger for those lines. Only `info`/`debug` are buffered.
- Something calls `printInfoLogs()`. It writes the buffer without an error.

## The request ID is missing from log lines

- Add `%X{x-request-id}` (or your `log4error.mdc-key`) to the log pattern.
- Lines logged on other threads don't share the request's MDC.
- Lines logged outside the filter's scope (by an earlier filter, or after the response completes) have no request ID.

## All flushed lines have the same timestamp

This is expected: the backend timestamps lines when they're flushed. See [Timestamps](how-it-works.md#timestamps-and-argument-formatting).

## A flushed line shows a value that changed later

Arguments are formatted at flush time. Pass a snapshot (`obj.toString()` or an immutable value) when the state at call time matters.

## "log4error buffer cap (500) reached" warning

A single request buffered more than `buffer-size` events, so the oldest were dropped. Either raise `log4error.buffer-size` (or use `new Logger(n)`), or move high-volume loops to DEBUG summaries.

## Flushed lines appear under `io.github.parvez3019.Logger`, not my class

With the starter (or `new Logger()`), every flushed line uses that logger name. Without the starter, bind `Logger.of(MyClass.class)` instead. Per-call-site names with the starter are tracked in [#22](https://github.com/parvez3019/log4error/issues/22).

## Does it work with Log4j2 / java.util.logging / Logback?

Yes. log4error only talks to the SLF4J API, so any SLF4J 2.x backend works, including Logback, Log4j2 via `log4j-slf4j2-impl`, and JUL via `slf4j-jdk14`. The project's tests use Logback. Native appender integrations are planned ([#18](https://github.com/parvez3019/log4error/issues/18), [#19](https://github.com/parvez3019/log4error/issues/19)).

## Does it work with WebFlux, Kotlin coroutines or virtual threads?

- **WebFlux / coroutines:** not yet. The buffer is a ThreadLocal, and those stacks hop between threads ([#24](https://github.com/parvez3019/log4error/issues/24)).
- **Virtual threads** (servlet stack, `spring.threads.virtual.enabled=true`): each request still runs on one (virtual) thread, so the ThreadLocal model applies. This hasn't been covered by tests yet.

## How much memory does it use?

Roughly the number of buffered events × (message pattern + argument references) per *in-flight* request, capped by `buffer-size`. Memory is released when the request ends. See [Buffer cap and memory](how-it-works.md#buffer-cap-and-memory).

## Still stuck?

Ask in [Discussions → Q&A](https://github.com/parvez3019/log4error/discussions/categories/q-a), or open a [bug report](https://github.com/parvez3019/log4error/issues/new/choose) with your versions and logging config.
