# Examples

Reference integration code for log4error. These files are **not** packaged in the published JAR.

| File | Purpose |
|------|---------|
| [`LoggerFilterExample.java`](LoggerFilterExample.java) | Spring `OncePerRequestFilter` that binds a request-scoped `Logger` to a ThreadLocal |

Requires `spring-web` and `jakarta.servlet-api` on your application classpath.
