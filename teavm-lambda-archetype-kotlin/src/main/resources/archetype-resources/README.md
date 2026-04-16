# ${artifactId}

A teavm-lambda Kotlin project with three deployment targets.

## Quick Start

```bash
./run.sh                  # JVM standalone (default, port 8080)
./run.sh cloudrun         # TeaVM/Node.js (no Docker needed)
./run.sh lambda           # TeaVM/Node.js via SAM (needs Docker)
./run.sh jvm-server 3000  # JVM on custom port
```

## Build and Run (Manual)

### JVM Standalone Server (default)

```bash
mvn clean package
java -jar target/${artifactId}-1.0.0.jar
```

### TeaVM / Node.js (no Docker)

```bash
mvn clean package -P cloudrun
node target/cloudrun/server.js
```

### AWS Lambda (SAM)

```bash
mvn clean package -P lambda
sam local start-api
```

### Docker (Cloud Run)

```bash
docker build -t ${artifactId} .
docker run -p 8080:8080 ${artifactId}
```

## Test

```bash
curl http://localhost:8080/hello
# {"message":"Hello, World!"}

curl http://localhost:8080/hello/Alice
# {"message":"Hello, Alice!"}
```
