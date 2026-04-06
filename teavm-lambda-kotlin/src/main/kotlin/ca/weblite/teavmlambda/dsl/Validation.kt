package ca.weblite.teavmlambda.dsl

/**
 * Lightweight validation DSL that leverages Kotlin's type system.
 *
 * ```kotlin
 * post {
 *     val req = body(CreateItemRequest)
 *     validate {
 *         require(req.name.isNotEmpty()) { "name" to "must not be empty" }
 *         require(req.quantity >= 0) { "quantity" to "must be >= 0" }
 *         require(req.quantity <= 10_000) { "quantity" to "must be <= 10000" }
 *     }
 *     created(service<ItemService>().create(req))
 * }
 * ```
 */
@DslMarker
annotation class ValidationDslMarker

data class FieldError(val field: String, val message: String)

/**
 * Validation result. Call [throwIfInvalid] to convert errors to a
 * [BadRequest] with structured error details.
 */
class ValidationResult(val errors: List<FieldError>) {

    val isValid: Boolean get() = errors.isEmpty()

    /** Throws [ValidationException] if there are errors. */
    fun throwIfInvalid() {
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    /** JSON representation of errors. */
    fun toJson(): String = buildString {
        append("""{"errors":[""")
        errors.forEachIndexed { i, e ->
            if (i > 0) append(',')
            append("""{"field":"${escapeJson(e.field)}","message":"${escapeJson(e.message)}"}""")
        }
        append("]}")
    }
}

/**
 * Thrown when validation fails. Caught by the router and converted
 * to a 400 response with structured error details.
 */
class ValidationException(val errors: List<FieldError>) : HttpException(400, "Validation failed") {
    fun toValidationResponse(): ca.weblite.teavmlambda.api.Response {
        val result = ValidationResult(errors)
        return ca.weblite.teavmlambda.api.Response.status(400)
            .header("Content-Type", "application/json")
            .body(result.toJson())
    }
}

@ValidationDslMarker
class ValidationScope {
    internal val errors = mutableListOf<FieldError>()

    /**
     * Assert a condition. If false, add an error.
     *
     * ```kotlin
     * require(name.isNotBlank()) { "name" to "must not be blank" }
     * ```
     */
    inline fun require(condition: Boolean, error: () -> Pair<String, String>) {
        if (!condition) {
            val (field, message) = error()
            errors += FieldError(field, message)
        }
    }

    /** Validate that a string is not null or empty. */
    fun notEmpty(value: String?, field: String, message: String = "$field must not be empty") {
        if (value.isNullOrEmpty()) errors += FieldError(field, message)
    }

    /** Validate that a string is not null or blank. */
    fun notBlank(value: String?, field: String, message: String = "$field must not be blank") {
        if (value.isNullOrBlank()) errors += FieldError(field, message)
    }

    /** Validate a minimum value. */
    fun min(value: Int, min: Int, field: String, message: String = "$field must be >= $min") {
        if (value < min) errors += FieldError(field, message)
    }

    /** Validate a maximum value. */
    fun max(value: Int, max: Int, field: String, message: String = "$field must be <= $max") {
        if (value > max) errors += FieldError(field, message)
    }

    /** Validate a long minimum value. */
    fun min(value: Long, min: Long, field: String, message: String = "$field must be >= $min") {
        if (value < min) errors += FieldError(field, message)
    }

    /** Validate a long maximum value. */
    fun max(value: Long, max: Long, field: String, message: String = "$field must be <= $max") {
        if (value > max) errors += FieldError(field, message)
    }

    /** Validate a value is within a range. */
    fun range(value: Int, range: IntRange, field: String, message: String = "$field must be in $range") {
        if (value !in range) errors += FieldError(field, message)
    }

    /** Validate a string matches a regex pattern. */
    fun matches(value: String?, pattern: Regex, field: String, message: String = "$field is invalid") {
        if (value == null || !pattern.matches(value)) errors += FieldError(field, message)
    }

    /** Validate string length constraints. */
    fun length(value: String?, min: Int = 0, max: Int = Int.MAX_VALUE, field: String, message: String? = null) {
        val len = value?.length ?: 0
        if (len < min || len > max) {
            errors += FieldError(field, message ?: "$field length must be between $min and $max")
        }
    }
}

/**
 * Run validation and throw [ValidationException] if any checks fail.
 *
 * ```kotlin
 * validate {
 *     notEmpty(req.name, "name")
 *     min(req.quantity, 0, "quantity")
 * }
 * ```
 */
fun validate(block: ValidationScope.() -> Unit) {
    val scope = ValidationScope()
    scope.block()
    if (scope.errors.isNotEmpty()) {
        throw ValidationException(scope.errors)
    }
}

/**
 * Run validation and return a [ValidationResult] without throwing.
 *
 * ```kotlin
 * val result = validationResult {
 *     notEmpty(req.name, "name")
 * }
 * if (!result.isValid) return result.toJson()
 * ```
 */
fun validationResult(block: ValidationScope.() -> Unit): ValidationResult {
    val scope = ValidationScope()
    scope.block()
    return ValidationResult(scope.errors)
}

private fun escapeJson(value: String): String =
    value.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
