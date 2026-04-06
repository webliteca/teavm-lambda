package ca.weblite.teavmlambda.dsl

/**
 * Marker interface for objects that can serialize themselves to JSON.
 *
 * Implement this on data classes / entities so the framework can
 * auto-serialize them in [RequestContext.ok], [RequestContext.created], etc.
 *
 * ```kotlin
 * data class Item(val id: Int, val name: String) : JsonSerializable {
 *     override fun toJson(): String = json {
 *         "id" to id
 *         "name" to name
 *     }
 * }
 * ```
 */
interface JsonSerializable {
    fun toJson(): String
}
