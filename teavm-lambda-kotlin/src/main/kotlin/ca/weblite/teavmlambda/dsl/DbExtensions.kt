package ca.weblite.teavmlambda.dsl

import ca.weblite.teavmlambda.api.db.Database
import ca.weblite.teavmlambda.api.db.DbResult
import ca.weblite.teavmlambda.api.db.DbRow

// ── DbRow extensions ─────────────────────────────────────────────────

/** Get a string column, returning null if the column doesn't exist or is null. */
fun DbRow.stringOrNull(column: String): String? =
    if (has(column) && !isNull(column)) getString(column) else null

/** Get an int column with a default. */
fun DbRow.int(column: String, default: Int = 0): Int =
    if (has(column) && !isNull(column)) getInt(column) else default

/** Get a double column with a default. */
fun DbRow.double(column: String, default: Double = 0.0): Double =
    if (has(column) && !isNull(column)) getDouble(column) else default

/** Get a boolean column with a default. */
fun DbRow.bool(column: String, default: Boolean = false): Boolean =
    if (has(column) && !isNull(column)) getBoolean(column) else default

/** Operator access: `row["name"]` returns the string value or null. */
operator fun DbRow.get(column: String): String? = stringOrNull(column)

// ── DbResult extensions ──────────────────────────────────────────────

/**
 * Map all rows using a [RowMapper].
 *
 * ```kotlin
 * val items: List<Item> = result.map(Item)
 * ```
 */
fun <T> DbResult.map(mapper: RowMapper<T>): List<T> =
    rows.map { mapper.fromRow(it) }

/**
 * Map all rows using a lambda.
 *
 * ```kotlin
 * val names = result.map { it.getString("name") }
 * ```
 */
fun <T> DbResult.map(transform: (DbRow) -> T): List<T> =
    rows.map(transform)

/** Get the first row mapped, or null if no results. */
fun <T> DbResult.firstOrNull(mapper: RowMapper<T>): T? =
    rows.firstOrNull()?.let { mapper.fromRow(it) }

/** Get the first row mapped, or null if no results (lambda version). */
fun <T> DbResult.firstOrNull(transform: (DbRow) -> T): T? =
    rows.firstOrNull()?.let(transform)

/** Get the first row mapped, or throw [NotFound]. */
fun <T> DbResult.first(mapper: RowMapper<T>, message: String = "Not found"): T =
    firstOrNull(mapper) ?: throw NotFound(message)

/** True if the result has no rows. */
val DbResult.isEmpty: Boolean get() = rowCount == 0

/** True if the result has at least one row. */
val DbResult.isNotEmpty: Boolean get() = rowCount > 0

// ── Database query extensions ────────────────────────────────────────

/**
 * Query and map all rows with a [RowMapper].
 *
 * ```kotlin
 * val items = db.queryAll("SELECT * FROM items", Item)
 * ```
 */
fun <T> Database.queryAll(sql: String, mapper: RowMapper<T>): List<T> =
    query(sql).map(mapper)

/** Query with params and map all rows. */
fun <T> Database.queryAll(sql: String, vararg params: String, mapper: RowMapper<T>): List<T> =
    query(sql, *params).map(mapper)

/**
 * Query and map the first row, or null.
 *
 * ```kotlin
 * val item = db.queryOne("SELECT * FROM items WHERE id = $1", id.toString(), mapper = Item)
 * ```
 */
fun <T> Database.queryOne(sql: String, vararg params: String, mapper: RowMapper<T>): T? =
    query(sql, *params).firstOrNull(mapper)

// ── RowMapper interface ──────────────────────────────────────────────

/**
 * Maps a [DbRow] to a domain object. Implement on companion objects:
 *
 * ```kotlin
 * data class Item(val id: Int, val name: String) : JsonSerializable {
 *     companion object : RowMapper<Item> {
 *         override fun fromRow(row: DbRow) = Item(
 *             id = row.getInt("id"),
 *             name = row.getString("name")
 *         )
 *     }
 *     override fun toJson() = json { "id" to id; "name" to name }
 * }
 *
 * // Usage:
 * val items = db.queryAll("SELECT * FROM items", Item)
 * val item = result.firstOrNull(Item)
 * ```
 */
interface RowMapper<T> {
    fun fromRow(row: DbRow): T
}
