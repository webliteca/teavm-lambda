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

public class MiddlewarePage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Middleware"))
            .child(p("teavm-lambda uses a composable middleware pipeline to handle "
                + "cross-cutting concerns like CORS, compression, and health checks. "
                + "Middleware wraps your router and processes requests before and after "
                + "they reach your route handlers."))
            .child(sectionMiddlewareRouter())
            .child(sectionBuiltInMiddleware())
            .child(sectionCustomMiddleware())
            .child(sectionOrdering())
            .build();
    }

    private static ReactElement sectionMiddlewareRouter() {
        String javaCode = """
var container = new GeneratedContainer();
var router = new GeneratedRouter(container);
var app = new MiddlewareRouter(router);
app.use(new CorsMiddleware());
app.use(new CompressionMiddleware());
app.use(new HealthEndpoint());
Platform.start(app);""";

        String kotlinCode = """
app {
    cors()
    compression()
    healthCheck()
    routes { /* ... */ }
}""";

        return Section.create().className("doc-section")
            .child(h2("MiddlewareRouter"))
            .child(p("The MiddlewareRouter wraps your generated router and lets you "
                + "compose middleware into a pipeline. Each middleware can inspect or "
                + "modify the request before passing it along, and transform the "
                + "response on the way back."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(Callout.pitfall("Import path",
                p("MiddlewareRouter is in the ca.weblite.teavmlambda.api package, "
                    + "not ca.weblite.teavmlambda.api.middleware. "
                    + "Make sure your import reads: "
                    + "import ca.weblite.teavmlambda.api.MiddlewareRouter;")))
            .build();
    }

    private static ReactElement sectionBuiltInMiddleware() {
        String corsJava = """
// Allow all origins with default settings
app.use(new CorsMiddleware());

// Custom CORS configuration
app.use(new CorsMiddleware()
    .allowOrigin("https://example.com")
    .allowMethods("GET", "POST", "PUT", "DELETE")
    .allowHeaders("Authorization", "Content-Type")
    .maxAge(3600));""";

        String corsKotlin = """
app {
    cors {
        allowOrigin("https://example.com")
        allowMethods("GET", "POST", "PUT", "DELETE")
        allowHeaders("Authorization", "Content-Type")
        maxAge(3600)
    }
}""";

        String compressionJava = """
// Compress responses automatically based on Accept-Encoding
app.use(new CompressionMiddleware());""";

        String compressionKotlin = """
app {
    compression()
}""";

        String healthJava = """
// Adds a GET /health endpoint returning {"status":"ok"}
app.use(new HealthEndpoint());""";

        String healthKotlin = """
app {
    healthCheck()
}""";

        return Section.create().className("doc-section")
            .child(h2("Built-in Middleware"))

            .child(h3("CorsMiddleware"))
            .child(p("Handles Cross-Origin Resource Sharing (CORS) headers. "
                + "By default it allows all origins and common HTTP methods. "
                + "You can customize allowed origins, methods, headers, and the "
                + "preflight cache duration."))
            .child(CodeTabs.create(corsJava, corsKotlin))

            .child(h3("CompressionMiddleware"))
            .child(p("Automatically compresses response bodies using gzip or deflate "
                + "based on the client's Accept-Encoding header. This reduces bandwidth "
                + "usage for API responses."))
            .child(CodeTabs.create(compressionJava, compressionKotlin))

            .child(h3("HealthEndpoint"))
            .child(p("Adds a GET /health endpoint that returns a 200 status with "
                + "a JSON body of {\"status\":\"ok\"}. This is useful for load "
                + "balancer health checks and container orchestration probes."))
            .child(CodeTabs.create(healthJava, healthKotlin))
            .build();
    }

    private static ReactElement sectionCustomMiddleware() {
        String javaCode = """
public class LoggingMiddleware implements Middleware {
    @Override
    public Response handle(Request request, MiddlewareChain chain) {
        long start = System.currentTimeMillis();
        Response response = chain.next(request);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println(request.getMethod() + " "
            + request.getPath() + " -> "
            + response.getStatus() + " (" + elapsed + "ms)");
        return response;
    }
}

// Register it
app.use(new LoggingMiddleware());""";

        String kotlinCode = """
class LoggingMiddleware : Middleware {
    override fun handle(request: Request, chain: MiddlewareChain): Response {
        val start = System.currentTimeMillis()
        val response = chain.next(request)
        val elapsed = System.currentTimeMillis() - start
        println("${request.method} ${request.path} -> "
            + "${response.status} (${elapsed}ms)")
        return response
    }
}

// Register it
app.use(LoggingMiddleware())""";

        return Section.create().className("doc-section")
            .child(h2("Custom Middleware"))
            .child(p("Create your own middleware by implementing the Middleware interface. "
                + "Your handle method receives the incoming Request and a MiddlewareChain. "
                + "Call chain.next(request) to pass control to the next middleware or the "
                + "router. You can modify the request before passing it along, or transform "
                + "the response after."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionOrdering() {
        String javaCode = """
// Middleware executes in registration order:
// 1. CORS headers are added first
// 2. Then logging records the request
// 3. Then compression wraps the response
// 4. Finally the router handles the request
app.use(new CorsMiddleware());       // 1st
app.use(new LoggingMiddleware());    // 2nd
app.use(new CompressionMiddleware());// 3rd
Platform.start(app);                 // router is last""";

        return Section.create().className("doc-section")
            .child(h2("Middleware Ordering"))
            .child(p("Middleware executes in the order you register it with app.use(). "
                + "The first middleware registered is the first to process the request "
                + "and the last to see the response. Think of it as an onion: each "
                + "layer wraps the next."))
            .child(CodeBlock.create(javaCode, "java"))
            .child(Callout.note("Order matters",
                p("Place CORS middleware first so preflight requests are handled "
                    + "before any other processing. Place compression last (before "
                    + "the router) so it compresses the final response body.")))
            .build();
    }
}
