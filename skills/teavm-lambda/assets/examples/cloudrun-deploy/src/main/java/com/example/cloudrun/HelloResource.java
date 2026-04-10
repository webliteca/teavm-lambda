package com.example.cloudrun;

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.json.JsonBuilder;

@Path("/hello")
@Component
@Singleton
public class HelloResource {

    @GET
    public Response hello() {
        return Response.ok(JsonBuilder.object().put("message", "Hello from Cloud Run!").build())
                .header("Content-Type", "application/json");
    }
}
