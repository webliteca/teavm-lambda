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
                .child(p("The generated project includes Maven profiles for multiple deployment "
                    + "targets, plus a SAM template, Dockerfile, and a run.sh convenience "
                    + "script. Your application code is the same for all targets."))
                .child(CodeBlock.create(
                    """
                    ./run.sh                  # JVM standalone (default, port 8080)
                    ./run.sh cloudrun         # TeaVM/Node.js (no Docker needed)
                    ./run.sh lambda           # TeaVM/Node.js via SAM (needs Docker)
                    ./run.sh jvm-server 3000  # JVM on custom port""",
                    "bash"))
                .child(p("Or run the build and server commands manually:"))

                // JVM Standalone
                .child(h3("JVM Standalone Server (default)"))
                .child(p("The simplest way to run locally. Build an uber JAR with the built-in "
                    + "HTTP server \u2014 no Docker or cloud tooling needed."))
                .child(CodeBlock.create(
                    """
                    mvn clean package
                    java -jar target/my-app-1.0.0.jar""",
                    "bash"))
                .child(p("The server starts on port 8080 by default. Set the PORT "
                    + "environment variable to change it:"))
                .child(CodeBlock.create(
                    "PORT=3000 java -jar target/my-app-1.0.0.jar",
                    "bash"))

                // AWS Lambda
                .child(h3("AWS Lambda (TeaVM / Node.js)"))
                .child(p("Build with the lambda profile to compile your Java to JavaScript via TeaVM. "
                    + "Use AWS SAM to run locally, or use the cloudrun profile for Docker-free "
                    + "local testing (see below)."))
                .child(CodeBlock.create(
                    """
                    # Build the Lambda package (TeaVM compiles Java to JS)
                    mvn clean package -P lambda

                    # Start the local Lambda API (requires SAM CLI and Docker)
                    sam local start-api

                    # Test (SAM defaults to port 3000)
                    curl http://localhost:3000/hello""",
                    "bash"))
                .child(Callout.note("SAM Prerequisites",
                    p("sam local start-api requires the AWS SAM CLI and Docker. "
                        + "If you don't have Docker, use the cloudrun profile below for "
                        + "Docker-free local testing \u2014 the same TeaVM-compiled code runs on "
                        + "both targets.")))

                // Cloud Run / Node.js local
                .child(h3("Cloud Run / Node.js (TeaVM)"))
                .child(p("Build with the cloudrun profile and run directly with Node.js \u2014 no "
                    + "Docker required. This is the fastest way to test your TeaVM-compiled code locally."))
                .child(CodeBlock.create(
                    """
                    # Build (TeaVM compiles Java to JS)
                    mvn clean package -P cloudrun

                    # Run directly with Node.js (no Docker needed)
                    node target/cloudrun/server.js

                    # Or use Docker to mirror the production environment
                    docker build -t my-app .
                    docker run -p 8080:8080 my-app

                    # Test
                    curl http://localhost:8080/hello""",
                    "bash"))
                .child(Callout.note("Node.js Required",
                    p("Running without Docker requires Node.js 22 installed locally. "
                        + "The npm install step runs automatically during the Maven build.")))

                // Test section
                .child(h3("Test Your Endpoint"))
                .child(p("Test with curl (adjust the port for SAM \u2014 it defaults to 3000):"))
                .child(CodeBlock.create(
                    """
                    curl http://localhost:8080/hello
                    # {"message":"Hello, World!"}

                    curl http://localhost:8080/hello/Alice
                    # {"message":"Hello, Alice!"}""",
                    "bash")))

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
