package ca.weblite.teavmlambda.demo.auth;

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;

@Path("/health")
@Component
@Singleton
@ApiTag(value = "Health", description = "Health check endpoint")
public class HealthResource {

    @GET
    @PermitAll
    @ApiOperation(summary = "Health check", description = "Returns service health status. No authentication required.")
    @ApiResponse(code = 200, description = "Service is healthy")
    public Response health() {
        return Response.ok("{\"status\":\"ok\"}")
                .header("Content-Type", "application/json");
    }
}
