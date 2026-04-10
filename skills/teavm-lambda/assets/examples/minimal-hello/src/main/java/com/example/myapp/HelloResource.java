package com.example.myapp;

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.json.JsonBuilder;

@Path("/hello")
@Component
@Singleton
public class HelloResource {

    @GET
    public Response hello() {
        return Response.ok(JsonBuilder.object().put("message", "Hello, World!").build())
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{name}")
    public Response helloName(@PathParam("name") String name) {
        return Response.ok(JsonBuilder.object().put("message", "Hello, " + name + "!").build())
                .header("Content-Type", "application/json");
    }
}
