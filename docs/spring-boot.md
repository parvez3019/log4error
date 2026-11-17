---
description: "Set up log4error in Spring Boot 3: starter properties, required logging levels, error handlers, background threads and testing."
---

# Spring Boot guide

`log4error-spring-boot-starter` wires log4error into a Spring Boot 3.x **servlet** application (Spring MVC on Tomcat, Jetty or Undertow). Reactive (WebFlux) applications aren't supported yet ([#24](https://github.com/parvez3019/log4error/issues/24)).

## Install

```xml
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error-spring-boot-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

```groovy
implementation 'io.github.parvez3019:log4error-spring-boot-starter:0.2.0'
```

The starter brings in the core `log4error` library and `spring-boot-autoconfigure`. It expects `spring-web` and the servlet API from your application, which `spring-boot-starter-web` already provides.

## What the starter registers

| Bean | Type | Purpose |
|------|------|---------|
| `log4ErrorFilter` | `Log4ErrorFilter` | Binds a fresh `Logger` per request, puts the request ID in the MDC, and cleans both up in `finally` |
| `log4ErrorFilterRegistration` | `FilterRegistrationBean<Log4ErrorFilter>` | Registers the filter with order `log4error.filter-order` |

Auto-configuration applies only when all of these hold:
- the app is a servlet web application
- `OncePerRequestFilter` is on the classpath
- `log4error.enabled` is not `false`

## Configuration reference

| Property | Default | Description |
|----------|---------|-------------|
| `log4error.enabled` | `true` | Set `false` to skip registering the filter |
| `log4error.buffer-size` | `500` | Max buffered events per request. Beyond it, the oldest are dropped with one WARN. |
| `log4error.request-id-header` | `x-request-id` | Header to read the request ID from. A UUID is generated when it's missing or blank. |
| `log4error.mdc-key` | `x-request-id` | MDC key for the request ID. Reference it in your pattern as `%X{x-request-id}`. |
| `log4error.filter-order` | `Ordered.HIGHEST_PRECEDENCE + 10` | Filter order. Runs before Spring Security (`-100`) by default, so the buffer covers the whole chain. |

Your IDE autocompletes these properties: the starter ships configuration metadata.

## Required: logging levels

Flushed lines are written through SLF4J under `io.github.parvez3019.Logger`, and your backend filters them at flush time. Allow INFO/DEBUG for that logger, or failed requests print only the ERROR:

```yaml
logging:
  level:
    root: WARN
    io.github.parvez3019: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} %-5level [%X{x-request-id:-}] %logger{20} - %msg%n"
```

For JSON logging (e.g. logstash-logback-encoder), the MDC request ID is included automatically as a field.

## Using it

```java
import io.github.parvez3019.spring.RequestLogger;

@Service
class PaymentService {
    void charge(Order order) {
        RequestLogger.get().info("Charging order {} amount={}", order.id(), order.total());
        try {
            gateway.charge(order);
        } catch (GatewayException ex) {
            RequestLogger.get().error("Charge failed for order {}", order.id(), ex);
            throw ex;
        }
    }
}
```

Guidelines:

- Use `RequestLogger.get().info/debug` for breadcrumbs that only matter if the request fails.
- Use `RequestLogger.get().error(...)` where you handle a failure. A central `@ControllerAdvice` is a good place, so every unhandled exception flushes its context:

  ```java
  @RestControllerAdvice
  class ErrorHandler {
      @ExceptionHandler(Exception.class)
      ResponseEntity<Void> handle(Exception ex) {
          RequestLogger.get().error("Request failed", ex);
          return ResponseEntity.internalServerError().build();
      }
  }
  ```

- Keep your normal `LoggerFactory.getLogger(...)` loggers for startup, audit and security logs that must always appear, or use `RequestLogger.get().pInfo(...)`.

## Replacing or disabling the filter

- **Disable:** set `log4error.enabled=false`. `RequestLogger.get()` keeps working, but nothing binds or clears a logger per request.
- **Replace:** declare your own `Log4ErrorFilter` bean. The starter backs off and registers yours.
- **Change registration** (URL patterns, dispatcher types): declare a bean named `log4ErrorFilterRegistration`.

  ```java
  @Bean
  FilterRegistrationBean<Log4ErrorFilter> log4ErrorFilterRegistration(Log4ErrorFilter filter) {
      FilterRegistrationBean<Log4ErrorFilter> reg = new FilterRegistrationBean<>(filter);
      reg.addUrlPatterns("/api/*");
      return reg;
  }
  ```

## Background threads and scheduled jobs

The filter runs only for HTTP requests. On any other thread (`@Scheduled`, `@Async`, message listeners), `RequestLogger.get()` creates a `Logger` and binds it to that thread. On pooled threads, give the work its own boundary so buffers don't build up across tasks:

```java
@Scheduled(fixedDelay = 60_000)
void syncInventory() {
    try {
        RequestLogger.get().info("Sync started");
        inventory.sync();
    } catch (Exception ex) {
        RequestLogger.get().error("Inventory sync failed", ex);
    } finally {
        RequestLogger.clear();
    }
}
```

Work started from a request on another thread doesn't see the request's buffer ([#20](https://github.com/parvez3019/log4error/issues/20)).

## Testing your app

In `@SpringBootTest` + MockMvc tests, the filter runs as in production. To assert on flushed output, attach a Logback `ListAppender` to `io.github.parvez3019.Logger`. The starter's own [`Log4ErrorFilterIT`](https://github.com/parvez3019/log4error/blob/main/log4error-spring-boot-starter/src/test/java/io/github/parvez3019/spring/Log4ErrorFilterIT.java) shows the pattern.

In plain unit tests without the filter, call `RequestLogger.clear()` in `@AfterEach` so state doesn't leak between tests that share a thread.

## See also

- [How it works](how-it-works.md): flush semantics, timestamps, memory
- [Troubleshooting & FAQ](troubleshooting.md)
- [Runnable demo](https://github.com/parvez3019/log4error/blob/main/examples/spring-boot-demo)
