package io.teavmlambda.demo.cloudrun;

import io.teavmlambda.core.Response;
import io.teavmlambda.core.annotation.GET;
import io.teavmlambda.core.annotation.Path;

@Path("/health")
public class HealthResource {

    @GET
    public Response health() {
        return Response.ok("{\"status\":\"ok\"}")
                .header("Content-Type", "application/json");
    }
}
