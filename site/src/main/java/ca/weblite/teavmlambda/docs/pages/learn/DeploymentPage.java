package ca.weblite.teavmlambda.docs.pages.learn;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder.Div;
import ca.weblite.teavmreact.html.DomBuilder.Section;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

import ca.weblite.teavmlambda.docs.El;
import ca.weblite.teavmlambda.docs.components.CodeBlock;
import ca.weblite.teavmlambda.docs.components.Callout;

public class DeploymentPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Deployment"))
            .child(p("teavm-lambda compiles to multiple deployment targets from the same "
                + "source code. Switch between AWS Lambda, Google Cloud Run, standalone "
                + "JVM, or Servlet WAR by changing a single Maven profile. No code "
                + "changes required."))
            .child(sectionQuickReference())
            .child(sectionProfileOverview())
            .child(sectionJvmServer())
            .child(sectionLambda())
            .child(sectionCloudRun())
            .child(sectionWar())
            .child(sectionSameCode())
            .build();
    }

    private static ReactElement sectionQuickReference() {
        return Section.create().className("doc-section")
            .child(h2("Quick Reference"))
            .child(p("If you generated your project from the Installation page, it includes "
                + "a run.sh script and all three profiles pre-configured:"))
            .child(CodeBlock.create(
                """
                ./run.sh                  # JVM standalone (default, port 8080)
                ./run.sh cloudrun         # TeaVM/Node.js (no Docker needed)
                ./run.sh lambda           # TeaVM/Node.js via SAM (needs Docker)
                ./run.sh jvm-server 3000  # JVM on custom port""",
                "bash"))
            .child(p("Or build manually with Maven profiles:"))
            .child(CodeBlock.create(
                """
                mvn clean package                 # JVM standalone (default)
                mvn clean package -P lambda       # AWS Lambda (TeaVM/Node.js)
                mvn clean package -P cloudrun     # Cloud Run (TeaVM/Node.js)
                mvn clean package -P jvm-war      # Servlet WAR""",
                "bash"))
            .build();
    }

    private static ReactElement sectionProfileOverview() {
        return Section.create().className("doc-section")
            .child(h2("Maven Profiles"))
            .child(p("Each deployment target is controlled by a Maven profile. "
                + "The profile determines the compilation strategy, output format, "
                + "and which platform adapter is bundled."))
            .child(El.table("api-table",
                thead(tr(
                    th("Profile"),
                    th("Compilation"),
                    th("Output"),
                    th("Adapter")
                )),
                tbody(
                    tr(td(code("jvm-server")), td(text("javac \u2192 bytecode")), td(text("Uber JAR")), td(text("JDK HttpServer"))),
                    tr(td(code("lambda")), td(text("TeaVM \u2192 JavaScript")), td(code("target/lambda/")), td(text("AWS Lambda (Node.js 22)"))),
                    tr(td(code("cloudrun")), td(text("TeaVM \u2192 JavaScript")), td(code("target/cloudrun/")), td(text("Cloud Run (Node.js 22)"))),
                    tr(td(code("jvm-war")), td(text("javac \u2192 bytecode")), td(text("WAR file")), td(text("Servlet container")))
                )
            ))
            .child(Callout.note("Default profile",
                p("The default profile (no -P flag) is jvm-server, which builds "
                    + "a standalone JVM uber JAR. This is the simplest option for "
                    + "local development.")))
            .build();
    }

    private static ReactElement sectionJvmServer() {
        return Section.create().className("doc-section")
            .child(h2("JVM Standalone Server"))
            .child(p("The default jvm-server profile compiles with javac and packages an uber JAR "
                + "using maven-shade-plugin. The JAR includes a zero-dependency HTTP "
                + "server based on the JDK built-in com.sun.net.httpserver. This is "
                + "ideal for local development and simple deployments."))
            .child(h3("Build and Run"))
            .child(CodeBlock.create(
                """
                mvn clean package
                java -jar target/my-app-1.0.0.jar""",
                "bash"))
            .child(h3("Custom Port"))
            .child(CodeBlock.create(
                "PORT=3000 java -jar target/my-app-1.0.0.jar",
                "bash"))
            .child(h3("Docker Deployment"))
            .child(p("You can also run the JVM server in a container:"))
            .child(CodeBlock.create(
                """
                FROM eclipse-temurin:21-jre-alpine
                WORKDIR /app
                COPY target/my-app-*.jar app.jar
                EXPOSE 8080
                CMD ["java", "-jar", "app.jar"]""",
                "bash"))
            .child(Callout.note("Port configuration",
                p("The standalone server reads the PORT environment variable. "
                    + "It defaults to 8080 if not set. This is the same variable "
                    + "used by Cloud Run, so the same JAR can run locally or in "
                    + "a container.")))
            .build();
    }

    private static ReactElement sectionLambda() {
        return Section.create().className("doc-section")
            .child(h2("AWS Lambda"))
            .child(p("The lambda profile compiles your Java code to JavaScript via TeaVM "
                + "and packages it for AWS Lambda with Node.js 22 runtime. Cold starts "
                + "are typically around 100ms since there is no JVM startup overhead."))
            .child(h3("Build"))
            .child(CodeBlock.create(
                "mvn clean package -P lambda",
                "bash"))
            .child(p("This produces a target/lambda/ directory containing index.js "
                + "(handler), teavm-app.js (compiled app), package.json, and node_modules/."))
            .child(h3("Local Testing"))
            .child(CodeBlock.create(
                """
                # With SAM CLI (requires Docker)
                sam local start-api
                curl http://localhost:3000/hello""",
                "bash"))
            .child(h3("SAM Template"))
            .child(p("The generated project includes a template.yaml:"))
            .child(CodeBlock.create(
                """
                AWSTemplateFormatVersion: '2010-09-09'
                Transform: AWS::Serverless-2016-10-31

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
                      Events:
                        RootApi:
                          Type: Api
                          Properties:
                            Path: /
                            Method: ANY
                        ProxyApi:
                          Type: Api
                          Properties:
                            Path: /{proxy+}
                            Method: ANY""",
                "yaml"))
            .child(h3("Deploy to AWS"))
            .child(CodeBlock.create(
                "sam deploy --guided",
                "bash"))
            .child(Callout.note("Docker-free local testing",
                p("If you don't have Docker, use the cloudrun profile for "
                    + "local development instead. It produces the same TeaVM-compiled "
                    + "code as a Node.js HTTP server that you can run directly with "
                    + "node target/cloudrun/server.js.")))
            .build();
    }

    private static ReactElement sectionCloudRun() {
        return Section.create().className("doc-section")
            .child(h2("Google Cloud Run"))
            .child(p("The cloudrun profile compiles to JavaScript via TeaVM and packages it "
                + "with a Node.js HTTP server. The output runs on any platform that "
                + "supports Node.js 22 \u2014 Cloud Run, a Docker container, or directly "
                + "on your machine."))
            .child(h3("Build"))
            .child(CodeBlock.create(
                "mvn clean package -P cloudrun",
                "bash"))
            .child(h3("Run Locally (No Docker)"))
            .child(p("The simplest way to test your TeaVM-compiled code:"))
            .child(CodeBlock.create(
                """
                node target/cloudrun/server.js
                # Server starts on http://localhost:8080""",
                "bash"))
            .child(h3("Run with Docker"))
            .child(p("The generated project includes a Dockerfile:"))
            .child(CodeBlock.create(
                """
                docker build -t my-app .
                docker run -p 8080:8080 my-app""",
                "bash"))
            .child(h3("Dockerfile"))
            .child(CodeBlock.create(
                """
                FROM maven:3.9-eclipse-temurin-21 AS build
                WORKDIR /app
                COPY pom.xml .
                COPY src src
                COPY docker docker
                RUN mvn clean package -P cloudrun -q

                FROM node:22-slim
                WORKDIR /app
                COPY --from=build /app/target/cloudrun/ .
                RUN npm install --production
                EXPOSE 8080
                CMD ["node", "server.js"]""",
                "bash"))
            .child(h3("Deploy to Cloud Run"))
            .child(CodeBlock.create(
                """
                # Build and push the image
                docker build -t gcr.io/PROJECT_ID/my-app .
                docker push gcr.io/PROJECT_ID/my-app

                # Deploy
                gcloud run deploy my-app \\
                    --image gcr.io/PROJECT_ID/my-app \\
                    --platform managed \\
                    --region us-central1 \\
                    --allow-unauthenticated""",
                "bash"))
            .build();
    }

    private static ReactElement sectionWar() {
        return Section.create().className("doc-section")
            .child(h2("Servlet WAR"))
            .child(p("The jvm-war profile produces a standard WAR file that deploys to "
                + "any Jakarta Servlet 6.0 container, including Tomcat 11, TomEE 10, "
                + "and Jetty 12. The WAR adapter registers your router as a servlet "
                + "automatically."))
            .child(h3("Build"))
            .child(CodeBlock.create(
                "mvn clean package -P jvm-war",
                "bash"))
            .child(h3("Deploy"))
            .child(CodeBlock.create(
                """
                # Copy to Tomcat webapps
                cp target/my-app-1.0.0.war $CATALINA_HOME/webapps/my-app.war

                # Or run with Docker
                docker run -p 8080:8080 \\
                    -v $(pwd)/target/my-app-1.0.0.war:/usr/local/tomcat/webapps/ROOT.war \\
                    tomcat:11""",
                "bash"))
            .child(Callout.pitfall("Platform.start() not used",
                p("In WAR deployments, the servlet container manages the lifecycle. "
                    + "Do not call Platform.start() in your Main class. The WAR adapter "
                    + "registers the router via a ServletContainerInitializer "
                    + "automatically.")))
            .build();
    }

    private static ReactElement sectionSameCode() {
        return Section.create().className("doc-section")
            .child(h2("Write Once, Run Anywhere"))
            .child(p("The key benefit of teavm-lambda is that your application code "
                + "never changes between deployment targets. The same resource classes, "
                + "middleware, and business logic compile to JavaScript via TeaVM or "
                + "to JVM bytecode. Only the Maven profile changes."))
            .child(CodeBlock.create(
                """
                // This exact code deploys to ALL targets.
                // Only the Maven profile changes.

                @Path("/hello")
                @Component
                @Singleton
                public class HelloResource {

                    @GET
                    public Response hello() {
                        return Response.ok("{\\"message\\":\\"Hello!\\"}")
                                .header("Content-Type", "application/json");
                    }
                }""",
                "java"))
            .child(CodeBlock.create(
                """
                # Same source, all targets:
                mvn clean package                 # JVM standalone (default)
                mvn clean package -P lambda       # AWS Lambda (TeaVM/Node.js)
                mvn clean package -P cloudrun     # Cloud Run (TeaVM/Node.js)
                mvn clean package -P jvm-war      # Servlet WAR""",
                "bash"))
            .child(Callout.note("Platform-neutral code",
                p("Your application code depends only on platform-neutral API modules. "
                    + "The correct platform implementation (Node.js or JVM) is discovered "
                    + "at runtime via ServiceLoader. You never write platform-specific code.")))
            .build();
    }
}
