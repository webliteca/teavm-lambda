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

public class InstallationPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Installation"))
            .child(p("This guide covers everything you need to set up a teavm-lambda project "
                + "from scratch, including prerequisites, Maven configuration, and optional "
                + "tooling for cloud deployments."))

            // Prerequisites
            .child(Section.create().className("doc-section")
                .child(h2("Prerequisites"))
                .child(p("The following tools are required to build and run a teavm-lambda project:"))
                .child(El.table("prerequisites-table",
                    thead(
                        tr(th("Tool"), th("Version"), th("Purpose"))),
                    tbody(
                        tr(td("JDK"), td("21+"), td("Java compiler and runtime")),
                        tr(td("Maven"), td("3.9+"), td("Build tool and dependency management")),
                        tr(td("Docker"), td("Latest"), td("Required for integration tests and local database")),
                        tr(td("Docker Compose"), td("Latest"), td("Orchestrates multi-container test environments"))))))

            // Maven dependencies
            .child(Section.create().className("doc-section")
                .child(h2("Maven Dependencies"))
                .child(p("Add the teavm-lambda core dependency and the appropriate adapter "
                    + "to your pom.xml. The example below uses the standalone JVM HTTP server "
                    + "adapter, which is ideal for local development."))
                .child(CodeBlock.create(
                    """
                    <properties>
                        <maven.compiler.source>21</maven.compiler.source>
                        <maven.compiler.target>21</maven.compiler.target>
                        <teavm-lambda.version>0.1.0-SNAPSHOT</teavm-lambda.version>
                    </properties>

                    <dependencies>
                        <dependency>
                            <groupId>ca.weblite</groupId>
                            <artifactId>teavm-lambda-core</artifactId>
                            <version>${teavm-lambda.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>ca.weblite</groupId>
                            <artifactId>teavm-lambda-adapter-httpserver</artifactId>
                            <version>${teavm-lambda.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>ca.weblite</groupId>
                            <artifactId>teavm-lambda-core-jvm</artifactId>
                            <version>${teavm-lambda.version}</version>
                        </dependency>
                    </dependencies>""",
                    "markup"))
                .child(Callout.note("Platform Modules",
                    p("For JVM deployment, use teavm-lambda-core-jvm. For TeaVM/Node.js "
                        + "deployment (AWS Lambda, Cloud Run), use teavm-lambda-core-js instead. "
                        + "Swap the adapter module to match your target platform."))))

            // Annotation processor
            .child(Section.create().className("doc-section")
                .child(h2("Annotation Processor"))
                .child(p("The teavm-lambda annotation processor must be configured in the "
                    + "Maven compiler plugin. It generates the GeneratedRouter and "
                    + "GeneratedContainer classes at compile time."))
                .child(CodeBlock.create(
                    """
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.13.0</version>
                                <configuration>
                                    <annotationProcessorPaths>
                                        <path>
                                            <groupId>ca.weblite</groupId>
                                            <artifactId>teavm-lambda-processor</artifactId>
                                            <version>${teavm-lambda.version}</version>
                                        </path>
                                    </annotationProcessorPaths>
                                </configuration>
                            </plugin>
                        </plugins>
                    </build>""",
                    "markup"))
                .child(p("For packaging as a standalone JAR, also add the maven-shade-plugin "
                    + "with a ManifestResourceTransformer pointing to your Main class.")))

            // Optional tools
            .child(Section.create().className("doc-section")
                .child(h2("Optional Tools"))
                .child(p("Depending on your deployment target, you may need additional CLI tools:"))
                .child(El.table("optional-tools-table",
                    thead(
                        tr(th("Tool"), th("Version"), th("When Needed"))),
                    tbody(
                        tr(td("AWS SAM CLI"), td("Latest"), td("Local testing and deployment of AWS Lambda functions")),
                        tr(td("Node.js"), td("22+"), td("Running TeaVM-compiled JavaScript output locally")),
                        tr(td("Google Cloud CLI"), td("Latest"), td("Deploying to Google Cloud Run"))))))

            // Project initializer
            .child(Section.create().className("doc-section")
                .child(h2("Generate a Project"))
                .child(p("Use the project initializer below to generate a ready-to-run "
                    + "starter project with your preferred configuration. The generated "
                    + "zip contains a complete Maven project with a sample REST endpoint."))
                .child(ProjectInitializer.create()))

            .build();
    }
}
