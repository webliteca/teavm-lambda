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

public class RoutingPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Routing"))
            .child(p("teavm-lambda uses JAX-RS-style annotations to define HTTP routes. "
                + "The annotation processor scans your resource classes at compile time "
                + "and generates all routing logic in the GeneratedRouter class. No "
                + "reflection is used at runtime."))
            .child(sectionPathAnnotation())
            .child(sectionPathParameters())
            .child(sectionQueryParameters())
            .child(sectionHeaderParameters())
            .child(sectionRequestBody())
            .build();
    }

    private static ReactElement sectionPathAnnotation() {
        String javaCode = """
                @Path("/users")
                @Component
                @Singleton
                public class UserResource {

                    @GET
                    public Response list() {
                        return Response.ok("{\\"users\\":[]}")
                                .header("Content-Type", "application/json");
                    }

                    @POST
                    public Response create(@Body String body) {
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
                        return Response.status(204).build();
                    }
                }""";

        String kotlinCode = """
                app {
                    routes {
                        get("/users") {
                            ok(json { "users" to emptyList<Any>() })
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
                    }
                }""";

        return Section.create().className("doc-section")
            .child(h2("@Path and HTTP Method Annotations"))
            .child(p("The @Path annotation on a class sets the base path for all routes in "
                + "that resource. Method-level @Path annotations are appended to the class "
                + "path. Use @GET, @POST, @PUT, and @DELETE to map methods to HTTP verbs."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(p("Each annotated method must return a Response object. The annotation "
                + "processor generates the routing table at compile time, so there is no "
                + "runtime cost for route resolution."))
            .build();
    }

    private static ReactElement sectionPathParameters() {
        String javaCode = """
                @GET
                @Path("/{id}")
                public Response getUser(@PathParam("id") String id) {
                    return Response.ok("{\\"id\\":\\"" + id + "\\"}")
                            .header("Content-Type", "application/json");
                }

                @GET
                @Path("/{userId}/posts/{postId}")
                public Response getPost(@PathParam("userId") String userId,
                                        @PathParam("postId") String postId) {
                    return Response.ok("{\\"userId\\":\\"" + userId
                            + "\\",\\"postId\\":\\"" + postId + "\\"}")
                            .header("Content-Type", "application/json");
                }""";

        String kotlinCode = """
                get("/users/{id}") {
                    val id = pathParam("id")
                    ok(json { "id" to id })
                }

                get("/users/{userId}/posts/{postId}") {
                    val userId = pathParam("userId")
                    val postId = pathParam("postId")
                    ok(json {
                        "userId" to userId
                        "postId" to postId
                    })
                }""";

        return Section.create().className("doc-section")
            .child(h2("Path Parameters"))
            .child(p("Use curly braces in the @Path template to define path parameters. "
                + "Annotate the corresponding method parameter with @PathParam to extract "
                + "the value. Path parameters are always passed as strings."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(p("You can include multiple path parameters in a single route. Each "
                + "parameter name in the template must match the name in the @PathParam "
                + "annotation."))
            .build();
    }

    private static ReactElement sectionQueryParameters() {
        String javaCode = """
                @GET
                public Response list(@QueryParam("page") String page,
                                     @QueryParam("limit") String limit) {
                    int p = page != null ? Integer.parseInt(page) : 1;
                    int l = limit != null ? Integer.parseInt(limit) : 20;
                    return Response.ok("{\\"page\\":" + p + ",\\"limit\\":" + l + "}")
                            .header("Content-Type", "application/json");
                }""";

        String kotlinCode = """
                get("/users") {
                    val page = queryParam("page")?.toIntOrNull() ?: 1
                    val limit = queryParam("limit")?.toIntOrNull() ?: 20
                    ok(json {
                        "page" to page
                        "limit" to limit
                    })
                }""";

        return Section.create().className("doc-section")
            .child(h2("Query Parameters"))
            .child(p("Use @QueryParam to extract query string parameters from the URL. "
                + "Query parameters are optional by default and will be null if not "
                + "provided in the request. All query parameter values are strings, "
                + "so you must parse them to the desired type yourself."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(p("For example, a request to /users?page=2&limit=50 will pass "
                + "\"2\" and \"50\" as the page and limit parameters respectively."))
            .build();
    }

    private static ReactElement sectionHeaderParameters() {
        String javaCode = """
                @GET
                @Path("/me")
                public Response me(@HeaderParam("Authorization") String auth) {
                    if (auth == null || !auth.startsWith("Bearer ")) {
                        return Response.status(401)
                                .body("{\\"error\\":\\"Missing token\\"}")
                                .header("Content-Type", "application/json");
                    }
                    String token = auth.substring(7);
                    return Response.ok("{\\"token\\":\\"" + token + "\\"}")
                            .header("Content-Type", "application/json");
                }""";

        String kotlinCode = """
                get("/me") {
                    val auth = header("Authorization")
                    if (auth == null || !auth.startsWith("Bearer ")) {
                        return@get unauthorized(
                            json { "error" to "Missing token" }
                        )
                    }
                    val token = auth.substring(7)
                    ok(json { "token" to token })
                }""";

        return Section.create().className("doc-section")
            .child(h2("Header Parameters"))
            .child(p("Use @HeaderParam to extract HTTP header values from the request. "
                + "Like query parameters, header values are strings and will be null "
                + "if the header is not present. Header names are case-insensitive as "
                + "defined by the HTTP specification."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionRequestBody() {
        String javaCode = """
                @POST
                public Response create(@Body String body) {
                    JsonReader reader = new JsonReader(body);
                    String name = reader.getString("name");
                    String email = reader.getString("email");
                    // Process the parsed fields...
                    return Response.status(201)
                            .body(new JsonBuilder()
                                    .put("name", name)
                                    .put("email", email)
                                    .build())
                            .header("Content-Type", "application/json");
                }""";

        String kotlinCode = """
                post("/users") {
                    val body = body()
                    val reader = JsonReader(body)
                    val name = reader.getString("name")
                    val email = reader.getString("email")
                    // Process the parsed fields...
                    created(json {
                        "name" to name
                        "email" to email
                    })
                }""";

        return Section.create().className("doc-section")
            .child(h2("Request Body"))
            .child(p("Use @Body to inject the raw request body as a String parameter. "
                + "This is typically used with POST and PUT methods to receive JSON "
                + "payloads from the client."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(Callout.note("@Body accepts String only",
                p("The @Body annotation only supports String parameters. To parse JSON "
                    + "request bodies, use JsonReader to extract individual fields. "
                    + "There is no automatic deserialization to POJOs -- this keeps the "
                    + "framework lightweight and compatible with TeaVM compilation.")))
            .build();
    }
}
