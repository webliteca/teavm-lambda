package ca.weblite.teavmlambda.demo.objectstore;

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;

@Path("/health")
public class HealthResource {

    @GET
    public Response health() {
        return Response.ok("{\"status\":\"ok\"}")
                .header("Content-Type", "application/json");
    }
}
