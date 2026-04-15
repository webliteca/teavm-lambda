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

public class SecurityPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Security"))
            .child(p("teavm-lambda provides built-in JWT authentication and role-based "
                + "access control. Secure your endpoints with annotations and access "
                + "the authenticated user via SecurityContext. The JWT middleware "
                + "validates tokens automatically on every request."))
            .child(sectionJwtAuthentication())
            .child(sectionRoleAnnotations())
            .child(sectionSecurityContext())
            .child(sectionEnvironmentVariables())
            .child(sectionFullExample())
            .build();
    }

    private static ReactElement sectionJwtAuthentication() {
        String javaCode = """
var container = new GeneratedContainer();
var router = new GeneratedRouter(container);
var app = new MiddlewareRouter(router);

// Add JWT authentication middleware
app.use(new JwtValidator());
app.use(new CorsMiddleware());

Platform.start(app);""";

        String kotlinCode = """
app {
    jwt()
    cors()
    routes { /* ... */ }
}""";

        return Section.create().className("doc-section")
            .child(h2("JWT Authentication"))
            .child(p("Add the JwtValidator middleware to your MiddlewareRouter to enable "
                + "JWT authentication. The validator extracts the Bearer token from the "
                + "Authorization header, verifies the signature and expiration, and "
                + "populates the SecurityContext for downstream handlers."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionRoleAnnotations() {
        String javaCode = """
@Path("/admin")
@Component
@Singleton
public class AdminResource {

    @GET
    @RolesAllowed("admin")
    public Response dashboard(SecurityContext ctx) {
        String userId = ctx.getSubject();
        return Response.ok("{\\"user\\":\\"" + userId + "\\"}");
    }

    @GET
    @Path("/public")
    @PermitAll
    public Response publicInfo() {
        return Response.ok("{\\"info\\":\\"public\\"}");
    }

    @POST
    @Path("/shutdown")
    @DenyAll
    public Response shutdown() {
        return Response.status(403).body("{\\"error\\":\\"forbidden\\"}");
    }
}""";

        String kotlinCode = """
app {
    routes {
        get("/admin", roles = listOf("admin")) { ctx ->
            val userId = ctx.subject
            ok(json { "user" to userId })
        }

        get("/admin/public", permitAll = true) {
            ok(json { "info" to "public" })
        }

        post("/admin/shutdown", denyAll = true) {
            status(403, json { "error" to "forbidden" })
        }
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("Role-Based Access Control"))
            .child(p("Use JAX-RS security annotations to restrict access to endpoints. "
                + "The JWT middleware checks the roles claim in the token against the "
                + "required roles on the endpoint."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Annotation"),
                            El.classedText("th", "", "Effect")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("@RolesAllowed(\"role\")")),
                            El.classedText("td", "", "Requires the JWT to contain the specified role(s) in its roles claim")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("@PermitAll")),
                            El.classedText("td", "", "Allows access to any authenticated or unauthenticated request")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("@DenyAll")),
                            El.classedText("td", "", "Denies access to all requests regardless of authentication")
                        )
                    )
                )
            ))
            .build();
    }

    private static ReactElement sectionSecurityContext() {
        String javaCode = """
@GET
@RolesAllowed({"user", "admin"})
public Response profile(SecurityContext ctx) {
    // Subject is the "sub" claim from the JWT
    String userId = ctx.getSubject();

    // Check if user has a specific role
    boolean isAdmin = ctx.isInRole("admin");

    // Get the raw JWT claims
    Map<String, Object> claims = ctx.getClaims();
    String email = (String) claims.get("email");

    return Response.ok("{\\"userId\\":\\"" + userId + "\\","
        + "\\"admin\\":" + isAdmin + ","
        + "\\"email\\":\\"" + email + "\\"}");
}""";

        String kotlinCode = """
get("/profile", roles = listOf("user", "admin")) { ctx ->
    // Subject is the "sub" claim from the JWT
    val userId = ctx.subject

    // Check if user has a specific role
    val isAdmin = ctx.isInRole("admin")

    // Get the raw JWT claims
    val email = ctx.claims["email"] as String

    ok(json {
        "userId" to userId
        "admin" to isAdmin
        "email" to email
    })
}""";

        return Section.create().className("doc-section")
            .child(h2("SecurityContext"))
            .child(p("The SecurityContext is injected into handler methods that accept it "
                + "as a parameter. It provides access to the authenticated user's identity, "
                + "roles, and raw JWT claims."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Method"),
                            El.classedText("th", "", "Return Type"),
                            El.classedText("th", "", "Description")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("getSubject()")),
                            El.classed("td", "", code("String")),
                            El.classedText("td", "", "The \"sub\" claim -- typically a user ID")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("isInRole(role)")),
                            El.classed("td", "", code("boolean")),
                            El.classedText("td", "", "Whether the user has the given role")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getClaims()")),
                            El.classed("td", "", code("Map<String, Object>")),
                            El.classedText("td", "", "All decoded JWT claims as a map")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getRoles()")),
                            El.classed("td", "", code("Set<String>")),
                            El.classedText("td", "", "All roles from the JWT roles claim")
                        )
                    )
                )
            ))
            .build();
    }

    private static ReactElement sectionEnvironmentVariables() {
        return Section.create().className("doc-section")
            .child(h2("Configuration"))
            .child(p("JWT validation is configured via environment variables. Set these "
                + "in your deployment environment, docker-compose.yml, or .env file."))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Variable"),
                            El.classedText("th", "", "Required"),
                            El.classedText("th", "", "Description")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("JWT_SECRET")),
                            El.classedText("td", "", "Yes*"),
                            El.classedText("td", "", "HMAC shared secret for HS256/HS384/HS512 algorithms")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("JWT_ALGORITHM")),
                            El.classedText("td", "", "No"),
                            El.classedText("td", "", "Algorithm (HS256, RS256, etc.). Defaults to HS256")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("JWT_PUBLIC_KEY")),
                            El.classedText("td", "", "Yes*"),
                            El.classedText("td", "", "PEM-encoded public key for RS256/RS384/RS512 algorithms")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("JWT_ISSUER")),
                            El.classedText("td", "", "No"),
                            El.classedText("td", "", "Expected issuer (\"iss\" claim). Validated if set")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("FIREBASE_PROJECT_ID")),
                            El.classedText("td", "", "No"),
                            El.classedText("td", "", "Firebase project ID for Firebase Auth JWT validation")
                        )
                    )
                )
            ))
            .child(Callout.note("Choosing a secret type",
                p("Set JWT_SECRET for HMAC algorithms (HS256) or JWT_PUBLIC_KEY for "
                    + "RSA algorithms (RS256). For Firebase Authentication, set "
                    + "FIREBASE_PROJECT_ID and the validator will automatically fetch "
                    + "Google's public keys.")))
            .build();
    }

    private static ReactElement sectionFullExample() {
        String javaCode = """
@Path("/api")
@Component
@Singleton
public class ApiResource {

    @GET
    @Path("/public")
    @PermitAll
    public Response publicEndpoint() {
        return Response.ok("{\\"message\\":\\"anyone can see this\\"}");
    }

    @GET
    @Path("/me")
    @RolesAllowed("user")
    public Response currentUser(SecurityContext ctx) {
        return Response.ok("{\\"userId\\":\\"" + ctx.getSubject() + "\\"}");
    }

    @DELETE
    @Path("/users/{id}")
    @RolesAllowed("admin")
    public Response deleteUser(
            @PathParam("id") String id,
            SecurityContext ctx) {
        // Only admins can delete users
        return Response.ok("{\\"deleted\\":\\"" + id + "\\"}");
    }
}""";

        String kotlinCode = """
app {
    jwt()
    cors()
    routes {
        get("/api/public", permitAll = true) {
            ok(json { "message" to "anyone can see this" })
        }

        get("/api/me", roles = listOf("user")) { ctx ->
            ok(json { "userId" to ctx.subject })
        }

        delete("/api/users/{id}", roles = listOf("admin")) { ctx ->
            val id = pathParam("id")
            ok(json { "deleted" to id })
        }
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("Full Example"))
            .child(p("Here is a complete resource class demonstrating public, "
                + "authenticated, and admin-only endpoints:"))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(Callout.pitfall("Middleware order",
                p("JwtValidator must be registered before your router in the "
                    + "middleware pipeline. If it comes after, the SecurityContext "
                    + "will not be populated and @RolesAllowed checks will fail.")))
            .build();
    }
}
