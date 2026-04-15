package ca.weblite.teavmlambda.docs.pages;

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

public class HomePage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page home-page")

            // Hero section
            .child(Section.create().className("hero")
                .child(h1("teavm-lambda"))
                .child(El.p("hero-tagline",
                    "Build serverless HTTP APIs in Java, compiled to JavaScript via TeaVM. "
                    + "Deploy to AWS Lambda, Cloud Run, or JVM."))
                .child(Div.create().className("hero-buttons")
                    .child(a("Get Started").href("#/learn/quick-start")
                        .className("btn btn-primary").build())
                    .child(a("API Reference").href("#/reference/annotations")
                        .className("btn btn-secondary").build())))

            // Feature grid
            .child(Section.create().className("feature-grid")
                .child(h2("Why teavm-lambda?"))
                .child(Div.create().className("grid grid-4")
                    .child(FeatureCard.create(
                        "Lightning-Fast Cold Starts",
                        "~100ms cold starts on AWS Lambda by compiling Java to JavaScript. "
                        + "No JVM startup overhead."))
                    .child(FeatureCard.create(
                        "Zero Reflection",
                        "Annotation processor generates all routing and DI code at compile time. "
                        + "No runtime reflection, ever."))
                    .child(FeatureCard.create(
                        "Write Once, Run Anywhere",
                        "Same application code deploys to AWS Lambda, Google Cloud Run, "
                        + "standalone JVM, or Servlet containers."))
                    .child(FeatureCard.create(
                        "Kotlin DSL",
                        "Idiomatic Kotlin with lambda-with-receiver DSL for routing, "
                        + "middleware, DI, and validation."))))

            // Code preview section
            .child(Section.create().className("code-preview")
                .child(h2("Simple, Familiar API"))
                .child(p("Define REST endpoints with JAX-RS-style annotations in Java, "
                    + "or use the idiomatic Kotlin DSL. Either way, the annotation processor "
                    + "generates all wiring at compile time."))
                .child(CodeTabs.create(
                    """
                    @Path("/hello")
                    @Component
                    @Singleton
                    public class HelloResource {

                        @GET
                        public Response hello() {
                            return Response.ok("{\\"message\\":\\"Hello!\\"}")
                                    .header("Content-Type", "application/json");
                        }

                        @GET
                        @Path("/{name}")
                        public Response greet(@PathParam("name") String name) {
                            return Response.ok("{\\"message\\":\\"Hello, " + name + "!\\"}")
                                    .header("Content-Type", "application/json");
                        }
                    }""",
                    """
                    app {
                        routes {
                            get("/hello") {
                                ok(json { "message" to "Hello!" })
                            }
                            get("/hello/{name}") {
                                val name = pathParam("name")
                                ok(json { "message" to "Hello, $name!" })
                            }
                        }
                    }""")))

            .build();
    }
}
