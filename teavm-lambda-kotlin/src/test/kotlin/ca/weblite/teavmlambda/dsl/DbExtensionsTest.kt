package ca.weblite.teavmlambda.dsl

import ca.weblite.teavmlambda.api.db.DbResult
import ca.weblite.teavmlambda.api.db.DbRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DbExtensionsTest {

    /** Simple in-memory DbRow implementation for testing. */
    private class TestRow(private val data: Map<String, Any?>) : DbRow {
        override fun getString(column: String): String = data[column] as String
        override fun getInt(column: String): Int = data[column] as Int
        override fun getDouble(column: String): Double = data[column] as Double
        override fun getBoolean(column: String): Boolean = data[column] as Boolean
        override fun has(column: String): Boolean = data.containsKey(column)
        override fun isNull(column: String): Boolean = data[column] == null
        override fun toJson(): String {
            val entries = data.entries.joinToString(",") { (k, v) ->
                when (v) {
                    null -> "\"$k\":null"
                    is String -> "\"$k\":\"$v\""
                    else -> "\"$k\":$v"
                }
            }
            return "{$entries}"
        }
    }

    /** Simple in-memory DbResult. */
    private class TestResult(private val data: List<DbRow>) : DbResult {
        override fun getRows(): List<DbRow> = data
        override fun getRowCount(): Int = data.size
    }

    private fun row(vararg pairs: Pair<String, Any?>): DbRow = TestRow(mapOf(*pairs))
    private fun result(vararg rows: DbRow): DbResult = TestResult(rows.toList())

    // ── DbRow extensions ─────────────────────────────────────────────

    @Test
    fun `stringOrNull returns value when present and non-null`() {
        val r = row("name" to "Alice")
        assertEquals("Alice", r.stringOrNull("name"))
    }

    @Test
    fun `stringOrNull returns null when column is null`() {
        val r = row("name" to null)
        assertNull(r.stringOrNull("name"))
    }

    @Test
    fun `stringOrNull returns null when column missing`() {
        val r = row("other" to "value")
        assertNull(r.stringOrNull("name"))
    }

    @Test
    fun `int returns value when present`() {
        val r = row("qty" to 5)
        assertEquals(5, r.int("qty"))
    }

    @Test
    fun `int returns default when column is null`() {
        val r = row("qty" to null)
        assertEquals(0, r.int("qty"))
    }

    @Test
    fun `int returns custom default when missing`() {
        val r = row("other" to 1)
        assertEquals(42, r.int("qty", 42))
    }

    @Test
    fun `double returns value when present`() {
        val r = row("price" to 9.99)
        assertEquals(9.99, r.double("price"))
    }

    @Test
    fun `double returns default when missing`() {
        val r = row("other" to 1)
        assertEquals(0.0, r.double("price"))
    }

    @Test
    fun `bool returns value when present`() {
        val r = row("active" to true)
        assertTrue(r.bool("active"))
    }

    @Test
    fun `bool returns default when missing`() {
        val r = row("other" to 1)
        assertFalse(r.bool("active"))
    }

    @Test
    fun `operator get returns string value or null`() {
        val r = row("name" to "Bob", "desc" to null)
        assertEquals("Bob", r["name"])
        assertNull(r["desc"])
        assertNull(r["missing"])
    }

    // ── DbResult extensions ──────────────────────────────────────────

    data class Item(val id: Int, val name: String)

    object ItemMapper : RowMapper<Item> {
        override fun fromRow(row: DbRow) = Item(row.getInt("id"), row.getString("name"))
    }

    @Test
    fun `map with RowMapper transforms all rows`() {
        val res = result(
            row("id" to 1, "name" to "A"),
            row("id" to 2, "name" to "B")
        )
        val items = res.map(ItemMapper)
        assertEquals(2, items.size)
        assertEquals(Item(1, "A"), items[0])
        assertEquals(Item(2, "B"), items[1])
    }

    @Test
    fun `map with lambda transforms all rows`() {
        val res = result(row("id" to 1, "name" to "A"))
        val names = res.map { it.getString("name") }
        assertEquals(listOf("A"), names)
    }

    @Test
    fun `firstOrNull with mapper returns first row`() {
        val res = result(row("id" to 1, "name" to "A"))
        val item = res.firstOrNull(ItemMapper)
        assertEquals(Item(1, "A"), item)
    }

    @Test
    fun `firstOrNull returns null on empty result`() {
        val res = result()
        assertNull(res.firstOrNull(ItemMapper))
    }

    @Test
    fun `firstOrNull with lambda returns first`() {
        val res = result(row("id" to 1, "name" to "A"))
        val name = res.firstOrNull { it.getString("name") }
        assertEquals("A", name)
    }

    @Test
    fun `first with mapper returns first row`() {
        val res = result(row("id" to 1, "name" to "X"))
        assertEquals(Item(1, "X"), res.first(ItemMapper))
    }

    @Test
    fun `first throws NotFound on empty result`() {
        val res = result()
        val ex = assertFailsWith<NotFound> {
            res.first(ItemMapper, "Item not found")
        }
        assertEquals("Item not found", ex.message)
    }

    @Test
    fun `isEmpty returns true for empty result`() {
        assertTrue(result().isEmpty)
    }

    @Test
    fun `isEmpty returns false for non-empty result`() {
        assertFalse(result(row("id" to 1, "name" to "A")).isEmpty)
    }

    @Test
    fun `isNotEmpty returns true for non-empty result`() {
        assertTrue(result(row("id" to 1, "name" to "A")).isNotEmpty)
    }

    @Test
    fun `isNotEmpty returns false for empty result`() {
        assertFalse(result().isNotEmpty)
    }
}
