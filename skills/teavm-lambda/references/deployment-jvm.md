# Deployment: JVM Standalone Server

> Read this when the user wants to run the app as a standalone JVM process (local dev, Docker, any platform).

## Profile: jvm-server

```bash
mvn clean package -P jvm-server
java -jar target/my-app-1.0-SNAPSHOT.jar
```

Uses `teavm-lambda-adapter-httpserver` — the JDK built-in `HttpServer` with zero external dependencies.

Listens on `PORT` environment variable (default 8080).

## Dependencies

In the `jvm-server` profile:
- `teavm-lambda-adapter-httpserver`
- `teavm-lambda-core-jvm`
- `teavm-lambda-db-jvm` + `org.postgresql:postgresql:42.7.3` (if using database)
- Other `-jvm` modules as needed (e.g., `teavm-lambda-auth-jvm`, `teavm-lambda-compression-jvm`)

## Build plugin

Uses `maven-shade-plugin` to produce an uber JAR:

```xml
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
                        <mainClass>com.example.myapp.Main</mainClass>
                    </transformer>
                    <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>
```

The `ServicesResourceTransformer` is essential — it merges `META-INF/services` files so ServiceLoader discovers all SPI implementations.

## Static files

Place static files in a `public/` directory next to the JAR. The HttpServer adapter serves them automatically with correct MIME types. Resolution order matches the Cloud Run adapter (see `references/deployment-cloudrun.md`).

## When to use

- **Local development**: fastest iteration cycle, no Docker needed
- **Docker deployment**: simple `java -jar` in any JDK 21 image
- **CI testing**: ideal for integration tests
- **Cloud Run (JVM)**: use with a JDK Docker image instead of TeaVM compilation

See `references/pom-templates.md` for the complete pom.xml template.
