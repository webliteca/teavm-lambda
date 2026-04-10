package com.example.crud;

import ca.weblite.teavmlambda.api.ProblemDetail;
import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.json.JsonBuilder;
import ca.weblite.teavmlambda.api.json.JsonReader;
import com.example.crud.repository.UserRepository;

import java.util.UUID;

@Path("/users")
@Component
@Singleton
public class UsersResource {

    private final UserRepository repo;

    @Inject
    public UsersResource(UserRepository repo) {
        this.repo = repo;
    }

    @GET
    public Response list() {
        return Response.ok(repo.findAllJson())
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") String id) {
        String json = repo.findByIdJson(id);
        if (json == null) {
            return ProblemDetail.notFound("User " + id + " not found").toResponse();
        }
        return Response.ok(json)
                .header("Content-Type", "application/json");
    }

    @POST
    public Response create(@NotNull @Body String body) {
        JsonReader json = JsonReader.parse(body);
        String name = json.getString("name");
        String email = json.getString("email");
        if (name == null || name.isEmpty()) {
            return ProblemDetail.badRequest("name is required").toResponse();
        }
        if (email == null || email.isEmpty()) {
            return ProblemDetail.badRequest("email is required").toResponse();
        }

        String id = UUID.randomUUID().toString();
        repo.create(id, name, email);

        return Response.status(201)
                .header("Content-Type", "application/json")
                .body(JsonBuilder.object()
                        .put("id", id)
                        .put("name", name)
                        .put("email", email)
                        .build());
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        boolean deleted = repo.deleteById(id);
        if (!deleted) {
            return ProblemDetail.notFound("User " + id + " not found").toResponse();
        }
        return Response.status(204);
    }
}
