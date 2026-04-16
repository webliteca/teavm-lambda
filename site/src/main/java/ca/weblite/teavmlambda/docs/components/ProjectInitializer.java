package ca.weblite.teavmlambda.docs.components;

import ca.weblite.teavmreact.core.React;
import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.hooks.Hooks;
import ca.weblite.teavmreact.html.DomBuilder.*;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

public class ProjectInitializer {

    @JSBody(params = {"groupId", "artifactId", "packageName", "language"},
            script = "window.generateTeavmLambdaProject(groupId, artifactId, packageName, language);")
    private static native void generateProject(
            String groupId, String artifactId, String packageName, String language);

    public static ReactElement create() {
        return component(ProjectInitializer::render, "ProjectInitializer");
    }

    private static ReactElement render(JSObject props) {
        var groupId = Hooks.useState("com.example");
        var artifactId = Hooks.useState("my-teavm-lambda-app");
        var packageName = Hooks.useState("com.example");
        var language = Hooks.useState("java");

        return Div.create().className("project-initializer")

            .child(Div.create().className("pi-header")
                .child(H2.create().text("Create a new project"))
                .child(P.create().text(
                    "Configure your project and click Generate. The download includes "
                    + "Maven profiles for JVM standalone, AWS Lambda (TeaVM/Node.js), and "
                    + "Cloud Run (TeaVM/Node.js), plus a SAM template and Dockerfile.")))

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
                    .child(Label.create().text("Language").prop("htmlFor", "pi-lang"))
                    .child(Select.create()
                        .id("pi-lang")
                        .value(language.getString())
                        .onChange(e -> language.setString(e.getTarget().getValue()))
                        .child(option("java", "Java"))
                        .child(option("kotlin", "Kotlin")))))

            .child(Div.create().className("pi-actions")
                .child(Button.create()
                    .className("btn btn-primary pi-generate-btn")
                    .text("Generate Project")
                    .onClick(e -> generateProject(
                        groupId.getString(),
                        artifactId.getString(),
                        packageName.getString(),
                        language.getString()))))

            .child(Div.create().className("pi-summary")
                .child(P.create().text(
                    artifactId.getString() + ".zip  |  "
                    + language.getString() + "  |  "
                    + packageName.getString())))

            .build();
    }

    private static ReactElement option(String value, String label) {
        JSObject props = React.createObject();
        React.setProperty(props, "value", value);
        return React.createElementWithText("option", props, label);
    }
}
