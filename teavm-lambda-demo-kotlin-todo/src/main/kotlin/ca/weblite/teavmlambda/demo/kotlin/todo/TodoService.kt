package ca.weblite.teavmlambda.demo.kotlin.todo

import ca.weblite.teavmlambda.api.annotation.Service
import ca.weblite.teavmlambda.api.annotation.Inject
import ca.weblite.teavmlambda.api.annotation.Singleton
import ca.weblite.teavmlambda.api.db.Database
import java.util.UUID

/**
 * Service for managing todos
 *
 * This is a business logic layer that handles todo operations.
 * It's injected into TodoResource via compile-time dependency injection.
 */
@Service
@Singleton
class TodoService @Inject constructor(private val database: Database) {

    /**
     * Initialize database schema on first use
     */
    init {
        try {
            database.query("""
                CREATE TABLE IF NOT EXISTS todos (
                    id VARCHAR(36) PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description TEXT,
                    completed BOOLEAN DEFAULT false,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
        } catch (e: Exception) {
            // Table might already exist
        }
    }

    /**
     * Get all todos
     */
    fun getAllTodos(): List<Todo> {
        val result = database.query("SELECT * FROM todos ORDER BY created_at DESC")
        return result.rows.map { Todo.fromDbRow(it) }
    }

    /**
     * Get a specific todo by ID
     */
    fun getTodoById(id: String): Todo? {
        val result = database.query("SELECT * FROM todos WHERE id = ?", id)
        return if (result.rowCount > 0) {
            Todo.fromDbRow(result.rows[0])
        } else {
            null
        }
    }

    /**
     * Create a new todo
     */
    fun createTodo(request: CreateTodoRequest): Todo {
        val id = UUID.randomUUID().toString()
        val now = java.time.Instant.now().toString()

        database.query(
            "INSERT INTO todos (id, title, description, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
            id,
            request.title,
            request.description ?: "",
            now,
            now
        )

        return Todo(
            id = id,
            title = request.title,
            description = request.description,
            completed = false,
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * Update a todo
     */
    fun updateTodo(id: String, request: UpdateTodoRequest): Todo? {
        val existing = getTodoById(id) ?: return null

        val now = java.time.Instant.now().toString()
        val title = request.title ?: existing.title
        val description = request.description ?: existing.description
        val completed = request.completed ?: existing.completed

        database.query(
            "UPDATE todos SET title = ?, description = ?, completed = ?, updated_at = ? WHERE id = ?",
            title,
            description ?: "",
            completed.toString(),
            now,
            id
        )

        return existing.copy(
            title = title,
            description = description,
            completed = completed,
            updatedAt = now
        )
    }

    /**
     * Delete a todo
     */
    fun deleteTodo(id: String): Boolean {
        val result = database.query("DELETE FROM todos WHERE id = ?", id)
        return result.rowCount > 0
    }

    /**
     * Get incomplete todos count
     */
    fun getIncompleteCount(): Long {
        val result = database.query("SELECT COUNT(*) as count FROM todos WHERE completed = false")
        return if (result.rowCount > 0) {
            result.rows[0].getInt("count").toLong()
        } else {
            0
        }
    }

    /**
     * Get completed todos count
     */
    fun getCompletedCount(): Long {
        val result = database.query("SELECT COUNT(*) as count FROM todos WHERE completed = true")
        return if (result.rowCount > 0) {
            result.rows[0].getInt("count").toLong()
        } else {
            0
        }
    }
}
