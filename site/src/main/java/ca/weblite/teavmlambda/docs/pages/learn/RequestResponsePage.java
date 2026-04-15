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

public class RequestResponsePage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Request and Response"))
            .child(p("teavm-lambda uses immutable Request and Response objects throughout "
                + "the framework. The Response uses a fluent builder pattern for "
                + "constructing HTTP responses, while the Request provides read-only "
                + "access to all parts of the incoming HTTP request."))
            .child(sectionResponse())
            .child(sectionRequest())
            .child(sectionJsonBuilder())
            .child(sectionJsonReader())
            .child(sectionProblemDetail())
            .build();
    }

    private static ReactElement sectionResponse() {
        String javaCode = """
                // 200 OK with a body
                Response.ok("{\\"status\\":\\"ok\\"}")
                        .header("Content-Type", "application/json");

                // 201 Created
                Response.status(201)
                        .body("{\\"id\\":\\"abc-123\\"}")
                        .header("Content-Type", "application/json")
                        .header("Location", "/users/abc-123");

                // 204 No Content
                Response.status(204).build();

                // 404 Not Found
                Response.status(404)
                        .body("{\\"error\\":\\"Not found\\"}")
                        .header("Content-Type", "application/json");

                // 302 Redirect
                Response.status(302)
                        .header("Location", "https://example.com");

                // Multiple headers
                Response.ok("data")
                        .header("Content-Type", "text/plain")
                        .header("Cache-Control", "no-cache")
                        .header("X-Request-Id", requestId);""";

        String kotlinCode = """
                // 200 OK with a body
                ok(json { "status" to "ok" })

                // 201 Created
                Response.status(201)
                    .body(json { "id" to "abc-123" })
                    .header("Content-Type", "application/json")
                    .header("Location", "/users/abc-123")

                // 204 No Content
                noContent()

                // 404 Not Found
                Response.status(404)
                    .body(json { "error" to "Not found" })
                    .header("Content-Type", "application/json")

                // 302 Redirect
                Response.status(302)
                    .header("Location", "https://example.com")

                // Multiple headers
                Response.ok("data")
                    .header("Content-Type", "text/plain")
                    .header("Cache-Control", "no-cache")
                    .header("X-Request-Id", requestId)""";

        return Section.create().className("doc-section")
            .child(h2("Building Responses"))
            .child(p("The Response class uses an immutable builder pattern. Each method "
                + "call returns a new Response instance, leaving the original unchanged. "
                + "Start with Response.ok() for a 200 response or Response.status(code) "
                + "for any other status code."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Method"),
                            El.classedText("th", "", "Description")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("Response.ok(body)")),
                            El.classedText("td", "", "Creates a 200 OK response with the given body")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("Response.status(code)")),
                            El.classedText("td", "", "Creates a response with the given HTTP status code")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code(".body(string)")),
                            El.classedText("td", "", "Sets the response body")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code(".header(name, value)")),
                            El.classedText("td", "", "Adds a response header")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code(".build()")),
                            El.classedText("td", "", "Finalizes the response (optional for most cases)")
                        )
                    )
                )
            ))
            .build();
    }

    private static ReactElement sectionRequest() {
        String javaCode = """
                @POST
                @Path("/echo")
                public Response echo(@Body String body,
                                     @HeaderParam("X-Request-Id") String requestId,
                                     @QueryParam("format") String format) {
                    // You can also access the full Request object
                    // in middleware or custom code:
                    // request.getMethod()      -> "POST"
                    // request.getPath()         -> "/echo"
                    // request.getBody()         -> raw body string
                    // request.getHeaders()      -> Map<String, String>
                    // request.getQueryParams()  -> Map<String, String>
                    // request.getPathParams()   -> Map<String, String>
                    return Response.ok(body)
                            .header("Content-Type", "application/json")
                            .header("X-Request-Id", requestId);
                }""";

        String kotlinCode = """
                post("/echo") {
                    val body = body()
                    val requestId = header("X-Request-Id")
                    val format = queryParam("format")
                    // You can also access the full Request object
                    // in middleware or custom code:
                    // request.method      -> "POST"
                    // request.path         -> "/echo"
                    // request.body         -> raw body string
                    // request.headers      -> Map<String, String>
                    // request.queryParams  -> Map<String, String>
                    // request.pathParams   -> Map<String, String>
                    ok(body)
                        .header("Content-Type", "application/json")
                        .header("X-Request-Id", requestId)
                }""";

        return Section.create().className("doc-section")
            .child(h2("Reading the Request"))
            .child(p("In route handler methods, use @PathParam, @QueryParam, @HeaderParam, "
                + "and @Body annotations to extract values from the request. In middleware, "
                + "you receive the full Request object with methods to access every part "
                + "of the HTTP request."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Method"),
                            El.classedText("th", "", "Returns"),
                            El.classedText("th", "", "Description")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("getMethod()")),
                            El.classed("td", "", code("String")),
                            El.classedText("td", "", "HTTP method (GET, POST, PUT, DELETE)")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getPath()")),
                            El.classed("td", "", code("String")),
                            El.classedText("td", "", "Request path without query string")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getBody()")),
                            El.classed("td", "", code("String")),
                            El.classedText("td", "", "Raw request body")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getHeaders()")),
                            El.classed("td", "", code("Map<String, String>")),
                            El.classedText("td", "", "All request headers")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getQueryParams()")),
                            El.classed("td", "", code("Map<String, String>")),
                            El.classedText("td", "", "Query string parameters")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getPathParams()")),
                            El.classed("td", "", code("Map<String, String>")),
                            El.classedText("td", "", "Path template parameters")
                        )
                    )
                )
            ))
            .build();
    }

    private static ReactElement sectionJsonBuilder() {
        String javaCode = """
                // Simple JSON object
                String json = new JsonBuilder()
                        .put("name", "Alice")
                        .put("age", 30)
                        .put("active", true)
                        .build();
                // {"name":"Alice","age":30,"active":true}

                // Nested object
                String nested = new JsonBuilder()
                        .put("user", new JsonBuilder()
                                .put("id", "abc-123")
                                .put("name", "Alice"))
                        .put("role", "admin")
                        .build();
                // {"user":{"id":"abc-123","name":"Alice"},"role":"admin"}

                // Return as response
                return Response.ok(new JsonBuilder()
                        .put("status", "created")
                        .put("id", newId)
                        .build())
                        .header("Content-Type", "application/json");""";

        String kotlinCode = """
                // Simple JSON object
                val result = json {
                    "name" to "Alice"
                    "age" to 30
                    "active" to true
                }
                // {"name":"Alice","age":30,"active":true}

                // Nested object
                val nested = json {
                    "user" to json {
                        "id" to "abc-123"
                        "name" to "Alice"
                    }
                    "role" to "admin"
                }
                // {"user":{"id":"abc-123","name":"Alice"},"role":"admin"}

                // Return as response
                ok(json {
                    "status" to "created"
                    "id" to newId
                })""";

        return Section.create().className("doc-section")
            .child(h2("JSON Builder"))
            .child(p("The JsonBuilder class provides a fluent API for constructing JSON "
                + "strings. It handles proper escaping and formatting. Use the put() "
                + "method to add key-value pairs, and call build() to produce the "
                + "final JSON string."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(Callout.note("No external JSON library",
                p("JsonBuilder and JsonReader are built into teavm-lambda-core. They "
                    + "produce and parse JSON strings directly without depending on "
                    + "Jackson, Gson, or any other library. This keeps the compiled "
                    + "output small for TeaVM targets.")))
            .build();
    }

    private static ReactElement sectionJsonReader() {
        String javaCode = """
                String body = "{\\"name\\":\\"Alice\\",\\"age\\":30,\\"active\\":true}";
                JsonReader reader = new JsonReader(body);

                String name = reader.getString("name");     // "Alice"
                int age = reader.getInt("age");              // 30
                boolean active = reader.getBoolean("active"); // true

                // Nested objects
                String nested = "{\\"user\\":{\\"id\\":\\"abc\\",\\"name\\":\\"Alice\\"}}";
                JsonReader outer = new JsonReader(nested);
                JsonReader userReader = outer.getObject("user");
                String userId = userReader.getString("id");  // "abc"

                // Check for missing keys
                String missing = reader.getString("missing"); // null""";

        String kotlinCode = """
                val body = "{\"name\":\"Alice\",\"age\":30,\"active\":true}"
                val reader = JsonReader(body)

                val name = reader.getString("name")      // "Alice"
                val age = reader.getInt("age")            // 30
                val active = reader.getBoolean("active")  // true

                // Nested objects
                val nested = "{\"user\":{\"id\":\"abc\",\"name\":\"Alice\"}}"
                val outer = JsonReader(nested)
                val userReader = outer.getObject("user")
                val userId = userReader.getString("id")   // "abc"

                // Check for missing keys
                val missing = reader.getString("missing") // null""";

        return Section.create().className("doc-section")
            .child(h2("JSON Reader"))
            .child(p("The JsonReader class parses a JSON string and provides typed "
                + "accessor methods for extracting values. It supports strings, integers, "
                + "booleans, doubles, nested objects, and arrays. Missing keys return "
                + "null for reference types or zero/false for primitives."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Method"),
                            El.classedText("th", "", "Returns"),
                            El.classedText("th", "", "Description")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("getString(key)")),
                            El.classed("td", "", code("String")),
                            El.classedText("td", "", "String value or null")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getInt(key)")),
                            El.classed("td", "", code("int")),
                            El.classedText("td", "", "Integer value or 0")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getLong(key)")),
                            El.classed("td", "", code("long")),
                            El.classedText("td", "", "Long value or 0")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getDouble(key)")),
                            El.classed("td", "", code("double")),
                            El.classedText("td", "", "Double value or 0.0")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getBoolean(key)")),
                            El.classed("td", "", code("boolean")),
                            El.classedText("td", "", "Boolean value or false")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getObject(key)")),
                            El.classed("td", "", code("JsonReader")),
                            El.classedText("td", "", "Nested JSON object as a new reader")
                        )
                    )
                )
            ))
            .build();
    }

    private static ReactElement sectionProblemDetail() {
        String javaCode = """
                // RFC 7807 Problem Detail response
                ProblemDetail problem = new ProblemDetail()
                        .type("https://example.com/errors/not-found")
                        .title("User Not Found")
                        .status(404)
                        .detail("No user exists with id abc-123")
                        .instance("/users/abc-123");

                return Response.status(404)
                        .body(problem.toJson())
                        .header("Content-Type", "application/problem+json");

                // Validation error with extensions
                ProblemDetail validation = new ProblemDetail()
                        .type("https://example.com/errors/validation")
                        .title("Validation Error")
                        .status(422)
                        .detail("One or more fields failed validation")
                        .extension("errors", new JsonBuilder()
                                .put("email", "must be a valid email address")
                                .put("age", "must be at least 18")
                                .build());

                return Response.status(422)
                        .body(validation.toJson())
                        .header("Content-Type", "application/problem+json");""";

        String kotlinCode = """
                // RFC 7807 Problem Detail response
                val problem = ProblemDetail()
                    .type("https://example.com/errors/not-found")
                    .title("User Not Found")
                    .status(404)
                    .detail("No user exists with id abc-123")
                    .instance("/users/abc-123")

                Response.status(404)
                    .body(problem.toJson())
                    .header("Content-Type", "application/problem+json")

                // Validation error with extensions
                val validation = ProblemDetail()
                    .type("https://example.com/errors/validation")
                    .title("Validation Error")
                    .status(422)
                    .detail("One or more fields failed validation")
                    .extension("errors", json {
                        "email" to "must be a valid email address"
                        "age" to "must be at least 18"
                    })

                Response.status(422)
                    .body(validation.toJson())
                    .header("Content-Type", "application/problem+json")""";

        return Section.create().className("doc-section")
            .child(h2("ProblemDetail (RFC 7807)"))
            .child(p("The ProblemDetail class implements RFC 7807 for standardized error "
                + "responses. It produces JSON in the application/problem+json format, "
                + "which is widely understood by HTTP clients and API tooling."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Field"),
                            El.classedText("th", "", "Type"),
                            El.classedText("th", "", "Description")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("type")),
                            El.classed("td", "", code("String")),
                            El.classedText("td", "", "URI identifying the problem type")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("title")),
                            El.classed("td", "", code("String")),
                            El.classedText("td", "", "Short human-readable summary")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("status")),
                            El.classed("td", "", code("int")),
                            El.classedText("td", "", "HTTP status code")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("detail")),
                            El.classed("td", "", code("String")),
                            El.classedText("td", "", "Detailed human-readable explanation")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("instance")),
                            El.classed("td", "", code("String")),
                            El.classedText("td", "", "URI reference for the specific occurrence")
                        )
                    )
                )
            ))
            .child(Callout.note("Content-Type header",
                p("When returning ProblemDetail responses, always set the Content-Type "
                    + "header to application/problem+json. This allows clients to "
                    + "distinguish error responses from normal JSON responses and "
                    + "parse them accordingly.")))
            .build();
    }
}
