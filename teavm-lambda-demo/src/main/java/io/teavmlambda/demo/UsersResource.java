package io.teavmlambda.demo;

import io.teavmlambda.core.Request;
import io.teavmlambda.core.Response;
import io.teavmlambda.core.annotation.*;
import io.teavmlambda.db.Db;
import io.teavmlambda.db.JsUtil;
import io.teavmlambda.db.PgResult;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;

@Path("/users")
public class UsersResource {

    private final Db db;

    public UsersResource(Db db) {
        this.db = db;
    }

    @GET
    public Response listUsers() {
        PgResult result = db.query("SELECT id, name, email FROM users ORDER BY id");
        JSArray<JSObject> rows = result.getRows();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < rows.getLength(); i++) {
            if (i > 0) json.append(",");
            json.append(JsUtil.toJson(rows.get(i)));
        }
        json.append("]");
        return Response.ok(json.toString())
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") String id) {
        PgResult result = db.query("SELECT id, name, email FROM users WHERE id = $1", id);
        if (result.getRowCount() == 0) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"User not found\"}");
        }
        return Response.ok(JsUtil.toJson(result.getRows().get(0)))
                .header("Content-Type", "application/json");
    }

    @POST
    public Response createUser(@Body String body) {
        JSObject parsed = JsUtil.parseJson(body);
        String name = JsUtil.getStringProperty(parsed, "name");
        String email = JsUtil.getStringProperty(parsed, "email");
        PgResult result = db.query(
                "INSERT INTO users (name, email) VALUES ($1, $2) RETURNING id, name, email",
                name, email);
        return Response.status(201)
                .header("Content-Type", "application/json")
                .body(JsUtil.toJson(result.getRows().get(0)));
    }

    @DELETE
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") String id) {
        PgResult result = db.query("DELETE FROM users WHERE id = $1 RETURNING id", id);
        if (result.getRowCount() == 0) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"User not found\"}");
        }
        return Response.status(204);
    }
}
