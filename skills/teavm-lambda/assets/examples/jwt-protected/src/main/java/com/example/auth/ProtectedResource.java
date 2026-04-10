package com.example.auth;

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.auth.SecurityContext;
import ca.weblite.teavmlambda.api.json.JsonBuilder;

@Path("/api")
@Component
@Singleton
@RolesAllowed({"user", "admin"})
public class ProtectedResource {

    @GET
    @Path("/me")
    public Response me(SecurityContext ctx) {
        return Response.ok(JsonBuilder.object()
                .put("subject", ctx.getSubject())
                .put("name", ctx.getName())
                .put("admin", ctx.isUserInRole("admin"))
                .build())
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/admin")
    @RolesAllowed({"admin"})
    public Response adminOnly(SecurityContext ctx) {
        return Response.ok(JsonBuilder.object()
                .put("message", "Admin access granted")
                .put("subject", ctx.getSubject())
                .build())
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/public")
    @PermitAll
    public Response publicEndpoint() {
        return Response.ok(JsonBuilder.object()
                .put("message", "This endpoint is public")
                .build())
                .header("Content-Type", "application/json");
    }
}
