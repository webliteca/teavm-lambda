package ca.weblite.teavmlambda.dsl

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonDslTest {

    @Test
    fun `json builds empty object`() {
        val result = json { }
        assertEquals("{}", result)
    }

    @Test
    fun `json builds object with string value`() {
        val result = json { "name" to "Alice" }
        assertEquals("""{"name":"Alice"}""", result)
    }

    @Test
    fun `json builds object with null string`() {
        val nullStr: String? = null
        val result = json { "name" to nullStr }
        assertEquals("""{"name":null}""", result)
    }

    @Test
    fun `json builds object with int value`() {
        val result = json { "age" to 30 }
        assertEquals("""{"age":30}""", result)
    }

    @Test
    fun `json builds object with long value`() {
        val result = json { "big" to 9999999999L }
        assertEquals("""{"big":9999999999}""", result)
    }

    @Test
    fun `json builds object with double value`() {
        val result = json { "pi" to 3.14 }
        assertEquals("""{"pi":3.14}""", result)
    }

    @Test
    fun `json builds object with boolean value`() {
        val result = json { "active" to true }
        assertEquals("""{"active":true}""", result)
    }

    @Test
    fun `json builds object with multiple fields`() {
        val result = json {
            "name" to "Alice"
            "age" to 30
            "active" to true
        }
        assertEquals("""{"name":"Alice","age":30,"active":true}""", result)
    }

    @Test
    fun `json builds nested objects`() {
        val inner = JsonObjectScope()
        inner.apply { "city" to "Toronto" }
        val result = json { "address" to inner }
        assertEquals("""{"address":{"city":"Toronto"}}""", result)
    }

    @Test
    fun `json builds object with array value`() {
        val result = json {
            "tags" to jsonArray {
                addString("kotlin")
                addString("serverless")
            }
        }
        assertEquals("""{"tags":["kotlin","serverless"]}""", result)
    }

    @Test
    fun `json builds object with JsonSerializable value`() {
        val item = object : JsonSerializable {
            override fun toJson() = """{"id":1}"""
        }
        val result = json { "item" to item }
        assertEquals("""{"item":{"id":1}}""", result)
    }

    @Test
    fun `json builds object with null JsonSerializable`() {
        val item: JsonSerializable? = null
        val result = json { "item" to item }
        assertEquals("""{"item":null}""", result)
    }

    @Test
    fun `json raw embeds pre-serialized JSON`() {
        val result = json { raw("data", """[1,2,3]""") }
        assertEquals("""{"data":[1,2,3]}""", result)
    }

    @Test
    fun `json escapes special characters`() {
        val result = json { "msg" to "hello\nworld" }
        assertEquals("""{"msg":"hello\nworld"}""", result)
    }

    // ── jsonArray ────────────────────────────────────────────────────

    @Test
    fun `jsonArrayString builds empty array`() {
        val result = jsonArrayString { }
        assertEquals("[]", result)
    }

    @Test
    fun `jsonArrayString builds array of raw json`() {
        val result = jsonArrayString {
            add("""{"id":1}""")
            add("""{"id":2}""")
        }
        assertEquals("""[{"id":1},{"id":2}]""", result)
    }

    @Test
    fun `jsonArrayString builds array with strings`() {
        val result = jsonArrayString {
            addString("a")
            addString("b")
        }
        assertEquals("""["a","b"]""", result)
    }

    @Test
    fun `jsonArray addAll serializes list of JsonSerializable`() {
        val items = listOf(
            object : JsonSerializable { override fun toJson() = """{"id":1}""" },
            object : JsonSerializable { override fun toJson() = """{"id":2}""" }
        )
        val result = jsonArrayString { addAll(items) }
        assertEquals("""[{"id":1},{"id":2}]""", result)
    }

    @Test
    fun `jsonArray addObject builds nested object`() {
        val result = jsonArrayString {
            addObject { "name" to "Alice" }
            addObject { "name" to "Bob" }
        }
        assertEquals("""[{"name":"Alice"},{"name":"Bob"}]""", result)
    }

    // ── toJsonArray extension ────────────────────────────────────────

    @Test
    fun `toJsonArray serializes iterable of JsonSerializable`() {
        val items = listOf(
            object : JsonSerializable { override fun toJson() = """{"x":1}""" },
            object : JsonSerializable { override fun toJson() = """{"x":2}""" }
        )
        assertEquals("""[{"x":1},{"x":2}]""", items.toJsonArray())
    }

    @Test
    fun `toJsonArray on empty list returns empty array`() {
        val items = emptyList<JsonSerializable>()
        assertEquals("[]", items.toJsonArray())
    }
}
