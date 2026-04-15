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

public class AnnotationsPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Annotations Reference"))
            .child(p("teavm-lambda uses compile-time annotations to define routing, "
                + "dependency injection, validation, security, and OpenAPI metadata. "
                + "The annotation processor scans these at build time and generates "
                + "all wiring code -- zero reflection at runtime."))
            .child(sectionRouting())
            .child(sectionDI())
            .child(sectionValidation())
            .child(sectionSecurity())
            .child(sectionOpenAPI())
            .build();
    }

    private static ReactElement sectionRouting() {
        String javaCode = """
@Path("/users")
@Component
@Singleton
public class UserResource {

    @GET
    public Response list(@QueryParam("page") String page) {
        return Response.ok("[]")
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        return Response.ok("{\\"id\\":\\"" + id + "\\"}")
                .header("Content-Type", "application/json");
    }

    @POST
    public Response create(@Body String body,
                           @HeaderParam("Content-Type") String ct) {
        return Response.status(201).body(body)
                .header("Content-Type", "application/json");
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") String id,
                           @Body String body) {
        return Response.ok(body)
                .header("Content-Type", "application/json");
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        return Response.status(204).body("");
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("Routing Annotations"))
            .child(p("Routing annotations map HTTP requests to handler methods. "
                + "They follow JAX-RS conventions so the API feels familiar to "
                + "Java developers."))
            .child(El.table("api-table",
                thead(
                    tr(th("Annotation"), th("Description"))),
                tbody(
                    tr(td(code("@Path(\"/path\")")), td(text("Defines URL path for class or method"))),
                    tr(td(code("@GET")), td(text("HTTP GET method"))),
                    tr(td(code("@POST")), td(text("HTTP POST method"))),
                    tr(td(code("@PUT")), td(text("HTTP PUT method"))),
                    tr(td(code("@DELETE")), td(text("HTTP DELETE method"))),
                    tr(td(code("@PATCH")), td(text("HTTP PATCH method"))),
                    tr(td(code("@HEAD")), td(text("HTTP HEAD method"))),
                    tr(td(code("@OPTIONS")), td(text("HTTP OPTIONS method"))),
                    tr(td(code("@PathParam(\"name\")")), td(text("Extracts path parameter"))),
                    tr(td(code("@QueryParam(\"name\")")), td(text("Extracts query parameter"))),
                    tr(td(code("@HeaderParam(\"name\")")), td(text("Extracts header value"))),
                    tr(td(code("@Body")), td(text("Extracts request body as String"))))))
            .child(h3("Example"))
            .child(p("The following resource demonstrates the most commonly used routing "
                + "annotations together:"))
            .child(CodeBlock.create(javaCode, "java"))
            .child(Callout.note("Path composition",
                p("When @Path is applied to both a class and a method, the paths are "
                    + "concatenated. A class-level @Path(\"/users\") combined with a "
                    + "method-level @Path(\"/{id}\") produces the route /users/{id}.")))
            .build();
    }

    private static ReactElement sectionDI() {
        String javaCode = """
@Component
@Singleton
public class UserService {
    private final UserRepository repo;

    @Inject
    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public String findAll() {
        return repo.findAll();
    }
}

@Repository
public class UserRepository {
    private final Database db;

    @Inject
    public UserRepository(Database db) {
        this.db = db;
    }

    public String findAll() {
        DbResult result = db.query("SELECT * FROM users");
        // ...
        return "[]";
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("Dependency Injection Annotations"))
            .child(p("DI annotations mark classes for the compile-time dependency injection "
                + "container. The annotation processor generates a GeneratedContainer that "
                + "wires all dependencies without reflection."))
            .child(El.table("api-table",
                thead(
                    tr(th("Annotation"), th("Description"))),
                tbody(
                    tr(td(code("@Component")), td(text("Marks class for DI container"))),
                    tr(td(code("@Service")), td(text("Alias for @Component (service layer)"))),
                    tr(td(code("@Repository")), td(text("Alias for @Component (data layer)"))),
                    tr(td(code("@Singleton")), td(text("Single instance across requests"))),
                    tr(td(code("@Inject")), td(text("Constructor injection"))))))
            .child(h3("Example"))
            .child(p("Use @Inject on constructors to declare dependencies. The annotation "
                + "processor resolves the dependency graph at compile time and generates "
                + "the correct instantiation order."))
            .child(CodeBlock.create(javaCode, "java"))
            .child(Callout.note("Singleton vs. default scope",
                p("Without @Singleton, a new instance is created for each injection point. "
                    + "With @Singleton, a single instance is shared across all injection "
                    + "points and requests.")))
            .build();
    }

    private static ReactElement sectionValidation() {
        String javaCode = """
@POST
@Path("/users")
public Response createUser(
        @Body String body,
        @NotNull @NotEmpty @QueryParam("name") String name,
        @Min(0) @Max(150) @QueryParam("age") int age,
        @Pattern(regexp = "^[\\\\w.]+@[\\\\w.]+$")
            @QueryParam("email") String email) {
    // If validation fails, a 400 ProblemDetail is returned automatically
    return Response.status(201).body("{\\"name\\":\\"" + name + "\\"}");
}""";

        return Section.create().className("doc-section")
            .child(h2("Validation Annotations"))
            .child(p("Validation annotations can be applied to handler method parameters. "
                + "When validation fails, the framework automatically returns a 400 Bad "
                + "Request response with RFC 7807 ProblemDetail JSON."))
            .child(El.table("api-table",
                thead(
                    tr(th("Annotation"), th("Description"))),
                tbody(
                    tr(td(code("@NotNull")), td(text("Parameter must not be null"))),
                    tr(td(code("@NotEmpty")), td(text("String must not be empty"))),
                    tr(td(code("@Min(value)")), td(text("Numeric minimum"))),
                    tr(td(code("@Max(value)")), td(text("Numeric maximum"))),
                    tr(td(code("@Pattern(regexp)")), td(text("String must match regex"))))))
            .child(h3("Example"))
            .child(CodeBlock.create(javaCode, "java"))
            .child(Callout.pitfall("Validation order",
                p("Validation annotations are checked in declaration order. If multiple "
                    + "validations fail, only the first failure is reported in the "
                    + "ProblemDetail response.")))
            .build();
    }

    private static ReactElement sectionSecurity() {
        String javaCode = """
@Path("/admin")
@Component
@Singleton
public class AdminResource {

    @GET
    @Path("/dashboard")
    @RolesAllowed({"admin"})
    public Response dashboard() {
        return Response.ok("{\\"section\\":\\"admin\\"}");
    }

    @GET
    @Path("/profile")
    @PermitAll
    public Response profile() {
        return Response.ok("{\\"section\\":\\"profile\\"}");
    }

    @GET
    @Path("/disabled")
    @DenyAll
    public Response disabled() {
        return Response.ok("unreachable");
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("Security Annotations"))
            .child(p("Security annotations control access to handler methods. They work "
                + "with the JWT authentication middleware to enforce role-based access "
                + "control. Unauthenticated or unauthorized requests receive a 401 or "
                + "403 ProblemDetail response."))
            .child(El.table("api-table",
                thead(
                    tr(th("Annotation"), th("Description"))),
                tbody(
                    tr(td(code("@RolesAllowed({\"admin\"})")), td(text("Requires specific roles"))),
                    tr(td(code("@PermitAll")), td(text("Allows all authenticated users"))),
                    tr(td(code("@DenyAll")), td(text("Denies all access"))))))
            .child(h3("Example"))
            .child(CodeBlock.create(javaCode, "java"))
            .child(Callout.note("JWT required",
                p("Security annotations require the teavm-lambda-auth module and JWT "
                    + "middleware to be configured. Without it, the annotations are "
                    + "ignored at runtime. See the Authentication guide for setup "
                    + "instructions.")))
            .build();
    }

    private static ReactElement sectionOpenAPI() {
        String javaCode = """
@Path("/pets")
@Component
@Singleton
@ApiTag(name = "Pets")
public class PetResource {

    @GET
    @ApiOperation(summary = "List all pets")
    @ApiResponse(code = 200, description = "A list of pets")
    public Response list() {
        return Response.ok("[]")
                .header("Content-Type", "application/json");
    }

    @POST
    @ApiOperation(summary = "Create a pet")
    @ApiResponse(code = 201, description = "Pet created")
    @ApiResponse(code = 400, description = "Validation error")
    public Response create(@Body String body) {
        return Response.status(201).body(body)
                .header("Content-Type", "application/json");
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("OpenAPI Annotations"))
            .child(p("OpenAPI annotations generate an OpenAPI 3.0 specification and "
                + "Swagger UI automatically. The annotation processor reads these "
                + "at compile time and produces a /openapi.json endpoint and a "
                + "/swagger-ui path."))
            .child(El.table("api-table",
                thead(
                    tr(th("Annotation"), th("Description"))),
                tbody(
                    tr(td(code("@ApiInfo(title, version)")), td(text("API metadata"))),
                    tr(td(code("@ApiTag(name)")), td(text("Groups operations"))),
                    tr(td(code("@ApiOperation(summary)")), td(text("Operation description"))),
                    tr(td(code("@ApiResponse(code, description)")), td(text("Response documentation"))))))
            .child(h3("Example"))
            .child(CodeBlock.create(javaCode, "java"))
            .child(Callout.note("Swagger UI",
                p("When OpenAPI annotations are present, the framework automatically "
                    + "serves Swagger UI at /swagger-ui. The generated OpenAPI spec "
                    + "is available at /openapi.json.")))
            .build();
    }
}
