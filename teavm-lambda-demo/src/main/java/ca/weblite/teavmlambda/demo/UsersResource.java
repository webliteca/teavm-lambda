package ca.weblite.teavmlambda.demo;

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.demo.dto.CreateUserRequest;
import ca.weblite.teavmlambda.demo.entity.User;
import ca.weblite.teavmlambda.demo.service.UserService;

@Path("/users")
@Component
@Singleton
@ApiTag(value = "Users", description = "User management operations")
@ApiInfo(title = "TeaVM Lambda Demo API", version = "0.1.0", description = "Demo REST API built with TeaVM Lambda")
public class UsersResource {

    private final UserService userService;

    @Inject
    public UsersResource(UserService userService) {
        this.userService = userService;
    }

    @GET
    @ApiOperation(summary = "List all users", description = "Returns a list of all users in the system")
    @ApiResponse(code = 200, description = "List of users")
    public Response listUsers() {
        return Response.ok(User.toJsonArray(userService.listUsers()))
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{id}")
    @ApiOperation(summary = "Get user by ID", description = "Returns a single user by their ID")
    @ApiResponse(code = 200, description = "User found")
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
    @ApiOperation(summary = "Create a new user", description = "Creates a user with the given name and email")
    @ApiResponse(code = 201, description = "User created")
    public Response createUser(@Body String body) {
        CreateUserRequest request = CreateUserRequest.fromJson(body);
        User created = userService.createUser(request);
        return Response.status(201)
                .header("Content-Type", "application/json")
                .body(created.toJson());
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(summary = "Delete a user", description = "Deletes a user by their ID")
    @ApiResponse(code = 204, description = "User deleted")
    @ApiResponse(code = 404, description = "User not found")
    public Response deleteUser(@PathParam("id") String id) {
        if (!userService.deleteUser(id)) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"User not found\"}");
        }
        return Response.status(204);
    }
}
