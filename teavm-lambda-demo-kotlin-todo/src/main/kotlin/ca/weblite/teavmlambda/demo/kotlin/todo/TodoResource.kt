package ca.weblite.teavmlambda.demo.kotlin.todo

import ca.weblite.teavmlambda.api.Response
import ca.weblite.teavmlambda.api.annotation.*

/**
 * Todo REST API Resource
 *
 * Provides endpoints for managing todos:
 *  - GET /todos - List all todos
 *  - GET /todos/{id} - Get a specific todo
 *  - POST /todos - Create a new todo
 *  - PUT /todos/{id} - Update a todo
 *  - DELETE /todos/{id} - Delete a todo
 *  - GET /todos/stats/summary - Get todo statistics
 */
@Path("/todos")
@Component
@Singleton
class TodoResource @Inject constructor(private val todoService: TodoService) {

    /**
     * List all todos
     *
     * Example:
     *   GET /todos
     *   Response: [{"id":"...", "title":"Buy milk", "completed":false, ...}, ...]
     */
    @GET
    fun listTodos(): Response {
        return try {
            val todos = todoService.getAllTodos()
            Response.ok(Todo.toJsonArray(todos))
                .header("Content-Type", "application/json")
        } catch (e: Exception) {
            Response.status(500)
                .header("Content-Type", "application/json")
                .body("""{"error":"Failed to list todos"}""")
        }
    }

    /**
     * Get a specific todo by ID
     *
     * Example:
     *   GET /todos/123e4567-e89b-12d3-a456-426614174000
     *   Response: {"id":"123e4567-e89b-12d3-a456-426614174000", "title":"Buy milk", ...}
     */
    @GET
    @Path("/{id}")
    fun getTodo(@PathParam("id") id: String): Response {
        return try {
            val todo = todoService.getTodoById(id)
            if (todo != null) {
                Response.ok(todo.toJson())
                    .header("Content-Type", "application/json")
            } else {
                Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("""{"error":"Todo not found"}""")
            }
        } catch (e: Exception) {
            Response.status(500)
                .header("Content-Type", "application/json")
                .body("""{"error":"Failed to get todo"}""")
        }
    }

    /**
     * Create a new todo
     *
     * Example:
     *   POST /todos
     *   Body: {"title":"Buy milk", "description":"2% milk"}
     *   Response: {"id":"...", "title":"Buy milk", "completed":false, ...}
     */
    @POST
    fun createTodo(@Body body: String): Response {
        return try {
            val request = CreateTodoRequest.fromJson(body)
            if (request.title.isBlank()) {
                return Response.status(400)
                    .header("Content-Type", "application/json")
                    .body("""{"error":"Title is required"}""")
            }

            val todo = todoService.createTodo(request)
            Response.status(201)
                .header("Content-Type", "application/json")
                .body(todo.toJson())
        } catch (e: Exception) {
            Response.status(400)
                .header("Content-Type", "application/json")
                .body("""{"error":"Invalid request: ${e.message}"}""")
        }
    }

    /**
     * Update a todo
     *
     * Example:
     *   PUT /todos/123e4567-e89b-12d3-a456-426614174000
     *   Body: {"completed":true}
     *   Response: {"id":"123e4567-e89b-12d3-a456-426614174000", "title":"Buy milk", "completed":true, ...}
     */
    @PUT
    @Path("/{id}")
    fun updateTodo(@PathParam("id") id: String, @Body body: String): Response {
        return try {
            val request = UpdateTodoRequest.fromJson(body)
            val todo = todoService.updateTodo(id, request)

            if (todo != null) {
                Response.ok(todo.toJson())
                    .header("Content-Type", "application/json")
            } else {
                Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("""{"error":"Todo not found"}""")
            }
        } catch (e: Exception) {
            Response.status(400)
                .header("Content-Type", "application/json")
                .body("""{"error":"Invalid request: ${e.message}"}""")
        }
    }

    /**
     * Delete a todo
     *
     * Example:
     *   DELETE /todos/123e4567-e89b-12d3-a456-426614174000
     *   Response: 204 No Content
     */
    @DELETE
    @Path("/{id}")
    fun deleteTodo(@PathParam("id") id: String): Response {
        return try {
            val deleted = todoService.deleteTodo(id)
            if (deleted) {
                Response.status(204)
            } else {
                Response.status(404)
                    .header("Content-Type", "application/json")
                    .body("""{"error":"Todo not found"}""")
            }
        } catch (e: Exception) {
            Response.status(500)
                .header("Content-Type", "application/json")
                .body("""{"error":"Failed to delete todo"}""")
        }
    }

    /**
     * Get todo statistics
     *
     * Example:
     *   GET /todos/stats/summary
     *   Response: {"total":5, "completed":2, "incomplete":3}
     */
    @GET
    @Path("/stats/summary")
    fun getStatistics(): Response {
        return try {
            val todos = todoService.getAllTodos()
            val completed = todos.count { it.completed }
            val incomplete = todos.size - completed

            val json = """{
                |"total":${todos.size},
                |"completed":$completed,
                |"incomplete":$incomplete
            |}""".trimMargin().replace("\n", "")

            Response.ok(json)
                .header("Content-Type", "application/json")
        } catch (e: Exception) {
            Response.status(500)
                .header("Content-Type", "application/json")
                .body("""{"error":"Failed to get statistics"}""")
        }
    }
}
