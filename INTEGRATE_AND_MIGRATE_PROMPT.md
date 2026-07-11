# AI prompt: integrate & migrate to log4error

Copy this prompt into Cursor, Claude, ChatGPT, or similar. Paste your project context (build file, logging setup, sample classes) with it.

---

You are integrating the Java library `log4error` (`io.github.parvez3019:log4error:0.0.11`)
into this project and migrating existing logging to it.

## Goal

Keep INFO/DEBUG context for failed requests without writing those logs on happy paths.
On error, flush buffered context with the error log.

## Library behavior

- `info()` / `debug()` → buffer in a request-scoped ThreadLocal (no I/O)
- `error()` → print buffered logs, then log ERROR, then clear the buffer
- `pInfo()` / `pDebug()` / `pWarn()` / `pError()` → immediate SLF4J pass-through (no buffering)
- Requires a servlet filter that creates/clears a `Logger` per request
  (see `LoggerFilterExample` / `LoggerThreadLocal`)

## Integration steps

1. Add the Maven/Gradle dependency for `io.github.parvez3019:log4error:0.0.11`.
2. Add a Spring `OncePerRequestFilter` (or equivalent) that:
   - `set(new Logger())` on the request ThreadLocal at start
   - `remove()` in `finally`
   - exposes `public static Logger Logger()` for call sites
3. Ensure SLF4J + a backend (Log4j2/Logback) remain configured; log4error writes through SLF4J.
4. Prefer request/thread boundaries only — do not share one Logger across threads.

## Migration mapping (from SLF4J / Log4j / Logback / java.util.logging)

| Existing call                         | Migrate to                                      | When |
|---------------------------------------|-------------------------------------------------|------|
| `log.info(...)` / `logger.info(...)`  | `Logger().info(...)`                            | Request context useful only if something fails later |
| `log.debug(...)`                      | `Logger().debug(...)`                           | Same as info |
| `log.warn(...)`                       | `Logger().pWarn(...)`                           | Always emit (no buffer API for warn) |
| `log.error(...)`                      | `Logger().error(...)`                           | Failures where prior info/debug context should flush |
| `log.error(...)` (ops / always-on)    | `Logger().pError(...)`                          | Must always write, even with empty buffer |
| Startup / shutdown / non-request logs | Keep existing logger or use `p*` methods        | Outside request scope |
| Static `LoggerFactory.getLogger(X)`   | Keep for class-named loggers if needed; for buffered flow use filter `Logger()` | Avoid mixing buffers incorrectly |

## Rules while migrating

- Do NOT replace every log blindly.
- Buffer (`info`/`debug`) for per-request breadcrumbs.
- Use `p*` for logs that must always appear (security, audit, metrics-adjacent, startup).
- On catch blocks that currently only `log.error`, switch to `Logger().error` so buffered context flushes.
- Preserve `{}` placeholders and throwable-last argument style.
- After migration, happy-path requests should not emit the buffered INFO/DEBUG lines.
- Update imports: remove unused `LoggerFactory` where fully migrated; add static import of `Logger()` from the filter class.
- If the app is not Spring MVC/WebFlux-servlet style, adapt the ThreadLocal lifecycle to the framework’s request/job boundary.
- Leave a short comment at the filter explaining why ThreadLocal must be cleared.

## Deliverables

1. Dependency change
2. Filter (or lifecycle hook) + static accessor
3. Migrated call sites with the mapping above
4. Brief summary of files touched and any logs intentionally left on the old logger
