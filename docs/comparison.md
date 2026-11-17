---
description: "When to use fingers-crossed logging in Java, and how log4error compares with log sampling, runtime log levels and APM breadcrumbs."
---

# When to use log4error, and how it compares

## Good fit

- Request/response services (REST, servlet) where **most requests succeed**.
- **INFO volume is a real cost**: log ingestion, storage, or search time.
- You debug production failures **from logs** and want the request's context next to the error.

## Not a fit

- **Audit or security trails** that must always be written. Use `pInfo` or your normal logger for those lines.
- **Long-running batch jobs** without a clear per-unit boundary to flush or clear at.
- **Fully reactive stacks** (WebFlux, coroutines). Not supported yet ([#24](https://github.com/parvez3019/log4error/issues/24)).

## Alternatives

| Approach | What you get | Trade-off |
|----------|--------------|-----------|
| **Keep INFO on** | Full context for every request | You pay ingestion and storage for millions of successful requests nobody reads. |
| **Log sampling** | Lower volume | It keeps a random fraction of requests, so the failing request's context is usually the part that got dropped. |
| **Change log levels at runtime** | Detail on demand | It only helps *after* you notice a problem, and the failure has to happen again. |
| **Sentry / APM breadcrumbs** | Context attached to errors | Works well if you already pay for it, but the context lives in another tool. |
| **log4error** | Full context for failed requests only | You call `error()` at failure points; the buffer is thread-bound ([limits](how-it-works.md#threading)). |

## Prior art

The same "fingers-crossed" pattern exists in other ecosystems:

- PHP **Monolog**: `FingersCrossedHandler`
- **Python** standard library: `logging.handlers.MemoryHandler`
- **.NET**: log buffering in `Microsoft.Extensions.Telemetry`

log4error brings it to SLF4J and Spring Boot.

See also: [Performance](performance.md) · [How it works](how-it-works.md)
