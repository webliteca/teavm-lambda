# Deployment: WAR (Servlet Containers)

> Read this when the user wants to deploy to Tomcat, TomEE, Jetty, or any Servlet 6.0+ container.

## Profile: jvm-war

```bash
mvn clean package -P jvm-war
# Deploy target/my-app-1.0.0.war to your servlet container
```

## Key difference: WarServlet, not Platform.start()

In WAR deployments, the servlet container manages the lifecycle. **Do not call `Platform.start()`**. Instead, subclass `WarServlet` and override `createRouter()`:

```java
package com.example.myapp;

import ca.weblite.teavmlambda.api.*;
import ca.weblite.teavmlambda.api.db.*;
import ca.weblite.teavmlambda.api.middleware.CorsMiddleware;
import ca.weblite.teavmlambda.generated.GeneratedContainer;
import ca.weblite.teavmlambda.generated.GeneratedRouter;
import ca.weblite.teavmlambda.impl.jvm.war.WarServlet;
import jakarta.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = "/*")
public class MyServlet extends WarServlet {

    @Override
    protected Router createRouter() {
        String dbUrl = Platform.env("DATABASE_URL",
                "postgresql://demo:demo@localhost:5432/demo");

        Container container = new GeneratedContainer();
        container.register(Database.class, DatabaseFactory.create(dbUrl));

        return new MiddlewareRouter(new GeneratedRouter(container))
                .use(CorsMiddleware.builder().build());
    }
}
```

`createRouter()` is called once during servlet `init()`.

## Dependencies

In the `jvm-war` profile:
- `teavm-lambda-adapter-war` (scope: provided — classes are in the WAR)
- `teavm-lambda-core-jvm`
- `teavm-lambda-db-jvm` + `org.postgresql:postgresql:42.7.3` (if using database)
- `jakarta.servlet:jakarta.servlet-api:6.0.0` (scope: provided)

## Build plugin

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-war-plugin</artifactId>
    <version>3.4.0</version>
</plugin>
```

The annotation processor still runs via `maven-compiler-plugin` to generate `GeneratedRouter` and `GeneratedContainer`.

## Container requirements

- **Servlet 6.0+** (Jakarta namespace) — NOT javax.servlet
- Tomcat 10.1+
- TomEE 10+
- Jetty 12+

## Dockerfile for Tomcat deployment

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn clean package -P jvm-war -q

FROM tomcat:10.1-jdk21
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/*.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
```

## Environment variables in servlet containers

`Platform.env()` reads `System.getenv()` on JVM, so set environment variables at the OS/container level:

```bash
# Docker
docker run -e DATABASE_URL=postgresql://... my-war-app

# Tomcat setenv.sh
export DATABASE_URL=postgresql://...
```

See `references/pom-templates.md` for the complete WAR pom.xml template.
