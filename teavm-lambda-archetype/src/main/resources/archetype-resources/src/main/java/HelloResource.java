package ${package};

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;

@Path("/hello")
@Component
@Singleton
public class HelloResource {

    @GET
    public Response hello() {
        return Response.ok("{\"message\":\"Hello, World!\"}")
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{name}")
    public Response helloName(@PathParam("name") String name) {
        return Response.ok("{\"message\":\"Hello, " + name + "!\"}")
                .header("Content-Type", "application/json");
    }
}
