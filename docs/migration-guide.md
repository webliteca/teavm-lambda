# Migration Guide: WORA (Write Once Run Anywhere)

This guide covers migrating an existing teavm-lambda application from the
platform-specific API (TeaVM/Node.js only) to the platform-neutral WORA API
that runs on both Node.js and JVM.

## Overview

The migration has three parts:
1. **Resource classes** — replace JS-specific types with platform-neutral interfaces
2. **Main class** — replace `@JSBody`/`LambdaAdapter` with `Platform`/`DatabaseFactory`
3. **pom.xml** — move platform-specific deps into Maven profiles

After migration, the same source code compiles for both targets:
```bash
mvn clean package            # Node.js/Lambda (default)
mvn clean package -P jvm     # JVM/Lambda
```

---

## Step 1: Migrate Resource Classes

Resource classes are the bulk of your application code. The migration replaces
JS-specific types with their platform-neutral counterparts.

### Import replacements

| Before (JS-specific) | After (platform-neutral) |
|---|---|
| `import io.teavmlambda.db.Db;` | `import ca.weblite.teavmlambda.api.db.Database;` |
| `import io.teavmlambda.db.PgResult;` | `import ca.weblite.teavmlambda.api.db.DbResult;` |
| `import io.teavmlambda.db.JsUtil;` | `import ca.weblite.teavmlambda.api.db.Json;` |
| `import org.teavm.jso.JSObject;` | `import ca.weblite.teavmlambda.api.db.DbRow;` |
| `import org.teavm.jso.core.JSArray;` | *(remove — not needed)* |

### Type replacements

| Before | After |
|---|---|
| `Db db` | `Database db` |
| `PgResult result` | `DbResult result` |
| `JSArray<JSObject> rows` | `List<DbRow> rows` |
| `JSObject parsed` | `DbRow parsed` |

### Method replacements

| Before | After |
|---|---|
| `result.getRows()` (returns `JSArray<JSObject>`) | `result.getRows()` (returns `List<DbRow>`) |
| `result.getRowCount()` | `result.getRowCount()` *(unchanged)* |
| `rows.getLength()` | `rows.size()` |
| `rows.get(i)` | `rows.get(i)` *(unchanged)* |
| `JsUtil.toJson(row)` | `row.toJson()` |
| `JsUtil.parseJson(str)` | `Json.parse(str)` |
| `JsUtil.getStringProperty(obj, "name")` | `row.getString("name")` |
| `JsUtil.getIntProperty(obj, "age")` | `row.getInt("age")` |
| `JsUtil.getDoubleProperty(obj, "score")` | `row.getDouble("score")` |
| `JsUtil.hasProperty(obj, "field")` | `row.has("field")` |

### Serializing all rows to a JSON array

Before:
```java
PgResult result = db.query("SELECT * FROM users");
JSArray<JSObject> rows = result.getRows();
StringBuilder json = new StringBuilder("[");
for (int i = 0; i < rows.getLength(); i++) {
    if (i > 0) json.append(",");
    json.append(JsUtil.toJson(rows.get(i)));
}
json.append("]");
return Response.ok(json.toString());
```

After:
```java
DbResult result = db.query("SELECT * FROM users");
return Response.ok(result.toJsonArray());
```

### Serializing a single row

Before:
```java
return Response.ok(JsUtil.toJson(result.getRows().get(0)));
```

After:
```java
return Response.ok(result.getRows().get(0).toJson());
```

### Parsing a JSON request body

Before:
```java
JSObject parsed = JsUtil.parseJson(body);
String name = JsUtil.getStringProperty(parsed, "name");
String email = JsUtil.getStringProperty(parsed, "email");
```

After:
```java
DbRow parsed = Json.parse(body);
String name = parsed.getString("name");
String email = parsed.getString("email");
```

### Full example

Before:
```java
import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import io.teavmlambda.db.Db;
import io.teavmlambda.db.JsUtil;
import io.teavmlambda.db.PgResult;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;

@Path("/users")
public class UsersResource {

    private final Db db;

    public UsersResource(Db db) {
        this.db = db;
    }

    @GET
    public Response list() {
        PgResult result = db.query("SELECT * FROM users");
        JSArray<JSObject> rows = result.getRows();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < rows.getLength(); i++) {
            if (i > 0) json.append(",");
            json.append(JsUtil.toJson(rows.get(i)));
        }
        json.append("]");
        return Response.ok(json.toString())
                .header("Content-Type", "application/json");
    }

    @POST
    public Response create(@Body String body) {
        JSObject parsed = JsUtil.parseJson(body);
        String name = JsUtil.getStringProperty(parsed, "name");
        db.query("INSERT INTO users (name) VALUES ($1)", name);
        return Response.status(201).body(body);
    }
}
```

After:
```java
import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DbResult;
import ca.weblite.teavmlambda.api.db.DbRow;
import ca.weblite.teavmlambda.api.db.Json;

@Path("/users")
public class UsersResource {

    private final Database db;

    public UsersResource(Database db) {
        this.db = db;
    }

    @GET
    public Response list() {
        DbResult result = db.query("SELECT * FROM users");
        return Response.ok(result.toJsonArray())
                .header("Content-Type", "application/json");
    }

    @POST
    public Response create(@Body String body) {
        DbRow parsed = Json.parse(body);
        String name = parsed.getString("name");
        db.query("INSERT INTO users (name) VALUES ($1)", name);
        return Response.status(201).body(body);
    }
}
```

**What stays the same:** Annotations (`@Path`, `@GET`, `@POST`, `@PathParam`,
`@Body`, etc.), `Response` builder pattern, `Request` class, SQL with `$1`
placeholders. These were already platform-neutral.

---

## Step 2: Migrate Main Class

### Replace environment access

Before:
```java
import org.teavm.jso.JSBody;

@JSBody(params = {"name"}, script = "return process.env[name] || '';")
private static native String getenv(String name);

// usage:
String dbUrl = getenv("DATABASE_URL");
if (dbUrl == null || dbUrl.isEmpty()) {
    dbUrl = "postgresql://demo:demo@localhost:5432/demo";
}
```

After:
```java
import ca.weblite.teavmlambda.api.Platform;

// usage:
String dbUrl = Platform.env("DATABASE_URL", "postgresql://demo:demo@localhost:5432/demo");
```

### Replace database creation

Before:
```java
import io.teavmlambda.db.Db;
import io.teavmlambda.db.PgPool;

PgPool pool = PgPool.create(dbUrl);
Db db = new Db(pool);
```

After:
```java
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DatabaseFactory;

Database db = DatabaseFactory.create(dbUrl);
```

### Replace adapter startup

Before:
```java
import io.teavmlambda.adapter.lambda.LambdaAdapter;
LambdaAdapter.start(router);
```
or:
```java
import io.teavmlambda.adapter.cloudrun.CloudRunAdapter;
CloudRunAdapter.start(router);
```

After:
```java
import ca.weblite.teavmlambda.api.Platform;
Platform.start(router);
```

### Full Main.java example

Before:
```java
import io.teavmlambda.adapter.lambda.LambdaAdapter;
import ca.weblite.teavmlambda.api.Router;
import io.teavmlambda.db.Db;
import io.teavmlambda.db.PgPool;
import ca.weblite.teavmlambda.generated.GeneratedRouter;
import org.teavm.jso.JSBody;

public class Main {
    @JSBody(params = {"name"}, script = "return process.env[name] || '';")
    private static native String getenv(String name);

    public static void main(String[] args) {
        String dbUrl = getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "postgresql://demo:demo@localhost:5432/demo";
        }
        PgPool pool = PgPool.create(dbUrl);
        Db db = new Db(pool);
        Router router = new GeneratedRouter(
            new HealthResource(),
            new UsersResource(db));
        LambdaAdapter.start(router);
    }
}
```

After:
```java
import ca.weblite.teavmlambda.api.Platform;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DatabaseFactory;
import ca.weblite.teavmlambda.generated.GeneratedRouter;

public class Main {
    public static void main(String[] args) {
        String dbUrl = Platform.env("DATABASE_URL", "postgresql://demo:demo@localhost:5432/demo");
        Database db = DatabaseFactory.create(dbUrl);
        Router router = new GeneratedRouter(
            new HealthResource(),
            new UsersResource(db));
        Platform.start(router);
    }
}
```

---

## Step 3: Migrate pom.xml

### Move platform-specific dependencies into profiles

**Before:** all dependencies at the top level, TeaVM-only.

**After:** platform-neutral deps at top level, platform-specific deps in profiles.

#### 3a. Keep these as top-level dependencies

```xml
<dependencies>
    <dependency>
        <groupId>ca.weblite</groupId>
        <artifactId>teavm-lambda-core</artifactId>
    </dependency>
    <dependency>
        <groupId>ca.weblite</groupId>
        <artifactId>teavm-lambda-db-api</artifactId>
    </dependency>
    <dependency>
        <groupId>ca.weblite</groupId>
        <artifactId>teavm-lambda-processor</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### 3b. Move these into a `teavm` profile (active by default)

```xml
<profile>
    <id>teavm</id>
    <activation>
        <activeByDefault>true</activeByDefault>
    </activation>
    <dependencies>
        <!-- Your existing adapter (lambda or cloudrun) -->
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-adapter-lambda</artifactId>
        </dependency>
        <!-- JS database implementation -->
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-db</artifactId>
        </dependency>
        <!-- TeaVM runtime -->
        <dependency>
            <groupId>org.teavm</groupId>
            <artifactId>teavm-classlib</artifactId>
        </dependency>
        <dependency>
            <groupId>org.teavm</groupId>
            <artifactId>teavm-jso</artifactId>
        </dependency>
        <dependency>
            <groupId>org.teavm</groupId>
            <artifactId>teavm-jso-apis</artifactId>
        </dependency>
    </dependencies>
    <build>
        <!-- Move your existing teavm-maven-plugin config here -->
    </build>
</profile>
```

#### 3c. Add a `jvm` profile

For **Lambda** deployments:
```xml
<profile>
    <id>jvm</id>
    <dependencies>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-adapter-lambda-jvm</artifactId>
        </dependency>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-core-jvm</artifactId>
        </dependency>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-db-jvm</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.3</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.2</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>your.package.Main</mainClass>
                                </transformer>
                                <!-- Required: merges META-INF/services for ServiceLoader -->
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

For **HTTP server** (Cloud Run, Docker, standalone) deployments, replace
`teavm-lambda-adapter-lambda-jvm` with `teavm-lambda-adapter-httpserver`.

#### 3d. Compiler plugin in both profiles

Both profiles need the annotation processor configured:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>ca.weblite</groupId>
                <artifactId>teavm-lambda-processor</artifactId>
                <version>${project.version}</version>
            </path>
            <path>
                <groupId>ca.weblite</groupId>
                <artifactId>teavm-lambda-core</artifactId>
                <version>${project.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## Optional: Feature Detection

Use `isAvailable()` to check for optional capabilities at runtime:

```java
if (DatabaseFactory.isAvailable()) {
    Database db = DatabaseFactory.create(url);
}

if (Sentry.isAvailable()) {
    Sentry.init(dsn);
}
```

Every factory supports this: `Platform.isAvailable()`, `DatabaseFactory.isAvailable()`,
`Json.isAvailable()`, `Logger.isAvailable()`, `Sentry.isAvailable()`,
`Resources.isAvailable()`.

---

## Verifying the Migration

### Check SPI parity
```bash
./check-spi-parity.sh
```
Verifies that every SPI interface has both JS and JVM implementations registered.

### Run the same tests against both platforms
```bash
# Test against Node.js
mvn clean package -P teavm
# start your server...
./teavm-lambda-demo/test.sh http://localhost:3001

# Test against JVM
mvn clean package -P jvm
# start your server...
./teavm-lambda-demo/test.sh http://localhost:8080
```

Both should produce identical results.

---

## Quick Reference: What Not to Use in WORA Code

These are **JS-only** — avoid in application code:

| Do not use | Use instead |
|---|---|
| `@JSBody` | `Platform.env()` |
| `org.teavm.jso.*` | `ca.weblite.teavmlambda.api.db.*` |
| `Db` / `PgPool` | `DatabaseFactory.create()` |
| `PgResult` | `DbResult` |
| `JSObject` / `JSArray` | `DbRow` / `List<DbRow>` |
| `JsUtil.toJson()` | `row.toJson()` / `result.toJsonArray()` |
| `JsUtil.parseJson()` | `Json.parse()` |
| `JsUtil.getStringProperty()` | `row.getString()` |
| `LambdaAdapter.start()` | `Platform.start()` |
| `CloudRunAdapter.start()` | `Platform.start()` |
| `process.env` | `Platform.env()` |
