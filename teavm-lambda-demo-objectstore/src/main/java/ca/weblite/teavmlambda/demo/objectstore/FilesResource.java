package ca.weblite.teavmlambda.demo.objectstore;

import ca.weblite.teavmlambda.api.Request;
import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClient;

import java.util.List;

@Path("/files")
public class FilesResource {

    private final ObjectStoreClient client;
    private final String bucket;

    public FilesResource(ObjectStoreClient client, String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    @GET
    public Response listFiles(@QueryParam("prefix") String prefix) {
        if (prefix == null) prefix = "";
        List<String> keys = client.listObjects(bucket, prefix);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(escapeJson(keys.get(i))).append("\"");
        }
        json.append("]");
        return Response.ok(json.toString())
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{key}")
    public Response getFile(@PathParam("key") String key) {
        String data = client.getObject(bucket, key);
        if (data == null) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"File not found\"}");
        }
        return Response.ok(data)
                .header("Content-Type", "application/octet-stream");
    }

    @PUT
    @Path("/{key}")
    public Response putFile(@PathParam("key") String key, @Body String body) {
        client.putObject(bucket, key, body, "application/octet-stream");
        return Response.status(201)
                .header("Content-Type", "application/json")
                .body("{\"key\":\"" + escapeJson(key) + "\",\"status\":\"created\"}");
    }

    @DELETE
    @Path("/{key}")
    public Response deleteFile(@PathParam("key") String key) {
        if (!client.objectExists(bucket, key)) {
            return Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("{\"error\":\"File not found\"}");
        }
        client.deleteObject(bucket, key);
        return Response.status(204);
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
