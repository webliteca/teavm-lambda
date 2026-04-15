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

public class DeploymentPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Deployment"))
            .child(p("teavm-lambda compiles to multiple deployment targets from the same "
                + "source code. Switch between AWS Lambda, Google Cloud Run, standalone "
                + "JVM, or Servlet WAR by changing a single Maven profile. No code "
                + "changes required."))
            .child(sectionProfileOverview())
            .child(sectionLambda())
            .child(sectionCloudRun())
            .child(sectionJvm())
            .child(sectionWar())
            .child(sectionSameCode())
            .build();
    }

    private static ReactElement sectionProfileOverview() {
        return Section.create().className("doc-section")
            .child(h2("Maven Profiles"))
            .child(p("Each deployment target is controlled by a Maven profile. "
                + "The profile determines the compilation strategy, output format, "
                + "and which platform adapter is bundled."))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Profile"),
                            El.classedText("th", "", "Compilation"),
                            El.classedText("th", "", "Output"),
                            El.classedText("th", "", "Adapter")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("teavm")),
                            El.classedText("td", "", "TeaVM -> JavaScript"),
                            El.classed("td", "", code("target/lambda/")),
                            El.classedText("td", "", "AWS Lambda (Node.js)")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("cloudrun")),
                            El.classedText("td", "", "TeaVM -> JavaScript"),
                            El.classed("td", "", code("target/cloudrun/")),
                            El.classedText("td", "", "Google Cloud Run (Node.js)")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("jvm")),
                            El.classedText("td", "", "javac -> bytecode"),
                            El.classedText("td", "", "Uber JAR"),
                            El.classedText("td", "", "Standalone JDK HttpServer")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("jvm-war")),
                            El.classedText("td", "", "javac -> bytecode"),
                            El.classedText("td", "", "WAR file"),
                            El.classedText("td", "", "Servlet container (Tomcat, TomEE, Jetty)")
                        )
                    )
                )
            ))
            .child(Callout.note("Default profile",
                p("The default profile (no -P flag) is teavm, which compiles "
                    + "to JavaScript for AWS Lambda deployment.")))
            .build();
    }

    private static ReactElement sectionLambda() {
        String buildCommand = """
# Build for AWS Lambda (TeaVM/Node.js)
mvn clean package

# Or explicitly specify the profile
mvn clean package -P teavm""";

        String deployCommand = """
# Deploy with SAM CLI
sam deploy --guided

# Or test locally first
sam local start-api --template template.yaml""";

        String templateSnippet = """
# template.yaml (SAM)
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31

Resources:
  ApiFunction:
    Type: AWS::Serverless::Function
    Properties:
      Handler: index.handler
      Runtime: nodejs22.x
      CodeUri: target/lambda/
      MemorySize: 256
      Timeout: 30
      Events:
        Api:
          Type: HttpApi
          Properties:
            Path: /{proxy+}
            Method: ANY""";

        return Section.create().className("doc-section")
            .child(h2("AWS Lambda"))
            .child(p("The default TeaVM profile compiles your Java code to JavaScript "
                + "and packages it for AWS Lambda with Node.js 22.x runtime. Cold starts "
                + "are typically around 100ms since there is no JVM startup overhead."))
            .child(h3("Build"))
            .child(CodeBlock.create(buildCommand, "bash"))
            .child(h3("Deploy"))
            .child(CodeBlock.create(deployCommand, "bash"))
            .child(h3("SAM Template"))
            .child(CodeBlock.create(templateSnippet, "yaml"))
            .build();
    }

    private static ReactElement sectionCloudRun() {
        String buildCommand = """
# Build for Google Cloud Run (TeaVM/Node.js)
mvn clean package -P cloudrun""";

        String dockerfile = """
FROM node:22-slim
WORKDIR /app
COPY target/cloudrun/ .
RUN npm install
EXPOSE 8080
CMD ["node", "index.js"]""";

        String deployCommand = """
# Build the Docker image
docker build -t my-api .

# Push to Google Container Registry
docker tag my-api gcr.io/my-project/my-api
docker push gcr.io/my-project/my-api

# Deploy to Cloud Run
gcloud run deploy my-api \\
    --image gcr.io/my-project/my-api \\
    --platform managed \\
    --region us-central1 \\
    --allow-unauthenticated""";

        return Section.create().className("doc-section")
            .child(h2("Google Cloud Run"))
            .child(p("The cloudrun profile compiles to JavaScript and packages it for "
                + "Cloud Run. The output includes a Node.js HTTP server that listens "
                + "on the PORT environment variable (default 8080)."))
            .child(h3("Build"))
            .child(CodeBlock.create(buildCommand, "bash"))
            .child(h3("Dockerfile"))
            .child(CodeBlock.create(dockerfile, "dockerfile"))
            .child(h3("Deploy"))
            .child(CodeBlock.create(deployCommand, "bash"))
            .build();
    }

    private static ReactElement sectionJvm() {
        String buildCommand = """
# Build a standalone JVM uber JAR
mvn clean package -P jvm""";

        String runCommand = """
# Run the standalone server (default port 8080)
java -jar target/my-app-1.0.0-SNAPSHOT.jar

# Override the port
PORT=3000 java -jar target/my-app-1.0.0-SNAPSHOT.jar""";

        return Section.create().className("doc-section")
            .child(h2("JVM Standalone"))
            .child(p("The jvm profile compiles with javac and packages an uber JAR "
                + "using maven-shade-plugin. The JAR includes a zero-dependency HTTP "
                + "server based on the JDK built-in com.sun.net.httpserver. This is "
                + "ideal for local development and simple deployments."))
            .child(h3("Build"))
            .child(CodeBlock.create(buildCommand, "bash"))
            .child(h3("Run"))
            .child(CodeBlock.create(runCommand, "bash"))
            .child(Callout.note("Port configuration",
                p("The standalone server reads the PORT environment variable. "
                    + "It defaults to 8080 if not set. This is the same variable "
                    + "used by Cloud Run, so the same JAR can run locally or in "
                    + "a container.")))
            .build();
    }

    private static ReactElement sectionWar() {
        String buildCommand = """
# Build a WAR file for Servlet containers
mvn clean package -P jvm-war""";

        String deployCommand = """
# Copy to Tomcat webapps
cp target/my-app-1.0.0-SNAPSHOT.war $CATALINA_HOME/webapps/my-app.war

# Or run with Docker
docker run -p 8080:8080 \\
    -v $(pwd)/target/my-app-1.0.0-SNAPSHOT.war:/usr/local/tomcat/webapps/ROOT.war \\
    tomcat:11""";

        return Section.create().className("doc-section")
            .child(h2("Servlet WAR"))
            .child(p("The jvm-war profile produces a standard WAR file that deploys to "
                + "any Jakarta Servlet 6.0 container, including Tomcat 11, TomEE 10, "
                + "and Jetty 12. The WAR adapter registers your router as a servlet "
                + "automatically."))
            .child(h3("Build"))
            .child(CodeBlock.create(buildCommand, "bash"))
            .child(h3("Deploy"))
            .child(CodeBlock.create(deployCommand, "bash"))
            .child(Callout.pitfall("Platform.start() not used",
                p("In WAR deployments, the servlet container manages the lifecycle. "
                    + "Do not call Platform.start() in your Main class. The WAR adapter "
                    + "registers the router via a ServletContainerInitializer "
                    + "automatically.")))
            .build();
    }

    private static ReactElement sectionSameCode() {
        String javaCode = """
// This exact code deploys to ALL four targets.
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
}""";

        String buildCommands = """
# Same source, four targets:
mvn clean package              # AWS Lambda (TeaVM/JS)
mvn clean package -P cloudrun  # Google Cloud Run (TeaVM/JS)
mvn clean package -P jvm       # Standalone JVM server
mvn clean package -P jvm-war   # Servlet WAR""";

        return Section.create().className("doc-section")
            .child(h2("Write Once, Run Anywhere"))
            .child(p("The key benefit of teavm-lambda is that your application code "
                + "never changes between deployment targets. The same resource classes, "
                + "middleware, and business logic compile to JavaScript via TeaVM or "
                + "to JVM bytecode. Only the Maven profile changes."))
            .child(CodeBlock.create(javaCode, "java"))
            .child(CodeBlock.create(buildCommands, "bash"))
            .child(Callout.note("Same code, all targets",
                p("Your application code depends only on platform-neutral API modules. "
                    + "The correct platform implementation (Node.js or JVM) is discovered "
                    + "at runtime via ServiceLoader. You never write platform-specific code.")))
            .build();
    }
}
