package ca.weblite.teavmlambda.docs.pages.learn;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder.Div;
import ca.weblite.teavmreact.html.DomBuilder.Section;
import org.teavm.jso.JSObject;
import static ca.weblite.teavmreact.html.Html.*;
import ca.weblite.teavmlambda.docs.El;
import ca.weblite.teavmlambda.docs.components.CodeBlock;
import ca.weblite.teavmlambda.docs.components.CodeTabs;
import ca.weblite.teavmlambda.docs.components.Callout;
import ca.weblite.teavmlambda.docs.components.FeatureCard;
import ca.weblite.teavmlambda.docs.components.ProjectInitializer;

public class QuickStartPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Quick Start"))
            .child(p("Get a teavm-lambda project up and running in under five minutes. "
                + "This guide walks you through creating a minimal REST API, building it, "
                + "and running it locally on the JVM."))

            // Project structure
            .child(Section.create().className("doc-section")
                .child(h2("Project Structure"))
                .child(p("A minimal teavm-lambda project has the following layout:"))
                .child(CodeBlock.create(
                    """
                    my-app/
                    \u251c\u2500\u2500 pom.xml
                    \u2514\u2500\u2500 src/
                        \u2514\u2500\u2500 main/
                            \u2514\u2500\u2500 java/
                                \u2514\u2500\u2500 com/
                                    \u2514\u2500\u2500 example/
                                        \u251c\u2500\u2500 Main.java
                                        \u2514\u2500\u2500 HelloResource.java""",
                    "text"))
                .child(p("The annotation processor generates GeneratedRouter and "
                    + "GeneratedContainer automatically during compilation. You never "
                    + "write these files by hand.")))

            // HelloResource
            .child(Section.create().className("doc-section")
                .child(h2("Define a Resource"))
                .child(p("Create a resource class with JAX-RS-style annotations. "
                    + "The @Path annotation defines the base URL, and @GET, @POST, etc. "
                    + "map HTTP methods to handler methods."))
                .child(CodeTabs.create(
                    """
                    package com.example;

                    import ca.weblite.teavmlambda.api.Response;
                    import ca.weblite.teavmlambda.api.annotation.*;

                    @Path("/hello")
                    @Component
                    @Singleton
                    public class HelloResource {

                        @GET
                        public Response hello() {
                            return Response.ok("{\\"message\\":\\"Hello, World!\\"}")
                                    .header("Content-Type", "application/json");
                        }

                        @GET
                        @Path("/{name}")
                        public Response helloName(@PathParam("name") String name) {
                            return Response.ok("{\\"message\\":\\"Hello, " + name + "!\\"}")
                                    .header("Content-Type", "application/json");
                        }
                    }""",
                    """
                    app {
                        routes {
                            get("/hello") {
                                ok(json { "message" to "Hello, World!" })
                            }
                            get("/hello/{name}") {
                                val name = pathParam("name")
                                ok(json { "message" to "Hello, $name!" })
                            }
                        }
                    }""")))

            // Main class
            .child(Section.create().className("doc-section")
                .child(h2("Entry Point"))
                .child(p("The Main class wires together the generated container and router, "
                    + "then starts the platform adapter. Platform.start() automatically "
                    + "discovers the correct adapter (Lambda, Cloud Run, or standalone JVM) "
                    + "via ServiceLoader."))
                .child(CodeTabs.create(
                    """
                    package com.example;

                    import ca.weblite.teavmlambda.api.Platform;
                    import ca.weblite.teavmlambda.generated.GeneratedContainer;
                    import ca.weblite.teavmlambda.generated.GeneratedRouter;

                    public class Main {

                        public static void main(String[] args) throws Exception {
                            var container = new GeneratedContainer();
                            var router = new GeneratedRouter(container);
                            Platform.start(router);
                        }
                    }""",
                    """
                    import ca.weblite.teavmlambda.api.Platform
                    import ca.weblite.teavmlambda.generated.GeneratedContainer
                    import ca.weblite.teavmlambda.generated.GeneratedRouter

                    fun main() {
                        val container = GeneratedContainer()
                        val router = GeneratedRouter(container)
                        Platform.start(router)
                    }""")))

            // Building and running
            .child(Section.create().className("doc-section")
                .child(h2("Build and Run"))
                .child(p("teavm-lambda supports multiple deployment targets. The same application "
                    + "code works with all of them \u2014 only the Maven profile and adapter dependency "
                    + "change. Choose the target that fits your environment."))

                // JVM Standalone
                .child(h3("JVM Standalone Server"))
                .child(p("The simplest way to run locally. Build an uber JAR with the built-in "
                    + "HTTP server \u2014 no Docker or cloud tooling needed."))
                .child(CodeBlock.create(
                    """
                    mvn clean package -P jvm
                    java -jar target/my-app-1.0.0-SNAPSHOT.jar""",
                    "bash"))
                .child(p("The server starts on port 8080 by default. To use a different port, "
                    + "set the PORT environment variable:"))
                .child(CodeBlock.create(
                    "PORT=3000 java -jar target/my-app-1.0.0-SNAPSHOT.jar",
                    "bash"))

                // AWS Lambda
                .child(h3("AWS Lambda (Local with SAM)"))
                .child(p("The default profile compiles Java to JavaScript via TeaVM and "
                    + "produces a Node.js Lambda package. Use AWS SAM CLI to run it locally."))
                .child(p("First, create a template.yaml in your project root:"))
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
                .child(p("Then build and run:"))
                .child(CodeBlock.create(
                    """
                    # Build the Lambda package (TeaVM compiles Java to JS)
                    mvn clean package

                    # Start the local Lambda API (requires SAM CLI and Docker)
                    sam local start-api

                    # Test (SAM defaults to port 3000)
                    curl http://localhost:3000/hello""",
                    "bash"))
                .child(Callout.note("Prerequisites",
                    p("Local Lambda testing requires the AWS SAM CLI and Docker. "
                        + "Install SAM from https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html")))

                // Cloud Run
                .child(h3("Cloud Run (Local with Docker)"))
                .child(p("Build a Docker image and run it locally. This mirrors the exact "
                    + "environment your app will run in on Google Cloud Run."))
                .child(p("Create a Dockerfile in your project:"))
                .child(CodeBlock.create(
                    """
                    FROM maven:3.9-eclipse-temurin-21 AS build
                    WORKDIR /app
                    COPY pom.xml .
                    COPY src src
                    RUN mvn clean package -P jvm-server -q

                    FROM eclipse-temurin:21-jre-alpine
                    WORKDIR /app
                    COPY --from=build /app/target/my-app-*.jar app.jar
                    EXPOSE 8080
                    CMD ["java", "-jar", "app.jar"]""",
                    "bash"))
                .child(p("Build and run:"))
                .child(CodeBlock.create(
                    """
                    docker build -t my-app .
                    docker run -p 8080:8080 my-app

                    # Test
                    curl http://localhost:8080/hello""",
                    "bash"))

                // Test section (applies to all)
                .child(h3("Test Your Endpoint"))
                .child(p("Regardless of how you started the server, test it with curl:"))
                .child(CodeBlock.create(
                    """
                    curl http://localhost:8080/hello
                    # {"message":"Hello, World!"}

                    curl http://localhost:8080/hello/Alice
                    # {"message":"Hello, Alice!"}""",
                    "bash"))
                .child(Callout.note("Port Differences",
                    p("The JVM standalone and Cloud Run servers default to port 8080. "
                        + "SAM local defaults to port 3000. Adjust the curl URL accordingly."))))

            // Annotation processor note
            .child(Section.create().className("doc-section")
                .child(h2("How It Works"))
                .child(p("When you compile the project, the teavm-lambda annotation processor "
                    + "scans your source code for @Path, @Component, @Service, @Repository, "
                    + "and @Inject annotations. It generates two classes:"))
                .child(ul(
                    li("GeneratedRouter - maps HTTP methods and URL patterns to your handler methods"),
                    li("GeneratedContainer - provides compile-time dependency injection for all components")))
                .child(Callout.note("Generated Code",
                    p("The GeneratedRouter and GeneratedContainer classes are created by the "
                        + "annotation processor during compilation. You never need to write or "
                        + "edit them. They appear in target/generated-sources/annotations/."))))

            .build();
    }
}
