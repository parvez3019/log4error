---
description: "Upgrade log4error to 0.2.0: no breaking changes, and how to switch a copied servlet filter to the Spring Boot starter."
---

# Upgrade guide: 0.1.x → 0.2.0

0.2.0 has **no breaking changes**. The core artifact `io.github.parvez3019:log4error` keeps its coordinates and API. The new piece is an optional Spring Boot starter.

## Option A: core only (nothing else changes)

Bump the version:

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error</artifactId>
    <version>0.2.0</version>
</dependency>
```

Your existing filter and `Logger()` call sites keep working.

## Option B: switch a Spring Boot app to the starter

If you copied `LoggerFilterExample` into a Spring Boot 3.x app, the starter can replace it.

### 1. Swap the dependency

```diff
 <dependency>
     <groupId>io.github.parvez3019</groupId>
-    <artifactId>log4error</artifactId>
-    <version>0.1.0</version>
+    <artifactId>log4error-spring-boot-starter</artifactId>
+    <version>0.2.0</version>
 </dependency>
```

### 2. Delete your copied filter

Remove your `LoggerFilterExample` (or similar) class and its `@Component` / `FilterRegistrationBean`. **Don't keep both**: two filters would each bind their own `Logger`, and call sites would get whichever they reference.

If your filter used a custom request-ID header or buffer size, move those settings to `application.yml`:

```yaml
log4error:
  request-id-header: x-correlation-id
  mdc-key: x-correlation-id
  buffer-size: 1000
```

### 3. Update call sites

```diff
- import static com.example.LoggerFilterExample.Logger;
+ import io.github.parvez3019.spring.RequestLogger;

- Logger().info("Processing order {}", orderId);
+ RequestLogger.get().info("Processing order {}", orderId);
```

A project-wide find/replace of `Logger()` → `RequestLogger.get()` usually covers it. To keep the diff small, you can keep a one-line helper instead:

```java
public static io.github.parvez3019.Logger Logger() { return RequestLogger.get(); }
```

### 4. Check logging levels

Make sure `io.github.parvez3019` is allowed at INFO/DEBUG (see [Spring Boot guide → Required: logging levels](spring-boot.md#required-logging-levels)).

If your filter used `Logger.of(MyClass.class)`, flushed lines will now appear under `io.github.parvez3019.Logger` instead ([#22](https://github.com/parvez3019/log4error/issues/22)). Update any log-pipeline filters or alerts that matched on the old logger name.

### 5. Smoke-test

- A successful request writes no buffered INFO/DEBUG.
- A failing request writes the buffered lines plus the ERROR, with the request ID.

## Checklist

- [ ] Dependency bumped (core) or swapped (starter)
- [ ] Copied filter removed (starter only)
- [ ] Call sites use `RequestLogger.get()` (starter only)
- [ ] `io.github.parvez3019` (or your `Logger.of` class) allowed at INFO/DEBUG
- [ ] Smoke-tested the happy and error paths
