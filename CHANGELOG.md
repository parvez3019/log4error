# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-07-11

Major cleanup release. See [Upgrade guide: 0.0.x → 0.1.0](docs/upgrade-0.1.0.md).

### Added
- `Logger.of(Class)` / `Logger.of(org.slf4j.Logger)` to preserve call-site logger identity
- Configurable buffer cap (default **500**); drops oldest events with a one-time WARN
- Throwable preservation on buffered `info` / `debug` flush
- Defensive copy of formatting argument arrays
- Unit and component integration tests (Surefire + Failsafe)
- Root `Makefile` for local setup, test, verify, package, and benchmark
- Example filter moved to [`examples/`](examples/) (documented, not packaged)
- Upgrade and docs under [`docs/`](docs/)

### Changed
- Version bump to **0.1.0**
- Core library depends only on `slf4j-api` at compile time
- `LoggerThreadLocal.remove()` no longer invents a Logger when unset
- Buffer storage uses `ArrayList` with a max size
- CI verify job skips GPG; source/javadoc/GPG signing moved to Maven `release` profile
- JMH benchmarks live under test sources; annotation processor wired for `make benchmark`

### Removed
- Spring `@Component` / `@RequestScope` from core `Logger` and `LoggerThreadLocal`
- Compile-time Spring, Tomcat embed, javax.servlet, Log4j, commons-lang3, Disruptor, and JMH from the published artifact
- Packaged `LoggerFilterExample` and benchmarking classes from the main JAR
- Stale Log4j 1.x `log4j.properties` from main resources

### Fixed
- Buffered logs that included a trailing `Throwable` no longer drop the stack trace on flush
- Spring 5 + Spring 6 / javax + jakarta classpath inconsistency in the library POM

### Migration
- Consumers must use filter + ThreadLocal (not Spring DI of `Logger`)
- Declare Spring Web / servlet yourself if needed
- Copy filter from `examples/` instead of importing the old packaged class

## [0.0.11] - 2024-04-08

### Changed
- Bump published artifact version to 0.0.11
- Logger updates and Java toolchain set to 17

## [0.0.10] - 2024-04-06

### Added
- Benchmark comparison against SLF4J / Log4j-style logging

### Changed
- Bump published artifact version to 0.0.10
- Java upgrade work landed in this line

## [0.0.9] - 2024-04-06

### Added
- Maven Central publishing support

### Changed
- Bump published artifact version to 0.0.9

## [0.0.8] - 2024-04-05

### Changed
- GitHub Actions workflows: remove publish-on-push; tighten deploy flow
- POM cleanup (duplicate imports)

## [0.0.7] - 2024-03-30

### Added
- Performance tests
- Collect DEBUG logs by default alongside INFO in the buffer path

### Fixed
- GitHub Actions / gitflow version wiring

## [0.0.6] - 2024-03-28

### Changed
- Dependency and artifact version bump to 0.0.6

## [0.0.5] - 2024-03-28

### Changed
- Artifact version bump to 0.0.5

## [0.0.4] - 2024-03-28

### Changed
- Artifact version bump to 0.0.4

## [0.0.3] - 2024-03-27

### Changed
- Artifact version bump to 0.0.3

## [0.0.2] - 2024-03-27

### Changed
- Artifact version bump to 0.0.2

### Notes
- Early Maven Central / GPG publishing iterations for the library

[Unreleased]: https://github.com/parvez3019/log4error/compare/0.1.0...HEAD
[0.1.0]: https://github.com/parvez3019/log4error/compare/0.0.11...0.1.0
[0.0.11]: https://github.com/parvez3019/log4error/compare/0.0.10...0.0.11
[0.0.10]: https://github.com/parvez3019/log4error/compare/0.0.9...0.0.10
[0.0.9]: https://github.com/parvez3019/log4error/compare/0.0.8...0.0.9
[0.0.8]: https://github.com/parvez3019/log4error/compare/0.0.7...0.0.8
[0.0.7]: https://github.com/parvez3019/log4error/compare/0.0.6...0.0.7
[0.0.6]: https://github.com/parvez3019/log4error/compare/0.0.5...0.0.6
[0.0.5]: https://github.com/parvez3019/log4error/compare/0.0.4...0.0.5
[0.0.4]: https://github.com/parvez3019/log4error/compare/0.0.3...0.0.4
[0.0.3]: https://github.com/parvez3019/log4error/compare/0.0.2...0.0.3
[0.0.2]: https://github.com/parvez3019/log4error/releases/tag/0.0.2
