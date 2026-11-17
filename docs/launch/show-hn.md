# Show HN

**Title (≤ 80 chars):** Show HN: log4error – write a request's INFO logs only if the request fails

**URL:** https://github.com/parvez3019/log4error

**First comment (post right after submitting):**

> Hi HN. Most teams I've worked with turn INFO logging off in production because of cost, then regret it the first time they debug an incident with only an ERROR line to go on.
>
> log4error buffers each request's INFO/DEBUG lines in a ThreadLocal list. If the request succeeds, the list is dropped. If `error()` is called, the buffered lines are written with the error, tagged with the request ID. It's the "fingers-crossed" pattern (Monolog, Python's MemoryHandler) for SLF4J, and it ships a Spring Boot starter.
>
> Numbers: buffering 1,000 lines costs ~5 µs vs ~1.4 ms to write them through Logback, so a successful request costs almost nothing.
> <IF YOU HAVE IT: "At <team/company> this cut log ingestion by N% with no loss of error context.">
>
> Trade-offs I'd like feedback on:
> - It's thread-bound, so async hops need explicit propagation (not done yet).
> - It's a wrapper API today. A Logback appender would let people keep `LoggerFactory.getLogger` call sites.
> - Memory: the buffer is capped (500 events by default, oldest dropped).

Timing: weekday, ~8–10am US Eastern. Don't ask for upvotes.
