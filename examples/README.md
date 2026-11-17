# Examples

Reference integration code for log4error. Nothing here is packaged in the published JARs.

| Path | Purpose |
|------|---------|
| [`spring-boot-demo/`](spring-boot-demo) | Runnable Spring Boot app using `log4error-spring-boot-starter`. Shows that the happy path writes nothing and a failure flushes the context. Run with `make demo`. |
| [`LoggerFilterExample.java`](LoggerFilterExample.java) | Copy-paste Spring `OncePerRequestFilter` for apps **not** using the starter. Binds a request-scoped `Logger` to a ThreadLocal. Requires `spring-web` and `jakarta.servlet-api`. |
