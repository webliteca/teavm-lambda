package io.teavmlambda.demo;

import io.teavmlambda.core.Response;
import io.teavmlambda.core.annotation.*;

@Path("/health")
@ApiTag(value = "Health", description = "Health check endpoint")
public class HealthResource {

    @GET
    @ApiOperation(summary = "Health check", description = "Returns the health status of the service")
    @ApiResponse(code = 200, description = "Service is healthy")
    public Response health() {
        return Response.ok("{\"status\":\"ok\"}")
                .header("Content-Type", "application/json");
    }
}
