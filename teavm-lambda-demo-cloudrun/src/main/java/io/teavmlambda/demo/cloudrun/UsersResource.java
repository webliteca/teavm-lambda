package io.teavmlambda.demo.cloudrun;

import io.teavmlambda.core.Response;
import io.teavmlambda.core.annotation.*;
import io.teavmlambda.db.api.Database;
import io.teavmlambda.db.api.DbResult;
import io.teavmlambda.db.api.DbRow;
import io.teavmlambda.db.api.Json;

@Path("/users")
@ApiTag(value = "Users", description = "User management operations")
@ApiInfo(title = "TeaVM Lambda Cloud Run Demo API", version = "0.1.0", description = "Demo REST API built with TeaVM Lambda for Cloud Run")
public class UsersResource {

    private final Database db;

    public UsersResource(Database db) {
        this.db = db;
    }

    @GET
    @ApiOperation(summary = "List all users", description = "Returns a list of all users in the system")
    @ApiResponse(code = 200, description = "List of users")
    public Response listUsers() {
        DbResult result = db.query("SELECT id, name, email FROM users ORDER BY id");
        return Response.ok(result.toJsonArray())
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{id}")
    @ApiOperation(summary = "Get user by ID", description = "Returns a single user by their ID")
    @ApiResponse(code = 200, description = "User found")
    @ApiResponse(code = 404, description = "User not found")
    public Response getUser(@PathParam("id") String id) {
        DbResult result = db.query("SELECT id, name, email FROM users WHERE id = $1", id);
        if (result.getRowCount() == 0) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"User not found\"}");
        }
        return Response.ok(result.getRows().get(0).toJson())
                .header("Content-Type", "application/json");
    }

    @POST
    @ApiOperation(summary = "Create a new user", description = "Creates a user with the given name and email")
    @ApiResponse(code = 201, description = "User created")
    public Response createUser(@Body String body) {
        DbRow parsed = Json.parse(body);
        String name = parsed.getString("name");
        String email = parsed.getString("email");
        DbResult result = db.query(
                "INSERT INTO users (name, email) VALUES ($1, $2) RETURNING id, name, email",
                name, email);
        return Response.status(201)
                .header("Content-Type", "application/json")
                .body(result.getRows().get(0).toJson());
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(summary = "Delete a user", description = "Deletes a user by their ID")
    @ApiResponse(code = 204, description = "User deleted")
    @ApiResponse(code = 404, description = "User not found")
    public Response deleteUser(@PathParam("id") String id) {
        DbResult result = db.query("DELETE FROM users WHERE id = $1 RETURNING id", id);
        if (result.getRowCount() == 0) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"User not found\"}");
        }
        return Response.status(204);
    }
}
