# log4error Spring Boot demo

A minimal Spring Boot 3 app that uses `log4error-spring-boot-starter`. The app logs at WARN (as many production setups do); log4error decides when the request's INFO/DEBUG context is written.

## Run

From the repository root:

```bash
make demo
```

## Try it

```bash
curl -H 'x-request-id: req-7f3a' localhost:8080/orders/1      # happy path
curl -H 'x-request-id: req-9b21' localhost:8080/orders/fail   # payment fails
```

**Happy path:** the console prints nothing. Four INFO/DEBUG lines were buffered and then discarded.

**Failure:** the buffered lines are flushed with the error:

```
10:33:15.376 INFO  [req-9b21] i.g.p.Logger - Fetching order fail
10:33:15.376 INFO  [req-9b21] i.g.p.Logger - Order fail has 3 items, total=42.50 EUR
10:33:15.376 DEBUG [req-9b21] i.g.p.Logger - Cache miss for customer profile, loading from DB
10:33:15.376 INFO  [req-9b21] i.g.p.Logger - Calling payment provider for order fail
10:33:15.376 ERROR [req-9b21] i.g.p.Logger - Payment failed for order fail
java.lang.IllegalStateException: card declined (code 51)
	at io.github.parvez3019.demo.OrderController.charge(OrderController.java:33)
	...
```

## Files

- [`OrderController.java`](src/main/java/io/github/parvez3019/demo/OrderController.java): the call sites that use `RequestLogger.get()`
- [`application.yml`](src/main/resources/application.yml): log pattern with `%X{x-request-id}` and the `log4error.*` settings
