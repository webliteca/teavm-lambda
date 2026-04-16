# JWT Protected API

Demonstrates JWT authentication with @RolesAllowed and SecurityContext.

## Prerequisites

- JDK 21, Maven 3.9+
- teavm-lambda installed locally

## Build and Run

```bash
JWT_SECRET=my-secret-key mvn clean package
JWT_SECRET=my-secret-key java -jar target/jwt-protected-1.0.0.jar
```

## Test

```bash
# Without token — 401 Unauthorized
curl http://localhost:8080/api/me

# With a valid JWT (use jwt.io to create one with {"sub":"user1","groups":["user"]})
TOKEN="eyJ..."
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/me

# Admin-only endpoint — needs "admin" in groups claim
curl -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/api/admin
```
