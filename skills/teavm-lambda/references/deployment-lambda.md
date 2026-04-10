# Deployment: AWS Lambda

> Read this when the user wants to deploy to AWS Lambda via SAM, either as TeaVM/Node.js or JVM.

## TeaVM / Node.js Lambda (default profile)

The default `teavm` profile compiles Java to JavaScript. The output in `target/lambda/` contains `index.js` (bootstrap), `teavm-app.js` (compiled app), `package.json`, and `node_modules/`.

### Build

```bash
mvn clean package
```

### SAM template (template.yaml)

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: teavm-lambda application

Globals:
  Function:
    Timeout: 30
    MemorySize: 256
    Runtime: nodejs22.x

Resources:
  ApiFunction:
    Type: AWS::Serverless::Function
    Properties:
      CodeUri: target/lambda/
      Handler: index.handler
      Environment:
        Variables:
          DATABASE_URL: !Sub "postgresql://${DBUser}:${DBPass}@${DBHost}:5432/${DBName}"
      Events:
        ProxyApi:
          Type: Api
          Properties:
            Path: /{proxy+}
            Method: ANY
        RootApi:
          Type: Api
          Properties:
            Path: /
            Method: ANY
```

### Local testing

```bash
# Start PostgreSQL
docker compose up -d

# Start SAM local API
sam local start-api --template template.yaml

# Test
curl http://localhost:3000/hello
```

### Deploy

```bash
sam deploy --guided
```

### Required files in project

**docker/bootstrap.js** (copied to `target/lambda/index.js` during build):
```javascript
const teavm = require('./teavm-app.js');
teavm.main([]);
exports.handler = async (event, context) => {
    return global.__teavmLambdaHandler(event, context);
};
```

**docker/package.json**:
```json
{
  "name": "my-lambda-app",
  "version": "1.0.0",
  "dependencies": {
    "pg": "^8.13.0"
  }
}
```

Add npm dependencies based on what cloud services you use (e.g., `@aws-sdk/client-s3`, `@aws-sdk/client-sqs`, `@aws-sdk/client-dynamodb`).

---

## JVM Lambda (jvm profile)

Build an uber JAR for the Java 21 Lambda runtime.

### Build

```bash
mvn clean package -P jvm
```

### SAM template

```yaml
Resources:
  ApiFunction:
    Type: AWS::Serverless::Function
    Properties:
      CodeUri: target/
      Handler: ca.weblite.teavmlambda.impl.jvm.lambda.LambdaHandler::handleRequest
      Runtime: java21
      MemorySize: 512
      Timeout: 30
```

### Dependencies

In the `jvm` profile, replace the TeaVM adapter with JVM modules:
- `teavm-lambda-adapter-lambda-jvm` (instead of `teavm-lambda-adapter-lambda`)
- `teavm-lambda-core-jvm` (instead of `teavm-lambda-core-js`)
- `teavm-lambda-db-jvm` + `org.postgresql:postgresql:42.7.3` (instead of `teavm-lambda-db`)
- Uses `maven-shade-plugin` with `ServicesResourceTransformer`

### Trade-offs

| | TeaVM (Node.js) | JVM |
|---|---|---|
| Cold start | ~100ms | ~3-8s |
| Warm latency | Comparable | Comparable |
| Package size | ~5-15 MB | ~15-30 MB |
| Debugging | Limited (compiled JS) | Full JVM tools |
| Local dev | SAM + Docker | `java -jar` or SAM |

---

## Docker Compose for local PostgreSQL

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:16
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: demo
      POSTGRES_PASSWORD: demo
      POSTGRES_DB: demo
    volumes:
      - ./docker/init.sql:/docker-entrypoint-initdb.d/init.sql
```
