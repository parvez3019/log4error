---
title: "Fingers-crossed logging in Spring Boot: keep INFO context for failures, write nothing for successes"
tags: java, springboot, logging, observability
canonical_url: <SET IF CROSS-POSTING>
---

# Fingers-crossed logging in Spring Boot

## The problem: INFO is either too expensive or missing

Log volume gets billed: ingestion, storage, and the time it takes to search. A common fix is to set the root level to WARN in production. That works until the first incident, when you open the logs and find:

```
ERROR Payment failed for order 8812
java.lang.IllegalStateException: card declined (code 51)
```

Which customer was it? Which items, which payment attempt, which cache path? All of that was logged at INFO, and INFO was off.

## The idea: buffer, then decide

Most requests succeed, and nobody reads their INFO lines. The ones you need are the lines from the requests that **failed**. You can't know in advance which requests will fail, but you can **hold** each request's lines in memory and decide at the end:

- request succeeds → throw the buffer away
- request fails → write the buffer, then the error

Other ecosystems call this *fingers-crossed* logging (PHP Monolog's `FingersCrossedHandler`, Python's `logging.handlers.MemoryHandler`). [log4error](https://github.com/parvez3019/log4error) brings it to SLF4J.

## Setup in Spring Boot (2 minutes)

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

The starter registers a servlet filter. For each request, it binds a fresh buffer and puts a request ID in the MDC. Use it from your code:

```java
@GetMapping("/orders/{id}")
public ResponseEntity<String> order(@PathVariable String id) {
    RequestLogger.get().info("Fetching order {}", id);
    RequestLogger.get().info("Order {} has 3 items, total=42.50 EUR", id);
    RequestLogger.get().debug("Cache miss for customer profile, loading from DB");
    RequestLogger.get().info("Calling payment provider for order {}", id);
    try {
        payments.charge(id);
    } catch (IllegalStateException ex) {
        RequestLogger.get().error("Payment failed for order {}", id, ex);
        return ResponseEntity.status(502).build();
    }
    return ResponseEntity.ok("paid");
}
```

Add the request ID to your log pattern:

```yaml
logging:
  pattern:
    console: "%d{HH:mm:ss.SSS} %-5level [%X{x-request-id:-}] %logger{20} - %msg%n"
```

## What you get

A successful request writes **nothing**.

A failed request writes its full trail:

```
10:33:15.376 INFO  [req-9b21] Fetching order fail
10:33:15.376 INFO  [req-9b21] Order fail has 3 items, total=42.50 EUR
10:33:15.376 DEBUG [req-9b21] Cache miss for customer profile, loading from DB
10:33:15.376 INFO  [req-9b21] Calling payment provider for order fail
10:33:15.376 ERROR [req-9b21] Payment failed for order fail
java.lang.IllegalStateException: card declined (code 51)
```

## Is the buffering itself cheap?

JMH results (1,000 INFO lines per operation, Logback writing to `/dev/null`):

| | log4error | plain SLF4J |
|-|-----------|-------------|
| Happy path | ~5.4 µs | ~1.37 ms |
| Error path | ~1.36 ms | ~1.38 ms |

Appending to a list costs a few nanoseconds; writing a log line goes through the appender. On failures you pay roughly the same as you would have anyway.

## Trade-offs (read before you adopt)

- **Thread-bound.** The buffer lives in a ThreadLocal, so `@Async`, `CompletableFuture` and WebFlux hops aren't covered yet.
- **Memory cap.** By default the buffer holds 500 events per request; beyond that the oldest are dropped, with a single WARN.
- **Explicit API.** You call `RequestLogger.get()` instead of your class's SLF4J logger. A native Logback appender is on the roadmap.
- **Things that must always be logged** (audit, security, startup) should use the pass-through methods (`pInfo`, `pWarn`, …) or your normal logger.

## Try it

- Repo + runnable demo: https://github.com/parvez3019/log4error (`make demo`)
- Contributions are welcome: issues labelled `good first issue` and `help wanted` are open, including the Logback appender and async propagation.

If you try it, I'd love to hear in Discussions how much log volume it saved you.
