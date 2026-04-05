package ca.weblite.teavmlambda.demo.nosqldb;

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.nosqldb.JsUtil;
import ca.weblite.teavmlambda.api.nosqldb.NoSqlClient;
import org.teavm.jso.JSObject;

import java.util.List;

@Path("/users")
@ApiTag(value = "Users", description = "User management operations")
@ApiInfo(title = "TeaVM Lambda NoSQL Demo API", version = "0.1.0", description = "Demo REST API built with TeaVM Lambda using NoSQL storage")
public class UsersResource {

    private static final String COLLECTION = "users";

    private final NoSqlClient client;

    public UsersResource(NoSqlClient client) {
        this.client = client;
    }

    @GET
    @ApiOperation(summary = "List all users", description = "Returns a list of all users in the system")
    @ApiResponse(code = 200, description = "List of users")
    public Response listUsers() {
        List<String> docs = client.list(COLLECTION);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < docs.size(); i++) {
            if (i > 0) json.append(",");
            json.append(docs.get(i));
        }
        json.append("]");
        return Response.ok(json.toString())
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{id}")
    @ApiOperation(summary = "Get user by ID", description = "Returns a single user by their ID")
    @ApiResponse(code = 200, description = "User found")
    @ApiResponse(code = 404, description = "User not found")
    public Response getUser(@PathParam("id") String id) {
        String doc = client.get(COLLECTION, id);
        if (doc == null) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"User not found\"}");
        }
        return Response.ok(doc)
                .header("Content-Type", "application/json");
    }

    @POST
    @ApiOperation(summary = "Create a new user", description = "Creates a user with the given ID, name, and email")
    @ApiResponse(code = 201, description = "User created")
    public Response createUser(@Body String body) {
        JSObject parsed = JsUtil.parseJson(body);
        String id = JsUtil.getStringProperty(parsed, "id");
        client.put(COLLECTION, id, body);
        return Response.status(201)
                .header("Content-Type", "application/json")
                .body(body);
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(summary = "Delete a user", description = "Deletes a user by their ID")
    @ApiResponse(code = 204, description = "User deleted")
    @ApiResponse(code = 404, description = "User not found")
    public Response deleteUser(@PathParam("id") String id) {
        String existing = client.get(COLLECTION, id);
        if (existing == null) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"User not found\"}");
        }
        client.delete(COLLECTION, id);
        return Response.status(204);
    }
}
