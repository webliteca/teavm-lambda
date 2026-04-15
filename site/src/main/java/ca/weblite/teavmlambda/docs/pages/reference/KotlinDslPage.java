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

public class KotlinDslPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Kotlin DSL Reference"))
            .child(p("teavm-lambda provides an idiomatic Kotlin DSL as an alternative to "
                + "annotations. The DSL uses lambda-with-receiver patterns for routing, "
                + "middleware, dependency injection, validation, and JSON construction. "
                + "The annotation processor is not needed when using the DSL -- all wiring "
                + "is defined in code."))
            .child(sectionAppBlock())
            .child(sectionRoutes())
            .child(sectionPathNesting())
            .child(sectionMiddleware())
            .child(sectionServices())
            .child(sectionJson())
            .child(sectionValidation())
            .child(sectionDatabase())
            .build();
    }

    private static ReactElement sectionAppBlock() {
        String kotlinCode = """
import ca.weblite.teavmlambda.dsl.*

fun main() {
    app {
        cors()
        compression()
        healthCheck()

        services {
            singleton<UserService> { UserServiceImpl() }
        }

        routes {
            get("/hello") {
                ok(json { "message" to "Hello!" })
            }
        }
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("app { } Block"))
            .child(p("The app block is the entry point for a Kotlin DSL application. "
                + "It configures middleware, services, and routes in a single "
                + "declarative block. Calling app { } automatically discovers the "
                + "correct platform adapter and starts the server."))
            .child(CodeBlock.create(kotlinCode, "kotlin"))
            .child(Callout.note("No annotation processor",
                p("When using the Kotlin DSL, you do not need the annotation processor "
                    + "or GeneratedRouter/GeneratedContainer. The DSL builds the router "
                    + "and container directly in code.")))
            .build();
    }

    private static ReactElement sectionRoutes() {
        String kotlinCode = """
routes {
    get("/users") {
        val page = queryParam("page") ?: "1"
        ok(json { "page" to page })
    }

    get("/users/{id}") {
        val id = pathParam("id")
        ok(json { "id" to id })
    }

    post("/users") {
        val body = body()
        created(body)
    }

    put("/users/{id}") {
        val id = pathParam("id")
        val body = body()
        ok(body)
    }

    delete("/users/{id}") {
        val id = pathParam("id")
        noContent()
    }

    patch("/users/{id}") {
        val id = pathParam("id")
        val body = body()
        ok(body)
    }

    head("/health") {
        ok("")
    }

    options("/users") {
        ok("")
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("Route DSL"))
            .child(p("The routes block defines HTTP endpoints. Each route method "
                + "(get, post, put, delete, patch, head, options) takes a path pattern "
                + "and a handler lambda. Inside the handler, you have access to "
                + "pathParam(), queryParam(), body(), header(), and response helpers "
                + "like ok(), created(), and noContent()."))
            .child(CodeBlock.create(kotlinCode, "kotlin"))
            .child(El.table("api-table",
                thead(
                    tr(th("Function"), th("Description"))),
                tbody(
                    tr(td(code("get(path) { }")), td(text("HTTP GET route"))),
                    tr(td(code("post(path) { }")), td(text("HTTP POST route"))),
                    tr(td(code("put(path) { }")), td(text("HTTP PUT route"))),
                    tr(td(code("delete(path) { }")), td(text("HTTP DELETE route"))),
                    tr(td(code("patch(path) { }")), td(text("HTTP PATCH route"))),
                    tr(td(code("head(path) { }")), td(text("HTTP HEAD route"))),
                    tr(td(code("options(path) { }")), td(text("HTTP OPTIONS route"))),
                    tr(td(code("pathParam(name)")), td(text("Extract path parameter"))),
                    tr(td(code("queryParam(name)")), td(text("Extract query parameter"))),
                    tr(td(code("body()")), td(text("Get request body"))),
                    tr(td(code("header(name)")), td(text("Get header value"))),
                    tr(td(code("ok(body)")), td(text("Return 200 response"))),
                    tr(td(code("created(body)")), td(text("Return 201 response"))),
                    tr(td(code("noContent()")), td(text("Return 204 response"))))))
            .build();
    }

    private static ReactElement sectionPathNesting() {
        String kotlinCode = """
routes {
    path("/api") {
        path("/v1") {
            get("/users") {
                // matches GET /api/v1/users
                ok(json { "version" to "v1" })
            }
            get("/users/{id}") {
                // matches GET /api/v1/users/{id}
                val id = pathParam("id")
                ok(json { "id" to id })
            }
        }

        path("/v2") {
            get("/users") {
                // matches GET /api/v2/users
                ok(json { "version" to "v2" })
            }
        }
    }

    // Top-level routes still work
    get("/health") {
        ok(json { "status" to "ok" })
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("Path Nesting"))
            .child(p("Use path() blocks to nest routes under a common prefix. "
                + "Nested paths are concatenated automatically. This is useful for "
                + "versioned APIs or grouping related endpoints."))
            .child(CodeBlock.create(kotlinCode, "kotlin"))
            .child(Callout.note("Path concatenation",
                p("Nested path() blocks concatenate their prefixes. A path(\"/api\") "
                    + "containing path(\"/v1\") containing get(\"/users\") produces "
                    + "the route GET /api/v1/users.")))
            .build();
    }

    private static ReactElement sectionMiddleware() {
        String kotlinCode = """
app {
    // Built-in middleware
    cors {
        allowOrigin("https://example.com")
        allowMethods("GET", "POST", "PUT", "DELETE")
        allowHeaders("Authorization", "Content-Type")
        maxAge(3600)
    }
    compression()
    healthCheck()

    // Custom middleware
    use { request, chain ->
        val start = System.currentTimeMillis()
        val response = chain.next(request)
        val elapsed = System.currentTimeMillis() - start
        println("${request.method} ${request.path} -> "
            + "${response.status} (${elapsed}ms)")
        response
    }

    routes { /* ... */ }
}""";

        return Section.create().className("doc-section")
            .child(h2("Middleware DSL"))
            .child(p("Middleware is configured at the top level of the app block. "
                + "Built-in middleware like cors(), compression(), and healthCheck() "
                + "have dedicated functions. Use the use { } block for custom "
                + "middleware."))
            .child(CodeBlock.create(kotlinCode, "kotlin"))
            .child(El.table("api-table",
                thead(
                    tr(th("Function"), th("Description"))),
                tbody(
                    tr(td(code("cors { }")), td(text("CORS middleware with optional configuration"))),
                    tr(td(code("compression()")), td(text("Response compression middleware"))),
                    tr(td(code("healthCheck()")), td(text("GET /health endpoint"))),
                    tr(td(code("use { request, chain -> }")), td(text("Custom middleware lambda"))))))
            .build();
    }

    private static ReactElement sectionServices() {
        String kotlinCode = """
app {
    services {
        // Singleton: one instance shared across all requests
        singleton<UserService> { UserServiceImpl(get()) }

        // Transient: new instance per injection
        transient<EmailSender> { SmtpEmailSender() }

        // Register a specific instance
        instance<Config>(AppConfig.load())
    }

    routes {
        get("/users") {
            // Retrieve a service inside a handler
            val userService = service<UserService>()
            ok(userService.findAll())
        }
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("DI DSL"))
            .child(p("The services block registers dependencies in the container. "
                + "Use singleton for shared instances, transient for per-injection "
                + "instances, and instance to register a pre-built object. Inside "
                + "handlers, call service<T>() to retrieve a dependency."))
            .child(CodeBlock.create(kotlinCode, "kotlin"))
            .child(El.table("api-table",
                thead(
                    tr(th("Function"), th("Description"))),
                tbody(
                    tr(td(code("singleton<T> { }")), td(text("Register singleton factory"))),
                    tr(td(code("transient<T> { }")), td(text("Register transient factory"))),
                    tr(td(code("instance<T>(obj)")), td(text("Register existing instance"))),
                    tr(td(code("get<T>()")), td(text("Resolve dependency inside factory"))),
                    tr(td(code("service<T>()")), td(text("Resolve dependency inside handler"))))))
            .build();
    }

    private static ReactElement sectionJson() {
        String kotlinCode = """
// Simple object
val user = json {
    "name" to "Alice"
    "age" to 30
    "active" to true
}
// {"name":"Alice","age":30,"active":true}

// Nested object
val response = json {
    "user" to json {
        "name" to "Alice"
        "roles" to jsonArray {
            add("admin")
            add("user")
        }
    }
    "status" to "ok"
}

// Use in route handlers
get("/users/{id}") {
    val id = pathParam("id")
    ok(json {
        "id" to id
        "name" to "Alice"
    })
}""";

        return Section.create().className("doc-section")
            .child(h2("JSON DSL"))
            .child(p("The json { } block provides a concise way to build JSON strings. "
                + "Use the to infix function for key-value pairs. Nest json { } "
                + "blocks for objects and jsonArray { } for arrays."))
            .child(CodeBlock.create(kotlinCode, "kotlin"))
            .child(El.table("api-table",
                thead(
                    tr(th("Function"), th("Description"))),
                tbody(
                    tr(td(code("json { }")), td(text("Build JSON object"))),
                    tr(td(code("jsonArray { }")), td(text("Build JSON array"))),
                    tr(td(code("\"key\" to value")), td(text("Add key-value pair"))),
                    tr(td(code("add(value)")), td(text("Add array element"))))))
            .build();
    }

    private static ReactElement sectionValidation() {
        String kotlinCode = """
post("/users") {
    val body = body()

    validate {
        require("name") { it.isNotEmpty() }
        require("email") { it.contains("@") }
        require("age") { it.toIntOrNull() != null && it.toInt() in 0..150 }
    }

    // If validation fails, a 400 ProblemDetail is returned automatically
    // Code below only runs if all validations pass
    created(body)
}

// Custom error messages
post("/login") {
    validate {
        require("username", "Username is required") { it.isNotEmpty() }
        require("password", "Password must be at least 8 characters") {
            it.length >= 8
        }
    }
    ok(json { "token" to "..." })
}""";

        return Section.create().className("doc-section")
            .child(h2("Validation DSL"))
            .child(p("The validate { } block validates request parameters inside a "
                + "handler. Each require() call specifies a parameter name and a "
                + "predicate. If validation fails, the handler short-circuits with "
                + "a 400 ProblemDetail response."))
            .child(CodeBlock.create(kotlinCode, "kotlin"))
            .child(El.table("api-table",
                thead(
                    tr(th("Function"), th("Description"))),
                tbody(
                    tr(td(code("validate { }")), td(text("Start validation block"))),
                    tr(td(code("require(name) { predicate }")), td(text("Validate parameter with predicate"))),
                    tr(td(code("require(name, message) { predicate }")), td(text("Validate with custom error message"))))))
            .build();
    }

    private static ReactElement sectionDatabase() {
        String kotlinCode = """
app {
    services {
        singleton<Database> { DatabaseFactory.create() }
    }

    routes {
        get("/users") {
            val db = service<Database>()
            val result = db.query("SELECT * FROM users")
            val users = result.rows.map { row ->
                json {
                    "id" to row.getInt("id")
                    "name" to row.getString("name")
                    "email" to row.getString("email")
                }
            }
            ok("[${'$'}{users.joinToString(",")}]")
        }

        post("/users") {
            val db = service<Database>()
            val reader = JsonReader.parse(body())
            db.query(
                "INSERT INTO users (name, email) VALUES ($1, $2)",
                reader.getString("name"),
                reader.getString("email")
            )
            created(json { "status" to "created" })
        }
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("Database Extensions"))
            .child(p("Database access in the Kotlin DSL follows the same API as Java. "
                + "Register the Database as a service and retrieve it inside handlers. "
                + "Combine it with the JSON DSL for concise CRUD operations."))
            .child(CodeBlock.create(kotlinCode, "kotlin"))
            .child(Callout.note("Same Database API",
                p("The Kotlin DSL uses the same Database, DbResult, and DbRow classes "
                    + "as the Java API. The only difference is idiomatic Kotlin syntax "
                    + "for iteration and string handling.")))
            .build();
    }
}
