# Minimal Hello World

The smallest working teavm-lambda application. One resource class, one Main class, one pom.xml.

## Prerequisites

- JDK 21
- Maven 3.9+
- teavm-lambda installed in local Maven repo (`mvn install` from the teavm-lambda root)

## Build and Run

```bash
mvn clean package
java -jar target/minimal-hello-1.0.0.jar
```

## Test

```bash
curl http://localhost:8080/hello
# {"message":"Hello, World!"}

curl http://localhost:8080/hello/Alice
# {"message":"Hello, Alice!"}
```
