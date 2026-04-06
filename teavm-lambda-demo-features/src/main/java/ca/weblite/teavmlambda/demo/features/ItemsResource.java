package ca.weblite.teavmlambda.demo.features;

import ca.weblite.teavmlambda.api.ProblemDetail;
import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.json.JsonBuilder;
import ca.weblite.teavmlambda.demo.features.dto.CreateItemRequest;
import ca.weblite.teavmlambda.demo.features.dto.UpdateItemRequest;
import ca.weblite.teavmlambda.demo.features.entity.Item;
import ca.weblite.teavmlambda.demo.features.service.ItemService;

import java.util.List;

/**
 * Items REST resource demonstrating new framework features:
 * <ul>
 *   <li>PATCH method for partial updates</li>
 *   <li>@HeaderParam for custom header binding</li>
 *   <li>@NotNull/@NotEmpty validation on parameters</li>
 *   <li>ProblemDetail for structured error responses</li>
 *   <li>JsonBuilder for JSON serialization</li>
 * </ul>
 */
@Path("/items")
@Component
@Singleton
@ApiTag(value = "Items", description = "Item management endpoints")
@ApiInfo(title = "Features Demo API", version = "1.0.0",
        description = "Demo API showcasing teavm-lambda framework features")
public class ItemsResource {

    private final ItemService service;

    @Inject
    public ItemsResource(ItemService service) {
        this.service = service;
    }

    @GET
    @ApiOperation(summary = "List all items")
    @ApiResponse(code = 200, description = "List of items", mediaType = "application/json")
    public Response list(@HeaderParam("X-Request-Id") String requestId) {
        List<Item> items = service.listItems();
        Response response = Response.ok(Item.toJsonArray(items))
                .header("Content-Type", "application/json");
        if (requestId != null) {
            response = response.header("X-Request-Id", requestId);
        }
        return response;
    }

    @GET
    @Path("/{id}")
    @ApiOperation(summary = "Get item by ID")
    @ApiResponse(code = 200, description = "Item details", mediaType = "application/json")
    @ApiResponse(code = 404, description = "Item not found")
    public Response getById(@PathParam("id") String id) {
        Item item = service.getItem(id);
        if (item == null) {
            return ProblemDetail.notFound("Item with id " + id + " was not found").toResponse();
        }
        return Response.ok(item.toJson())
                .header("Content-Type", "application/json");
    }

    @POST
    @ApiOperation(summary = "Create a new item")
    @ApiResponse(code = 201, description = "Item created", mediaType = "application/json")
    @ApiResponse(code = 400, description = "Validation error")
    public Response create(@NotNull @Body String body) {
        CreateItemRequest request = CreateItemRequest.fromJson(body);
        if (request.getName() == null || request.getName().isEmpty()) {
            return ProblemDetail.badRequest("name is required").toResponse();
        }
        Item item = service.createItem(request);
        return Response.status(201)
                .header("Content-Type", "application/json")
                .body(item.toJson());
    }

    @PATCH
    @Path("/{id}")
    @ApiOperation(summary = "Partially update an item")
    @ApiResponse(code = 200, description = "Item updated", mediaType = "application/json")
    @ApiResponse(code = 404, description = "Item not found")
    public Response patch(@PathParam("id") String id, @NotNull @Body String body) {
        Item existing = service.getItem(id);
        if (existing == null) {
            return ProblemDetail.notFound("Item with id " + id + " was not found").toResponse();
        }
        UpdateItemRequest request = UpdateItemRequest.fromJson(body);
        Item updated = service.updateItem(id, request);
        if (updated == null) {
            return ProblemDetail.notFound("Item with id " + id + " was not found").toResponse();
        }
        return Response.ok(updated.toJson())
                .header("Content-Type", "application/json");
    }

    @DELETE
    @Path("/{id}")
    @ApiOperation(summary = "Delete an item")
    @ApiResponse(code = 204, description = "Item deleted")
    @ApiResponse(code = 404, description = "Item not found")
    public Response delete(@PathParam("id") String id) {
        boolean deleted = service.deleteItem(id);
        if (!deleted) {
            return ProblemDetail.notFound("Item with id " + id + " was not found").toResponse();
        }
        return Response.status(204);
    }
}
