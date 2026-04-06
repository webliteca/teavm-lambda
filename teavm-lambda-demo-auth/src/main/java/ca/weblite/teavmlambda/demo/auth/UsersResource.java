package ca.weblite.teavmlambda.demo.auth;

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.auth.SecurityContext;
import ca.weblite.teavmlambda.demo.auth.dto.CreateUserRequest;
import ca.weblite.teavmlambda.demo.auth.entity.User;
import ca.weblite.teavmlambda.demo.auth.service.UserService;

/**
 * User management resource with role-based access control.
 * <p>
 * Class-level {@code @RolesAllowed} requires authentication for all methods.
 * Individual methods can override with {@code @PermitAll} or narrow roles.
 */
@Path("/users")
@Component
@Singleton
@RolesAllowed({"admin", "user"})
@ApiTag(value = "Users", description = "User management (requires authentication)")
@ApiInfo(title = "TeaVM Lambda Auth Demo API", version = "0.1.0",
        description = "Demo REST API with JWT authentication")
public class UsersResource {

    private final UserService userService;

    @Inject
    public UsersResource(UserService userService) {
        this.userService = userService;
    }

    @GET
    @ApiOperation(summary = "List all users", description = "Returns all users. Requires 'admin' or 'user' role.")
    @ApiResponse(code = 200, description = "List of users")
    @ApiResponse(code = 401, description = "Missing or invalid token")
    public Response listUsers(SecurityContext securityContext) {
        return Response.ok(User.toJsonArray(userService.listUsers()))
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{id}")
    @ApiOperation(summary = "Get user by ID", description = "Returns a single user. Requires 'admin' or 'user' role.")
    @ApiResponse(code = 200, description = "User found")
    @ApiResponse(code = 401, description = "Missing or invalid token")
    @ApiResponse(code = 404, description = "User not found")
    public Response getUser(@PathParam("id") String id) {
        User user = userService.getUser(id);
        if (user == null) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"User not found\"}");
        }
        return Response.ok(user.toJson())
                .header("Content-Type", "application/json");
    }

    @POST
    @RolesAllowed({"admin"})
    @ApiOperation(summary = "Create a new user", description = "Creates a user. Requires 'admin' role.")
    @ApiResponse(code = 201, description = "User created")
    @ApiResponse(code = 401, description = "Missing or invalid token")
    @ApiResponse(code = 403, description = "Insufficient role")
    public Response createUser(@Body String body) {
        CreateUserRequest request = CreateUserRequest.fromJson(body);
        User created = userService.createUser(request);
        return Response.status(201)
                .header("Content-Type", "application/json")
                .body(created.toJson());
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"admin"})
    @ApiOperation(summary = "Delete a user", description = "Deletes a user. Requires 'admin' role.")
    @ApiResponse(code = 204, description = "User deleted")
    @ApiResponse(code = 401, description = "Missing or invalid token")
    @ApiResponse(code = 403, description = "Insufficient role")
    @ApiResponse(code = 404, description = "User not found")
    public Response deleteUser(@PathParam("id") String id) {
        if (!userService.deleteUser(id)) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"User not found\"}");
        }
        return Response.status(204);
    }

    @GET
    @Path("/me")
    @ApiOperation(summary = "Get current user info", description = "Returns the authenticated user's identity from the JWT token.")
    @ApiResponse(code = 200, description = "Current user info")
    @ApiResponse(code = 401, description = "Missing or invalid token")
    public Response me(SecurityContext securityContext) {
        return Response.ok("{\"subject\":\"" + escapeJson(securityContext.getSubject())
                        + "\",\"name\":\"" + escapeJson(securityContext.getName()) + "\"}")
                .header("Content-Type", "application/json");
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
