package io.teavmlambda.demo.nosqldb;

import io.teavmlambda.core.Response;
import io.teavmlambda.core.annotation.*;
import io.teavmlambda.nosqldb.JsUtil;
import io.teavmlambda.nosqldb.NoSqlClient;
import org.teavm.jso.JSObject;

import java.util.List;

@Path("/users")
public class UsersResource {

    private static final String COLLECTION = "users";

    private final NoSqlClient client;

    public UsersResource(NoSqlClient client) {
        this.client = client;
    }

    @GET
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
