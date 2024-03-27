# What is info-log-optimizer?

We often see organisations turning off `INFO` level logs in production to reduce the logging cost.
I have worked in many projects where teams have only enabled `ERROR` level logs in production.

The problem with logging only an `ERROR` log is at the time of any unexpected incident you don't get any other information, just the limited info from the error logs along with stack trace.
Contextual, logical information or debugging-related information is not present since you have disabled info logs.


## With `info-log-optimizer` library -
- You can `COLLECT INFO LOGS` on the GO and `print` them to the console only when an `exception occurs`.

## How it works? 
- It keeps collecting info logs in an in-memory request scoped local thread.
- When you print an error log at any statement, at the point it prints the complete info stack for reference.
- That helps in debugging issues when they occur and also reduces overall logging data.

## How to use it -

### Create a LoggerFilter
- Create a LoggerFilter.class for the initialization of Logger
- Checkout `LoggerFilterExample.class` for reference.

### Logger.info()
- Using the Logger().info(String message, Object... obj) method you can collect the info logs, across your application.

```
  Logger().info(String message, Object... obj)
  example - Logger().info("Here I am printing some logs with argument one: {} and arg 2 : {}", arg1, arg2)
```

### Logger.error()
- Will print the complete info log stack until that point and will reset the info log stack to empty.
```
- Logger().error(String message, Object... obj);
```

### Static log methods
- For static log methods you can use the following methods -
```
  Logger.printInfo(String message, Object... obj)
  Logger.printError(String message, Object... obj)
  Logger.debug(String message, Object... obj)
  Logger.warn(String message, Object... obj)
```

# Installation

## Add the following dependency to your pom.xml file
```
<dependency>
  <groupId>io.github.parvez3019</groupId>
  <artifactId>info-log-optimizer</artifactId>
  <version>0.0.1</version>
</dependency>
```

## Run the following command for installation
```
mvn clean install
```
