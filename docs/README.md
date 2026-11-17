# Docs

These pages are published as a website at **https://parvez3019.github.io/log4error/** (built with MkDocs from this folder; see `mkdocs.yml`).

**Start here:** the [README](../README.md) quick start, then the [Spring Boot guide](spring-boot.md).

## Guides

| Document | Description |
|----------|-------------|
| [Spring Boot guide](spring-boot.md) | Starter install, properties, required logging levels, replacing the filter, background threads, testing |
| [When to use it & comparison](comparison.md) | Good fit / not a fit, alternatives, prior art |
| [Performance](performance.md) | JMH benchmarks |
| [How it works](how-it-works.md) | Request lifecycle, exact flush semantics, timestamps, buffer cap and memory, threading |
| [Troubleshooting & FAQ](troubleshooting.md) | "Only the ERROR is printed", missing request IDs, buffer-cap warnings, backend support |

## Upgrading

| Document | Description |
|----------|-------------|
| [Upgrade to 0.2.0](upgrade-0.2.0.md) | No breaking changes; switching a copied filter to the starter |
| [Upgrade to 0.1.0](upgrade-0.1.0.md) | Breaking changes and steps from 0.0.x |
| [Changelog](../CHANGELOG.md) | Release history by version / tag |

## More

| Document | Description |
|----------|-------------|
| [Spring Boot demo](../examples/spring-boot-demo/README.md) | Runnable before/after demo (`make demo`) |
| [Examples](../examples/README.md) | Copy-paste filter for non-Boot servlet apps |
| [Integrate & migrate prompt](INTEGRATE_AND_MIGRATE_PROMPT.md) | AI/agent prompt for adopting log4error |
| [Contributing](../CONTRIBUTING.md) | Build, test and PR conventions |
| [Security policy](../SECURITY.md) | Supported versions and vulnerability reporting |
| [Launch kit](launch/README.md) | Draft announcement posts |
