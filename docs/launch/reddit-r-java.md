# r/java post

**Title:** Fingers-crossed logging for Java: buffer INFO per request, write it only when the request fails

**Body:**

Many of us turn INFO off in production to save on log volume. Then something breaks and all we have is:

```
ERROR Payment failed for order 8812
java.lang.IllegalStateException: card declined
```

I built log4error, a small SLF4J-based library that keeps each request's INFO/DEBUG lines in an in-memory buffer:

- request succeeds → buffer discarded, nothing written
- request fails → buffered lines are flushed with the error, tagged with the request ID

```
INFO  [req-9b21] Fetching order 8812
INFO  [req-9b21] Order 8812 has 3 items, total=42.50 EUR
DEBUG [req-9b21] Cache miss for customer profile, loading from DB
INFO  [req-9b21] Calling payment provider for order 8812
ERROR [req-9b21] Payment failed for order 8812
```

The same idea exists elsewhere as Monolog's FingersCrossedHandler and Python's MemoryHandler.

In JMH, buffering a happy-path request's 1,000 log lines is about 250× cheaper than writing them through Logback. On the error path the cost is about the same as normal logging.

New in 0.2.0: a Spring Boot starter. Add the dependency and the request filter is registered for you.

- Repo: https://github.com/parvez3019/log4error
- Write-up: <ARTICLE LINK>

Known limits: the buffer is thread-bound, so `@Async` and WebFlux aren't covered yet. I'd love input on the best way to handle those, and on whether a native Logback appender would be more useful than the wrapper API.

---
r/springboot variant: lead with the starter ("add one dependency") and the `application.yml` snippet.
