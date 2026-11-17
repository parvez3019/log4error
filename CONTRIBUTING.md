# Contributing to log4error

Thanks for helping. Bug reports, ideas, docs fixes and code are all welcome.

## Ways to help

- **Try it and tell us.** Post in [Discussions → Show and tell](https://github.com/parvez3019/log4error/discussions). A real-world usage note helps more than you'd think.
- **Pick an issue.** [`good first issue`](https://github.com/parvez3019/log4error/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) issues are small and well scoped. [`help wanted`](https://github.com/parvez3019/log4error/issues?q=is%3Aissue+is%3Aopen+label%3A%22help+wanted%22) issues are bigger features (Logback/Log4j2 integration, async propagation, WebFlux).
- **Report a bug** with the bug template. Include your Java, Spring Boot and logging backend versions.

## Project layout

| Module | Artifact | Notes |
|--------|----------|-------|
| `log4error/` | `io.github.parvez3019:log4error` | Core. **Compile dependency on `slf4j-api` only.** Keep it that way. |
| `log4error-spring-boot-starter/` | `io.github.parvez3019:log4error-spring-boot-starter` | Auto-configuration, `Log4ErrorFilter`, `RequestLogger` |
| `examples/spring-boot-demo/` | not published | Runnable demo (`make demo`) |
| `examples/LoggerFilterExample.java` | not built | Copy-paste filter for non-Boot apps |

## Build and test

You need JDK 17+ and Maven 3.9+.

```bash
make verify      # clean + unit + integration tests, all modules (same as CI)
make test        # unit tests only
make demo        # run the demo on :8080
make benchmark   # JMH benchmarks for core
```

Test conventions:

- `*Test` classes are unit tests, run by Surefire.
- `*IT` classes are integration tests, run by Failsafe.
- Use `LogCapture` (in each module's `support` test package) to assert on what was actually logged.
- Benchmarks live under `log4error/src/test/java/.../benchmarking` and are excluded from the test run.

## Pull requests

1. Fork the repo and branch from `main`.
2. Keep each PR focused: one fix or feature.
3. Add or update tests. `make verify` must pass.
4. Add a line under `## [Unreleased]` in [CHANGELOG.md](CHANGELOG.md).
5. Update the README or docs if behavior or the API changes.
6. Don't add compile dependencies to the core module.

By contributing, you agree that your work is licensed under the [Apache License 2.0](LICENSE). Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).
