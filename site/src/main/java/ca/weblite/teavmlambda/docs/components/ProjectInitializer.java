package ca.weblite.teavmlambda.docs.components;

import ca.weblite.teavmreact.core.React;
import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.hooks.Hooks;
import ca.weblite.teavmreact.html.DomBuilder.*;
import ca.weblite.teavmlambda.docs.El;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

public class ProjectInitializer {

    @JSBody(params = {"groupId", "artifactId", "packageName", "deployTarget"},
            script =
        "var zip = new JSZip();" +
        "var pkgPath = packageName.replace(/\\./g, '/');" +

        // pom.xml
        "var pom = '<?xml version=\"1.0\" encoding=\"UTF-8\"?>\\n'" +
        " + '<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\\n'" +
        " + '         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\\n'" +
        " + '         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0\\n'" +
        " + '           http://maven.apache.org/xsd/maven-4.0.0.xsd\">\\n'" +
        " + '    <modelVersion>4.0.0</modelVersion>\\n\\n'" +
        " + '    <groupId>' + groupId + '</groupId>\\n'" +
        " + '    <artifactId>' + artifactId + '</artifactId>\\n'" +
        " + '    <version>1.0.0-SNAPSHOT</version>\\n\\n'" +
        " + '    <properties>\\n'" +
        " + '        <maven.compiler.source>21</maven.compiler.source>\\n'" +
        " + '        <maven.compiler.target>21</maven.compiler.target>\\n'" +
        " + '        <teavm.version>0.13.1</teavm.version>\\n'" +
        " + '        <teavm-lambda.version>0.1.6</teavm-lambda.version>\\n'" +
        " + '    </properties>\\n\\n'" +
        " + '    <dependencies>\\n'" +
        " + '        <dependency>\\n'" +
        " + '            <groupId>ca.weblite</groupId>\\n'" +
        " + '            <artifactId>teavm-lambda-core</artifactId>\\n'" +
        " + '            <version>${teavm-lambda.version}</version>\\n'" +
        " + '        </dependency>\\n'" +
        " + '        <dependency>\\n'" +
        " + '            <groupId>ca.weblite</groupId>\\n'" +
        " + '            <artifactId>teavm-lambda-adapter-httpserver</artifactId>\\n'" +
        " + '            <version>${teavm-lambda.version}</version>\\n'" +
        " + '        </dependency>\\n'" +
        " + '        <dependency>\\n'" +
        " + '            <groupId>ca.weblite</groupId>\\n'" +
        " + '            <artifactId>teavm-lambda-core-jvm</artifactId>\\n'" +
        " + '            <version>${teavm-lambda.version}</version>\\n'" +
        " + '        </dependency>\\n'" +
        " + '    </dependencies>\\n\\n'" +
        " + '    <build>\\n'" +
        " + '        <plugins>\\n'" +
        " + '            <plugin>\\n'" +
        " + '                <groupId>org.apache.maven.plugins</groupId>\\n'" +
        " + '                <artifactId>maven-compiler-plugin</artifactId>\\n'" +
        " + '                <version>3.13.0</version>\\n'" +
        " + '                <configuration>\\n'" +
        " + '                    <annotationProcessorPaths>\\n'" +
        " + '                        <path>\\n'" +
        " + '                            <groupId>ca.weblite</groupId>\\n'" +
        " + '                            <artifactId>teavm-lambda-processor</artifactId>\\n'" +
        " + '                            <version>${teavm-lambda.version}</version>\\n'" +
        " + '                        </path>\\n'" +
        " + '                    </annotationProcessorPaths>\\n'" +
        " + '                </configuration>\\n'" +
        " + '            </plugin>\\n'" +
        " + '            <plugin>\\n'" +
        " + '                <groupId>org.apache.maven.plugins</groupId>\\n'" +
        " + '                <artifactId>maven-shade-plugin</artifactId>\\n'" +
        " + '                <version>3.5.3</version>\\n'" +
        " + '                <executions>\\n'" +
        " + '                    <execution>\\n'" +
        " + '                        <phase>package</phase>\\n'" +
        " + '                        <goals><goal>shade</goal></goals>\\n'" +
        " + '                        <configuration>\\n'" +
        " + '                            <transformers>\\n'" +
        " + '                                <transformer implementation=\"org.apache.maven.plugins.shade.resource.ManifestResourceTransformer\">\\n'" +
        " + '                                    <mainClass>' + packageName + '.Main</mainClass>\\n'" +
        " + '                                </transformer>\\n'" +
        " + '                                <transformer implementation=\"org.apache.maven.plugins.shade.resource.ServicesResourceTransformer\"/>\\n'" +
        " + '                            </transformers>\\n'" +
        " + '                        </configuration>\\n'" +
        " + '                    </execution>\\n'" +
        " + '                </executions>\\n'" +
        " + '            </plugin>\\n'" +
        " + '        </plugins>\\n'" +
        " + '    </build>\\n'" +
        " + '</project>\\n';" +

        // HelloResource.java
        "var resource = 'package ' + packageName + ';\\n\\n'" +
        " + 'import ca.weblite.teavmlambda.api.Response;\\n'" +
        " + 'import ca.weblite.teavmlambda.api.annotation.*;\\n\\n'" +
        " + '@Path(\"/hello\")\\n'" +
        " + '@Component\\n'" +
        " + '@Singleton\\n'" +
        " + 'public class HelloResource {\\n\\n'" +
        " + '    @GET\\n'" +
        " + '    public Response hello() {\\n'" +
        " + '        return Response.ok(\"{\\\\\"message\\\\\":\\\\\"Hello, World!\\\\\"}\")\\n'" +
        " + '                .header(\"Content-Type\", \"application/json\");\\n'" +
        " + '    }\\n\\n'" +
        " + '    @GET\\n'" +
        " + '    @Path(\"/{name}\")\\n'" +
        " + '    public Response helloName(@PathParam(\"name\") String name) {\\n'" +
        " + '        return Response.ok(\"{\\\\\"message\\\\\":\\\\\"Hello, \" + name + \"!\\\\\"}\")\\n'" +
        " + '                .header(\"Content-Type\", \"application/json\");\\n'" +
        " + '    }\\n'" +
        " + '}\\n';" +

        // Main.java
        "var main = 'package ' + packageName + ';\\n\\n'" +
        " + 'import ca.weblite.teavmlambda.api.Platform;\\n'" +
        " + 'import ca.weblite.teavmlambda.generated.GeneratedContainer;\\n'" +
        " + 'import ca.weblite.teavmlambda.generated.GeneratedRouter;\\n\\n'" +
        " + 'public class Main {\\n\\n'" +
        " + '    public static void main(String[] args) throws Exception {\\n'" +
        " + '        var container = new GeneratedContainer();\\n'" +
        " + '        var router = new GeneratedRouter(container);\\n'" +
        " + '        Platform.start(router);\\n'" +
        " + '    }\\n'" +
        " + '}\\n';" +

        // Add files to zip
        "var root = artifactId + '/';" +
        "zip.file(root + 'pom.xml', pom);" +
        "zip.file(root + 'src/main/java/' + pkgPath + '/HelloResource.java', resource);" +
        "zip.file(root + 'src/main/java/' + pkgPath + '/Main.java', main);" +

        // Generate and trigger download
        "zip.generateAsync({type: 'blob', platform: 'UNIX'}).then(function(blob) {" +
        "  var link = document.createElement('a');" +
        "  link.href = URL.createObjectURL(blob);" +
        "  link.download = artifactId + '.zip';" +
        "  document.body.appendChild(link);" +
        "  link.click();" +
        "  document.body.removeChild(link);" +
        "  URL.revokeObjectURL(link.href);" +
        "});"
    )
    private static native void generateProject(
            String groupId, String artifactId, String packageName, String deployTarget);

    public static ReactElement create() {
        return component(ProjectInitializer::render, "ProjectInitializer");
    }

    private static ReactElement render(JSObject props) {
        var groupId = Hooks.useState("com.example");
        var artifactId = Hooks.useState("my-teavm-lambda-app");
        var packageName = Hooks.useState("com.example");
        var deployTarget = Hooks.useState("jvm-server");

        return Div.create().className("project-initializer")

            .child(Div.create().className("pi-header")
                .child(H2.create().text("Create a new project"))
                .child(P.create().text(
                    "Configure your project below and click Generate to download "
                    + "a ready-to-run teavm-lambda starter project.")))

            .child(Div.create().className("pi-form")

                .child(Div.create().className("pi-field")
                    .child(Label.create().text("Group").prop("htmlFor", "pi-group"))
                    .child(Input.text()
                        .id("pi-group")
                        .value(groupId.getString())
                        .placeholder("com.example")
                        .onChange(e -> groupId.setString(e.getTarget().getValue()))))

                .child(Div.create().className("pi-field")
                    .child(Label.create().text("Artifact").prop("htmlFor", "pi-artifact"))
                    .child(Input.text()
                        .id("pi-artifact")
                        .value(artifactId.getString())
                        .placeholder("my-teavm-lambda-app")
                        .onChange(e -> artifactId.setString(e.getTarget().getValue()))))

                .child(Div.create().className("pi-field")
                    .child(Label.create().text("Package Name").prop("htmlFor", "pi-package"))
                    .child(Input.text()
                        .id("pi-package")
                        .value(packageName.getString())
                        .placeholder("com.example")
                        .onChange(e -> packageName.setString(e.getTarget().getValue()))))

                .child(Div.create().className("pi-field")
                    .child(Label.create().text("Deploy Target").prop("htmlFor", "pi-target"))
                    .child(Select.create()
                        .id("pi-target")
                        .value(deployTarget.getString())
                        .onChange(e -> deployTarget.setString(e.getTarget().getValue()))
                        .child(option("jvm-server", "JVM Standalone Server"))
                        .child(option("lambda", "AWS Lambda"))
                        .child(option("cloudrun", "Google Cloud Run")))))

            .child(Div.create().className("pi-actions")
                .child(Button.create()
                    .className("btn btn-primary pi-generate-btn")
                    .text("Generate Project")
                    .onClick(e -> generateProject(
                        groupId.getString(),
                        artifactId.getString(),
                        packageName.getString(),
                        deployTarget.getString()))))

            .child(Div.create().className("pi-summary")
                .child(P.create().text(
                    artifactId.getString() + ".zip  |  "
                    + packageName.getString() + "  |  " + deployTarget.getString())))

            .build();
    }

    private static ReactElement option(String value, String label) {
        JSObject props = React.createObject();
        React.setProperty(props, "value", value);
        return React.createElementWithText("option", props, label);
    }
}
