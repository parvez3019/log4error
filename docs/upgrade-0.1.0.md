# Upgrade guide: 0.0.x → 0.1.0

This guide covers upgrading from **log4error 0.0.11** (and earlier 0.0.x tags) to **0.1.0**.

0.1.0 is an intentional breaking release: the library surface is smaller, Spring is no longer bundled, and the supported integration path is **filter + ThreadLocal** only.

## 1. Bump the dependency

**Maven**

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle**

```groovy
implementation 'io.github.parvez3019:log4error:0.1.0'
```

## 2. Breaking changes

| Area | 0.0.x | 0.1.0 | What you must do |
|------|-------|-------|------------------|
| Spring DI | `Logger` / `LoggerThreadLocal` had `@Component` / `@RequestScope` | Annotations removed | Stop `@Autowired` / injecting `Logger`; use filter + ThreadLocal |
| Transitive deps | Spring, Tomcat, javax.servlet, Log4j, etc. came with the JAR | Compile dep is **`slf4j-api` only** | Declare Spring Web / `jakarta.servlet-api` (and your SLF4J backend) yourself |
| Example filter | `io.github.parvez3019.example.LoggerFilterExample` in the JAR | Not packaged; lives in repo [`examples/`](../examples/) | Copy the example into your app (or keep your own filter) |
| `InfoLoggerEvent` | `(level, message, argArray)` | Adds `throwable` component | Recompile if you construct or pattern-match the record |
| Buffer size | Unbounded | Default cap **500**; oldest dropped + one WARN | Raise cap via `new Logger(maxSize)` if you need more breadcrumbs |
| Call-site logger name | Always `io.github.parvez3019.Logger` | Optional `Logger.of(MyClass.class)` | Optional improvement; `new Logger()` still works |

### Still compatible (primary path)

These keep working with small or no call-site changes:

- `new Logger()` / `LoggerThreadLocal` + filter `set` / `remove` in `finally`
- `info` / `debug` / `error` / `pInfo` / `pDebug` / `pWarn` / `pError`
- Class name remains `Logger` (not renamed)

## 3. Required code changes

### A. Replace Spring injection with ThreadLocal

**Before (0.0.x — no longer supported)**

```java
@Autowired
private Logger logger; // @RequestScope bean
```

**After (0.1.0)**

```java
// In a OncePerRequestFilter (or plain Filter):
requestLogInfoThreadLocal.set(new Logger()); // or Logger.of(MyService.class)
try {
    filterChain.doFilter(request, response);
} finally {
    requestLogInfoThreadLocal.remove(); // required on pooled threads
}

// At call sites:
Logger().info("...");
Logger().error("...", ex);
```

Copy [`examples/LoggerFilterExample.java`](../examples/LoggerFilterExample.java) if you need a starting point.

### B. Add dependencies your app actually uses

Example for a Spring MVC app:

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-web</artifactId>
    <version>6.1.x</version>
</dependency>
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <version>6.0.0</version>
    <scope>provided</scope>
</dependency>
<!-- plus your SLF4J binding: logback-classic or log4j-slf4j2-impl -->
```

### C. Fix imports of the old packaged example

```diff
- import io.github.parvez3019.example.LoggerFilterExample;
+ // use your app's filter class copied from examples/
```

### D. Optional: call-site logger identity

```java
// Filter init
requestLogInfoThreadLocal.set(Logger.of(OrderService.class));
```

Flushed lines then appear under `OrderService` instead of `io.github.parvez3019.Logger`.

### E. Optional: larger buffer

```java
requestLogInfoThreadLocal.set(new Logger(2000));
```

## 4. Behavioral changes to expect

1. **Happy path** — buffered `info`/`debug` still produce no I/O until `error()` or explicit `printInfoLogs()`.
2. **Throwables on buffered calls** — `Logger().info("x", ex)` now keeps the stack trace when flushed (bugfix).
3. **Very chatty requests** — after 500 buffered events, oldest are dropped and one WARN is emitted.
4. **ThreadLocal** — still not propagated to `@Async`, WebFlux, or child threads.

## 5. Checklist

- [ ] Dependency version set to `0.1.0`
- [ ] No `@Autowired` / component-scan usage of `io.github.parvez3019.Logger`
- [ ] Filter sets and removes ThreadLocal in `finally`
- [ ] Spring / servlet / SLF4J backend declared in your POM/Gradle
- [ ] Old `io.github.parvez3019.example.*` imports removed
- [ ] Smoke-test: happy path silent; error path prints breadcrumbs then error

## 6. Further reading

- [Changelog](../CHANGELOG.md)
- [README](../README.md)
- [Integrate & migrate prompt](INTEGRATE_AND_MIGRATE_PROMPT.md)
