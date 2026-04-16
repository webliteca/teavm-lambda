# CRUD with PostgreSQL

Demonstrates the repository pattern with PostgreSQL: resource class, service, repository, Database interface.

## Prerequisites

- JDK 21, Maven 3.9+
- PostgreSQL 16 (or Docker)
- teavm-lambda installed locally

## Setup

```bash
# Start PostgreSQL
docker run -d --name demo-pg -p 5432:5432 \
    -e POSTGRES_USER=demo -e POSTGRES_PASSWORD=demo -e POSTGRES_DB=demo \
    postgres:16

# Create table
docker exec -i demo-pg psql -U demo -d demo <<'SQL'
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
SQL
```

## Build and Run

```bash
mvn clean package
java -jar target/crud-postgres-1.0.0.jar
```

## Test

```bash
# Create
curl -X POST http://localhost:8080/users \
    -H "Content-Type: application/json" \
    -d '{"name":"Alice","email":"alice@example.com"}'

# List
curl http://localhost:8080/users

# Get by ID
curl http://localhost:8080/users/<id>

# Delete
curl -X DELETE http://localhost:8080/users/<id>
```
