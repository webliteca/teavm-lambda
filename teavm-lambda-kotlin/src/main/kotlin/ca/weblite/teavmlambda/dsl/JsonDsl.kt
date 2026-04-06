package ca.weblite.teavmlambda.dsl

import ca.weblite.teavmlambda.api.json.JsonBuilder

/**
 * Kotlin DSL for building JSON objects.
 *
 * ```kotlin
 * val s = json {
 *     "name" to "Alice"
 *     "age" to 30
 *     "active" to true
 *     "address" to json {
 *         "city" to "Toronto"
 *     }
 *     "tags" to jsonArray {
 *         add("kotlin")
 *         add("serverless")
 *     }
 * }
 * ```
 */
@DslMarker
annotation class JsonDslMarker

@JsonDslMarker
class JsonObjectScope(@PublishedApi internal val builder: JsonBuilder = JsonBuilder.`object`()) {

    infix fun String.to(value: String?) {
        if (value == null) builder.put(this, null as String?) else builder.put(this, value)
    }

    infix fun String.to(value: Int) {
        builder.put(this, value)
    }

    infix fun String.to(value: Long) {
        builder.put(this, value)
    }

    infix fun String.to(value: Double) {
        builder.put(this, value)
    }

    infix fun String.to(value: Boolean) {
        builder.put(this, value)
    }

    /** Nest a JSON object. */
    infix fun String.to(block: JsonObjectScope) {
        builder.putRaw(this, block.builder.build())
    }

    /** Nest a JSON array. */
    infix fun String.to(array: JsonArrayValue) {
        builder.putRaw(this, array.raw)
    }

    /** Embed a [JsonSerializable] value. */
    infix fun String.to(value: JsonSerializable?) {
        if (value == null) builder.put(this, null as String?) else builder.putRaw(this, value.toJson())
    }

    /** Embed a raw JSON string (for pre-serialized content). */
    fun raw(key: String, rawJson: String?) {
        builder.putRaw(key, rawJson)
    }

    @PublishedApi
    internal fun build(): String = builder.build()
}

@JsonDslMarker
class JsonArrayScope(@PublishedApi internal val builder: JsonBuilder = JsonBuilder.array()) {

    /** Add a raw JSON element. */
    fun add(rawJson: String) {
        builder.add(rawJson)
    }

    /** Add a string element. */
    fun addString(value: String?) {
        builder.addString(value)
    }

    /** Add a [JsonSerializable] element. */
    fun add(value: JsonSerializable) {
        builder.add(value.toJson())
    }

    /** Add all [JsonSerializable] elements. */
    fun addAll(values: Iterable<JsonSerializable>) {
        for (v in values) builder.add(v.toJson())
    }

    /** Add a nested object. */
    fun addObject(block: JsonObjectScope.() -> Unit) {
        val scope = JsonObjectScope()
        scope.block()
        builder.add(scope.build())
    }

    @PublishedApi
    internal fun build(): String = builder.build()
}

/** Wrapper to distinguish array values in the DSL. */
class JsonArrayValue(@PublishedApi internal val raw: String)

/** Build a JSON object string. */
inline fun json(block: JsonObjectScope.() -> Unit): String {
    val scope = JsonObjectScope()
    scope.block()
    return scope.build()
}

/** Build a JSON array string. Returns a [JsonArrayValue] for embedding in objects. */
inline fun jsonArray(block: JsonArrayScope.() -> Unit): JsonArrayValue {
    val scope = JsonArrayScope()
    scope.block()
    return JsonArrayValue(scope.build())
}

/** Build a JSON array as a raw string. */
inline fun jsonArrayString(block: JsonArrayScope.() -> Unit): String {
    val scope = JsonArrayScope()
    scope.block()
    return scope.build()
}

/** Serialize a list of [JsonSerializable] to a JSON array string. */
fun Iterable<JsonSerializable>.toJsonArray(): String {
    val b = JsonBuilder.array()
    for (item in this) b.add(item.toJson())
    return b.build()
}
