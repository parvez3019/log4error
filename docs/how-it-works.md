---
description: "How log4error buffers request-scoped INFO/DEBUG logs and flushes them on error: lifecycle, flush semantics, timestamps, memory and threading."
---

# How log4error works

This page describes exactly what log4error does with each call, so you can predict its output and its cost.

## Request lifecycle

```
filter: bind new Logger ──► your code runs ──► filter: remove Logger (finally)
                              │
                              ├─ info()/debug()   → append event to the buffer (no I/O)
                              ├─ pInfo()/pWarn()… → write immediately through SLF4J
                              └─ error()          → write buffered events, write ERROR, clear buffer
```

1. A servlet filter binds a fresh `Logger` to the request thread through a `ThreadLocal`. With the Spring Boot starter this is `Log4ErrorFilter`; without it, the filter you copy from `examples/`.
2. The filter also puts a request ID in the SLF4J **MDC**, taken from the `x-request-id` header or a random UUID.
3. Your code calls `info`/`debug`, which appends an event to an in-memory list.
4. If the request fails and you call `error(...)`, the buffered events are written in order, then the ERROR, and the buffer is cleared.
5. In `finally`, the filter removes the MDC entry and the `Logger`. Anything still buffered is discarded.

## What happens on each call

| Call | Effect |
|------|--------|
| `info(msg, args...)` | Appends an INFO event. Nothing is written yet. |
| `debug(msg, args...)` | Appends a DEBUG event. Nothing is written yet. |
| `error(msg, args...)` | Writes every buffered event through SLF4J, then `slf4jLogger.error(msg, args)`, then clears the buffer. |
| `printInfoLogs()` | Writes every buffered event but **keeps** them. Calling `error()` afterwards writes them a second time. |
| `clearInfoLogStack()` | Discards the buffer without writing. |
| `pInfo` / `pDebug` / `pWarn` / `pError` | Writes immediately. These calls never touch the buffer. |

Notes:

- **Multiple errors in one request.** Each `error()` flushes only what was buffered *since the previous flush*, so context is never repeated.
- **`pError` doesn't flush.** Use `error()` when you want the context, and `pError()` for errors that should stand alone.
- **There is no buffered `warn`.** `pWarn` writes immediately.

## The backend still decides what's printed

Flushed events go through the normal SLF4J logger, so **your backend's level for that logger applies at flush time**:

| Logger used for flushing | Logger name the backend sees |
|--------------------------|------------------------------|
| `new Logger()`, `new Logger(int)`, the starter's `RequestLogger` | `io.github.parvez3019.Logger` |
| `Logger.of(OrderService.class)` | `com.example.OrderService` |
| `Logger.of(someSlf4jLogger)` | that logger's name |

If that logger's effective level is WARN, the flushed INFO/DEBUG lines are dropped and only the ERROR appears. Set that logger to INFO (or DEBUG, if you buffer DEBUG lines) while keeping everything else at WARN:

```xml
<!-- logback.xml / logback-spring.xml -->
<logger name="io.github.parvez3019" level="DEBUG"/>
<root level="WARN">...</root>
```

```xml
<!-- log4j2.xml -->
<Logger name="io.github.parvez3019" level="debug"/>
<Root level="warn">...</Root>
```

This doesn't bring back noise: the logger only writes when a request fails.

## Timestamps and argument formatting

- **Timestamps are flush time.** The backend creates the log event when the buffer is flushed, so every flushed line has (nearly) the same timestamp as the ERROR. Order is preserved. If you need the original times, include them in the message for now.
- **Arguments are formatted at flush time.** `info("cart {}", cart)` stores a reference to `cart` (the argument *array* is copied, but not the objects in it). If `cart` changes before the flush, the flushed line shows the changed state. For mutable objects whose state at the time of the call matters, pass a snapshot such as `cart.toString()` or an immutable value.
- **Throwables are kept.** A trailing `Throwable` on `info`/`debug` is detected the same way SLF4J does it, and its stack trace is printed when the line is flushed.

## Buffer cap and memory

- The default cap is **500 events per request**. Set it with `new Logger(n)` or `log4error.buffer-size` in the starter.
- When the buffer is full, the **oldest** event is dropped, and log4error writes **one** WARN per request (*"log4error buffer cap (500) reached…"*). The WARN is written immediately; it isn't buffered.
- Each event holds the message pattern, a copy of the argument array, and an optional throwable. Memory per request is roughly `events × (pattern + args)`. It's released when the request ends.
- At the cap, dropping the oldest event is O(n) today ([#21](https://github.com/parvez3019/log4error/issues/21)). This only matters for requests that sit at the cap.

## Threading

- The buffer lives in a `ThreadLocal`, and a `Logger` instance is **not thread-safe**. Use it from the request thread only.
- Work handed to `@Async` methods, executors, `CompletableFuture` or reactive operators runs on other threads and won't see the request's buffer ([#20](https://github.com/parvez3019/log4error/issues/20), [#24](https://github.com/parvez3019/log4error/issues/24)).
- `RequestLogger.get()` and `LoggerThreadLocal.getLogger()` **create and bind** a `Logger` if none is bound. On a thread the filter never saw, that logger stays bound until you call `RequestLogger.clear()` or `LoggerThreadLocal.remove()`. See [Background threads](spring-boot.md#background-threads-and-scheduled-jobs).

## Further reading

- [Spring Boot guide](spring-boot.md)
- [Troubleshooting & FAQ](troubleshooting.md)
