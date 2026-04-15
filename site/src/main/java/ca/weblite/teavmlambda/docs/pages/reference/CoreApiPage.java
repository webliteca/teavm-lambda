package ca.weblite.teavmlambda.docs.pages.reference;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder.Div;
import ca.weblite.teavmreact.html.DomBuilder.Section;
import org.teavm.jso.JSObject;
import static ca.weblite.teavmreact.html.Html.*;
import ca.weblite.teavmlambda.docs.El;
import ca.weblite.teavmlambda.docs.components.CodeBlock;
import ca.weblite.teavmlambda.docs.components.CodeTabs;
import ca.weblite.teavmlambda.docs.components.Callout;

public class CoreApiPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Core API Reference"))
            .child(p("This page documents the core classes in the "
                + "ca.weblite.teavmlambda.api package. These are the building blocks "
                + "for every teavm-lambda application: requests, responses, DI, "
                + "platform abstraction, JSON utilities, and error handling."))
            .child(sectionResponse())
            .child(sectionRequest())
            .child(sectionContainer())
            .child(sectionPlatform())
            .child(sectionJsonBuilder())
            .child(sectionJsonReader())
            .child(sectionProblemDetail())
            .build();
    }

    private static ReactElement sectionResponse() {
        String javaCode = """
// Simple 200 response
Response ok = Response.ok("{\\"message\\":\\"Hello!\\"}");

// Custom status with headers
Response created = Response.status(201)
        .header("Content-Type", "application/json")
        .header("Location", "/users/42")
        .body("{\\"id\\": 42}");

// Binary response
byte[] png = loadImage();
Response image = Response.ok("")
        .header("Content-Type", "image/png")
        .bodyBytes(png);

// Reading response fields
int status = created.getStatusCode();
String body = created.getBody();
Map<String, String> headers = created.getHeaders();""";

        return Section.create().className("doc-section")
            .child(h2("Response"))
            .child(p("Response is an immutable HTTP response object built with a fluent "
                + "API. Every handler method returns a Response. Each builder method "
                + "returns a new instance, so Response objects are safe to share and reuse."))
            .child(El.table("api-table",
                thead(
                    tr(th("Method"), th("Description"))),
                tbody(
                    tr(td(code("Response.ok(body)")), td(text("200 OK with body"))),
                    tr(td(code("Response.status(code)")), td(text("Custom status code"))),
                    tr(td(code(".header(name, value)")), td(text("Add header"))),
                    tr(td(code(".body(string)")), td(text("Set response body"))),
                    tr(td(code(".bodyBytes(byte[])")), td(text("Set binary body"))),
                    tr(td(code(".getStatusCode()")), td(text("Get status code"))),
                    tr(td(code(".getBody()")), td(text("Get body string"))),
                    tr(td(code(".getHeaders()")), td(text("Get headers map"))))))
            .child(h3("Example"))
            .child(CodeBlock.create(javaCode, "java"))
            .child(Callout.note("Immutability",
                p("Response is immutable. Methods like .header() and .body() return a "
                    + "new Response instance rather than modifying the original. This "
                    + "makes Response safe to use in middleware pipelines.")))
            .build();
    }

    private static ReactElement sectionRequest() {
        String javaCode = """
// Request is passed to middleware and available in handlers
public Response handle(Request request, MiddlewareChain chain) {
    String method = request.getMethod();       // "GET", "POST", etc.
    String path = request.getPath();           // "/users/42"
    String body = request.getBody();           // request body string
    Map<String, String> headers = request.getHeaders();
    Map<String, String> query = request.getQueryParams();
    Map<String, String> pathParams = request.getPathParams();

    // Clone with new path parameters
    Map<String, String> newParams = new HashMap<>(pathParams);
    newParams.put("version", "v2");
    Request modified = request.withPathParams(newParams);

    return chain.next(modified);
}""";

        return Section.create().className("doc-section")
            .child(h2("Request"))
            .child(p("Request represents an incoming HTTP request. It provides read-only "
                + "access to the HTTP method, path, headers, query parameters, path "
                + "parameters, and body. Use withPathParams() to create a modified "
                + "copy for middleware forwarding."))
            .child(El.table("api-table",
                thead(
                    tr(th("Method"), th("Description"))),
                tbody(
                    tr(td(code("getMethod()")), td(text("HTTP method"))),
                    tr(td(code("getPath()")), td(text("Request path"))),
                    tr(td(code("getBody()")), td(text("Request body string"))),
                    tr(td(code("getHeaders()")), td(text("Headers map"))),
                    tr(td(code("getQueryParams()")), td(text("Query parameters map"))),
                    tr(td(code("getPathParams()")), td(text("Path parameters map"))),
                    tr(td(code("withPathParams(map)")), td(text("Clone with new path params"))))))
            .child(h3("Example"))
            .child(CodeBlock.create(javaCode, "java"))
            .build();
    }

    private static ReactElement sectionContainer() {
        String javaCode = """
// Manual container usage (usually handled by GeneratedContainer)
Container container = new Container();
container.register(Database.class, DatabaseFactory.create());
container.registerSingleton(UserService.class, () -> {
    Database db = container.get(Database.class);
    return new UserService(db);
});

// Retrieve instances
UserService service = container.get(UserService.class);
boolean hasDb = container.has(Database.class); // true""";

        return Section.create().className("doc-section")
            .child(h2("Container"))
            .child(p("Container is the dependency injection container. In most applications "
                + "you use the GeneratedContainer produced by the annotation processor. "
                + "The Container API is useful when you need to register additional "
                + "dependencies manually or in tests."))
            .child(El.table("api-table",
                thead(
                    tr(th("Method"), th("Description"))),
                tbody(
                    tr(td(code("register(class, instance)")), td(text("Register instance"))),
                    tr(td(code("registerSingleton(class, supplier)")), td(text("Register singleton factory"))),
                    tr(td(code("get(class)")), td(text("Get instance"))),
                    tr(td(code("has(class)")), td(text("Check if registered"))))))
            .child(h3("Example"))
            .child(CodeBlock.create(javaCode, "java"))
            .child(Callout.note("GeneratedContainer",
                p("You rarely interact with Container directly. The annotation processor "
                    + "generates GeneratedContainer, which pre-registers all @Component, "
                    + "@Service, and @Repository classes with their dependencies.")))
            .build();
    }

    private static ReactElement sectionPlatform() {
        String javaCode = """
// Start the server with auto-detected adapter
var container = new GeneratedContainer();
var router = new GeneratedRouter(container);
Platform.start(router);

// Read environment variables
String dbUrl = Platform.env("DATABASE_URL");
String port = Platform.env("PORT", "8080");""";

        return Section.create().className("doc-section")
            .child(h2("Platform"))
            .child(p("Platform is the runtime abstraction layer. It discovers the correct "
                + "adapter (AWS Lambda, Cloud Run, standalone JVM, or Servlet) via "
                + "ServiceLoader and starts the server. It also provides portable "
                + "access to environment variables."))
            .child(El.table("api-table",
                thead(
                    tr(th("Method"), th("Description"))),
                tbody(
                    tr(td(code("Platform.start(router)")), td(text("Start the server"))),
                    tr(td(code("Platform.env(name)")), td(text("Read env variable"))),
                    tr(td(code("Platform.env(name, default)")), td(text("Read with default"))))))
            .child(h3("Example"))
            .child(CodeBlock.create(javaCode, "java"))
            .child(Callout.pitfall("WAR deployment",
                p("When deploying as a WAR to a Servlet container, Platform.start() is "
                    + "not used. The servlet container manages the lifecycle. "
                    + "Platform.env() still works in all deployment modes.")))
            .build();
    }

    private static ReactElement sectionJsonBuilder() {
        String javaCode = """
// Build a JSON object
String json = JsonBuilder.object()
        .put("name", "Alice")
        .put("age", 30)
        .put("active", true)
        .build();
// {"name":"Alice","age":30,"active":true}

// Build a JSON array
String arr = JsonBuilder.array()
        .add("one")
        .add("two")
        .add("three")
        .build();
// ["one","two","three"]

// Nested objects
String nested = JsonBuilder.object()
        .put("user", JsonBuilder.object()
                .put("name", "Alice")
                .put("roles", JsonBuilder.array()
                        .add("admin")
                        .add("user")
                        .build())
                .build())
        .build();""";

        return Section.create().className("doc-section")
            .child(h2("JsonBuilder"))
            .child(p("JsonBuilder provides a fluent API for creating JSON strings without "
                + "external dependencies. It handles escaping and formatting automatically."))
            .child(El.table("api-table",
                thead(
                    tr(th("Method"), th("Description"))),
                tbody(
                    tr(td(code("JsonBuilder.object()")), td(text("Start object"))),
                    tr(td(code("JsonBuilder.array()")), td(text("Start array"))),
                    tr(td(code(".put(key, value)")), td(text("Add field"))),
                    tr(td(code(".add(value)")), td(text("Add array element"))),
                    tr(td(code(".build()")), td(text("Build JSON string"))))))
            .child(h3("Example"))
            .child(CodeBlock.create(javaCode, "java"))
            .build();
    }

    private static ReactElement sectionJsonReader() {
        String javaCode = """
// Parse and read fields
JsonReader reader = JsonReader.parse(
        "{\\"name\\":\\"Alice\\",\\"age\\":30,\\"active\\":true}");

String name = reader.getString("name");     // "Alice"
int age = reader.getInt("age");             // 30
double score = reader.getDouble("score");   // 0.0 if missing
boolean active = reader.getBoolean("active"); // true

// Check if a field exists before reading
if (reader.has("email")) {
    String email = reader.getString("email");
}""";

        return Section.create().className("doc-section")
            .child(h2("JsonReader"))
            .child(p("JsonReader parses a JSON string and provides typed accessor methods "
                + "for reading fields. It is a lightweight alternative to full JSON "
                + "binding libraries."))
            .child(El.table("api-table",
                thead(
                    tr(th("Method"), th("Description"))),
                tbody(
                    tr(td(code("JsonReader.parse(json)")), td(text("Parse JSON string"))),
                    tr(td(code(".getString(key)")), td(text("Get string field"))),
                    tr(td(code(".getInt(key)")), td(text("Get int field"))),
                    tr(td(code(".getDouble(key)")), td(text("Get double field"))),
                    tr(td(code(".getBoolean(key)")), td(text("Get boolean field"))),
                    tr(td(code(".has(key)")), td(text("Check field exists"))))))
            .child(h3("Example"))
            .child(CodeBlock.create(javaCode, "java"))
            .build();
    }

    private static ReactElement sectionProblemDetail() {
        String javaCode = """
// Convenience factory methods
Response badReq = ProblemDetail.badRequest("Name is required")
        .toResponse();

Response notFound = ProblemDetail.notFound("User 42 not found")
        .toResponse();

Response conflict = ProblemDetail.conflict("Email already exists")
        .toResponse();

Response error = ProblemDetail.internalError("Unexpected failure")
        .toResponse();

// Custom error
Response custom = ProblemDetail.of(422, "Unprocessable Entity",
        "The age field must be a positive integer")
        .toResponse();""";

        return Section.create().className("doc-section")
            .child(h2("ProblemDetail"))
            .child(p("ProblemDetail implements RFC 7807 Problem Details for HTTP APIs. "
                + "It provides a standard JSON error format with type, title, status, "
                + "and detail fields. Use the factory methods to create common error "
                + "responses."))
            .child(El.table("api-table",
                thead(
                    tr(th("Method"), th("Description"))),
                tbody(
                    tr(td(code("ProblemDetail.badRequest(detail)")), td(text("400 error"))),
                    tr(td(code("ProblemDetail.notFound(detail)")), td(text("404 error"))),
                    tr(td(code("ProblemDetail.conflict(detail)")), td(text("409 error"))),
                    tr(td(code("ProblemDetail.internalError(detail)")), td(text("500 error"))),
                    tr(td(code("ProblemDetail.of(status, title, detail)")), td(text("Custom error"))))))
            .child(h3("Example"))
            .child(CodeBlock.create(javaCode, "java"))
            .child(Callout.note("RFC 7807 format",
                p("ProblemDetail responses use Content-Type: application/problem+json "
                    + "and include type, title, status, and detail fields. This is the "
                    + "standard format used by validation errors and security rejections.")))
            .build();
    }
}
