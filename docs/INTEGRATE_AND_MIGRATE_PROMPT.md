# AI prompt: integrate & migrate to log4error

Copy this prompt into Cursor, Claude, ChatGPT, or similar. Paste your project context (build file, logging setup, sample classes) with it.

---

You are integrating the Java library `log4error` (`io.github.parvez3019:log4error:0.1.0`)
into this project and migrating existing logging to it.

## Goal

Keep INFO/DEBUG context for failed requests without writing those logs on happy paths.
On error, flush buffered context with the error log.

## Library behavior

- `info()` / `debug()` → buffer in a request-scoped ThreadLocal (no I/O)
- `error()` → print buffered logs, then log ERROR, then clear the buffer
- `pInfo()` / `pDebug()` / `pWarn()` / `pError()` → immediate SLF4J pass-through (no buffering)
- Requires a servlet filter that creates/clears a `Logger` per request
  (see `examples/LoggerFilterExample.java` / `LoggerThreadLocal`)
- Default buffer cap is 500; oldest events drop with a one-time WARN
- Optional: `Logger.of(MyService.class)` to preserve call-site logger names
- Core JAR depends only on `slf4j-api` (no Spring on the classpath unless you add it)

## Integration steps

1. Add the Maven/Gradle dependency for `io.github.parvez3019:log4error:0.1.0`.
2. Add a Spring `OncePerRequestFilter` (or plain `Filter`) that:
   - `set(new Logger())` or `set(Logger.of(SomeClass.class))` on the request ThreadLocal at start
   - `remove()` in `finally` (prevents leaks on pooled threads)
   - exposes `public static Logger Logger()` for call sites
3. Copy from `examples/LoggerFilterExample.java` in the log4error repo if useful.
4. Ensure SLF4J + a backend (Log4j2/Logback) remain configured; log4error writes through SLF4J.
5. Prefer request/thread boundaries only — do not share one Logger across threads.
6. Do not use Spring `@Autowired` / `@RequestScope` injection of `io.github.parvez3019.Logger`
   (annotations were removed in 0.1.0; ThreadLocal + filter is the supported path).

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

## Migrating from log4error 0.0.11 → 0.1.0

1. Bump dependency version to `0.1.0`.
2. If you used `@Autowired` / component-scanned `Logger`, switch to filter + ThreadLocal.
3. Declare Spring Web / servlet yourself (no longer transitive).
4. Copy filter from repo `examples/` if you imported the old packaged example class.
5. Expect buffer capping on very chatty requests (default 500).

## Rules while migrating

- Do NOT replace every log blindly.
- Buffer (`info`/`debug`) for per-request breadcrumbs.
- Use `p*` for logs that must always appear (security, audit, metrics-adjacent, startup).
- On catch blocks that currently only `log.error`, switch to `Logger().error` so buffered context flushes.
- Preserve `{}` placeholders and throwable-last argument style.
- After migration, happy-path requests should not emit the buffered INFO/DEBUG lines.
- Update imports: remove unused `LoggerFactory` where fully migrated; add static import of `Logger()` from the filter class.
- If the app is not Spring MVC/servlet style, adapt the ThreadLocal lifecycle to the framework’s request/job boundary.
- Leave a short comment at the filter explaining why ThreadLocal must be cleared.

## Deliverables

1. Dependency change
2. Filter (or lifecycle hook) + static accessor
3. Migrated call sites with the mapping above
4. Brief summary of files touched and any logs intentionally left on the old logger
