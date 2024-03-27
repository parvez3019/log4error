# What is info-log-optimizer?

We often see organisations turning off `INFO` level logs in production to reduce the logging cost.
I have worked in many projects where teams have only enabled `ERROR` level logs in production.

The problem with logging only an `ERROR` log is you don't get any other information at the time of any unexpected incident, just the limited info from the error logs along with stack trace.
Contextual, logical, or debugging information is not present since you have disabled info logs.


## With `info-log-optimizer` library -
- You can `COLLECT INFO LOGS` on the GO and `print` them to the console only when an `exception occurs`.

## How it works? 
- It keeps collecting info logs in an in-memory request scoped local thread.
- When you print an error log at any statement, at the point it prints the complete info stack for reference.
- That helps debug issues when they occur and reduces overall logging data.

## How to use it -

### Create a LoggerFilter
- Create a LoggerFilter.class for the initialization of Logger
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

## Add the following dependency to your pom.xml file

- Add Github Repository in your `settings.xml` (can be found at `~/.m2/` path)
```
<settings xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.1.0 http://maven.apache.org/xsd/settings-1.1.0.xsd"
          xmlns="http://maven.apache.org/SETTINGS/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <servers>
    <server>
      <id>github</id>
      <username>{Your-github-username}</username>
      <password>{token-create-personal-classic-token-with-read-packages-access}</password>
    </server>
  </servers>

 <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>

   <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
        </repository>
        <repository>
          <id>github</id>
          <url>https://maven.pkg.github.com/parvez3019/info-log-optimizer/</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
</settings>
```

Reference For GitHub token and dependency download - [Link](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages)

```
<dependency>
  <groupId>io.github.parvez3019</groupId>
  <artifactId>info-log-optimizer</artifactId>
  <version>0.0.3</version>
</dependency>
```


## Run the following command for installation
```
mvn clean install
```
