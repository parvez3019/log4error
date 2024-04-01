# What is `log4error`?

We often see organisations turning off `INFO` level logs in production to reduce the logging cost.
I have worked in many projects where teams have only enabled `ERROR` level logs in production.

The problem with logging only an `ERROR` log is you don't get any other information at the time of any unexpected incident, just the limited info from the error logs along with stack trace.
Contextual, logical, or debugging information is not present since you have disabled info logs.

# Detailed Article - 
[Medium Link](https://medium.com/@pha3019/log4error-java-library-for-reduced-info-level-logging-5f1c29867fc4)

## With `log4error` library -
- You can `COLLECT INFO LOGS` on the GO and `print` them to the console only when an `exception occurs`.

## How does it work? 
- It keeps collecting info logs in an in-memory request scoped local thread.
- When you print an error log at any statement, at the point it prints the complete info stack for reference.
- That helps debug issues when they occur and reduces overall logging data.

## Performance Statistics -
- There is going to be a performance difference between both logging approaches.
- Since we are keeping an in-memory local stack of info logs, we are ingesting info log messages during info statements.
- And printing all info logs during error, there is an increase in time to process both usages.

```
RUN Suite of 10 sets while logging 10,000 times each time to get average performance -

Info Logs -
Log4j - Average log time was 15 ns
log4error -  Average log time was 38 ns

ERROR Logs - 
Log4j - Average log time was 15 ns
log4error - Average log time was 42 ns
```

Based on this trade-off, you can decide whether this will be helpful for you or not.

## How to use it -

### Create a LoggerFilter
- Create a `LoggerFilter.Class` for the initialization of Logger
- Check out `LoggerFilterExample.class` for reference.

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

## Maven Central Repository - [Link](https://central.sonatype.com/artifact/io.github.parvez3019/log4error)

### Add the following dependency to your pom.xml file


Reference For GitHub token and dependency download - [Link](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages)

Maven 
```
<dependency>
    <groupId>io.github.parvez3019</groupId>
    <artifactId>log4error</artifactId>
    <version>0.0.8</version>
</dependency>
```

Gradle
```
implementation group: 'io.github.parvez3019', name: 'log4error', version: '0.0.8'
```

Gradle (short)
```
implementation 'io.github.parvez3019:log4error:0.0.8'
```

Gradle (kotlin)
```
implementation("io.github.parvez3019:log4error:0.0.8")
```


## Run the following command for installation
```
mvn clean install
```
