package ca.weblite.teavmlambda.demo.kotlin.todo

import ca.weblite.teavmlambda.api.db.DbRow
import java.time.Instant

/**
 * Todo item data class
 */
data class Todo(
    val id: String,
    val title: String,
    val description: String?,
    val completed: Boolean = false,
    val createdAt: String = Instant.now().toString(),
    val updatedAt: String = Instant.now().toString()
) {
    companion object {
        /**
         * Convert database row to Todo
         */
        fun fromDbRow(row: DbRow): Todo {
            return Todo(
                id = row.getString("id"),
                title = row.getString("title"),
                description = if (row.has("description") && !row.isNull("description")) row.getString("description") else null,
                completed = row.getBoolean("completed"),
                createdAt = row.getString("created_at"),
                updatedAt = row.getString("updated_at")
            )
        }

        /**
         * Convert list of database rows to JSON array
         */
        fun toJsonArray(todos: List<Todo>): String {
            val items = todos.joinToString(",") { it.toJson() }
            return "[$items]"
        }
    }

    /**
     * Convert to JSON
     */
    fun toJson(): String {
        val desc = description?.let { "\"${it.replace("\"", "\\\"")}\"" } ?: "null"
        return """{
            |"id":"$id",
            |"title":"${title.replace("\"", "\\\"")}",
            |"description":$desc,
            |"completed":$completed,
            |"createdAt":"$createdAt",
            |"updatedAt":"$updatedAt"
        |}""".trimMargin().replace("\n", "")
    }
}

/**
 * Request DTO for creating/updating todos
 */
data class CreateTodoRequest(
    val title: String,
    val description: String? = null
) {
    companion object {
        /**
         * Parse JSON string to CreateTodoRequest
         */
        fun fromJson(json: String): CreateTodoRequest {
            // Simple JSON parsing (in production, use a proper JSON library)
            val titleMatch = """"title"\s*:\s*"([^"]*)"""".toRegex().find(json)
            val descMatch = """"description"\s*:\s*"([^"]*)"""".toRegex().find(json)

            val title = titleMatch?.groupValues?.get(1) ?: throw IllegalArgumentException("Missing title field")
            val description = descMatch?.groupValues?.get(1)

            return CreateTodoRequest(title, description)
        }
    }
}

/**
 * Request DTO for updating todo completion status
 */
data class UpdateTodoRequest(
    val title: String? = null,
    val description: String? = null,
    val completed: Boolean? = null
) {
    companion object {
        /**
         * Parse JSON string to UpdateTodoRequest
         */
        fun fromJson(json: String): UpdateTodoRequest {
            val titleMatch = """"title"\s*:\s*"([^"]*)"""".toRegex().find(json)
            val descMatch = """"description"\s*:\s*"([^"]*)"""".toRegex().find(json)
            val completedMatch = """"completed"\s*:\s*(true|false)""".toRegex().find(json)

            val title = titleMatch?.groupValues?.get(1)
            val description = descMatch?.groupValues?.get(1)
            val completed = completedMatch?.groupValues?.get(1)?.toBoolean()

            return UpdateTodoRequest(title, description, completed)
        }
    }
}
