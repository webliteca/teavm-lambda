package ca.weblite.teavmlambda.dsl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationTest {

    @Test
    fun `validate passes when no errors`() {
        // Should not throw
        validate {
            notEmpty("hello", "name")
            min(5, 0, "quantity")
        }
    }

    @Test
    fun `validate throws ValidationException on failure`() {
        val ex = assertFailsWith<ValidationException> {
            validate {
                notEmpty("", "name")
            }
        }
        assertEquals(1, ex.errors.size)
        assertEquals("name", ex.errors[0].field)
        assertEquals("name must not be empty", ex.errors[0].message)
    }

    @Test
    fun `validate collects multiple errors`() {
        val ex = assertFailsWith<ValidationException> {
            validate {
                notEmpty("", "name")
                min(-1, 0, "quantity")
                max(200, 100, "quantity")
            }
        }
        assertEquals(3, ex.errors.size)
    }

    @Test
    fun `notEmpty fails on null`() {
        val ex = assertFailsWith<ValidationException> {
            validate { notEmpty(null, "field") }
        }
        assertEquals("field", ex.errors[0].field)
    }

    @Test
    fun `notBlank fails on whitespace`() {
        val ex = assertFailsWith<ValidationException> {
            validate { notBlank("   ", "name") }
        }
        assertEquals("name must not be blank", ex.errors[0].message)
    }

    @Test
    fun `notBlank passes on non-blank string`() {
        validate { notBlank("hello", "name") }
    }

    @Test
    fun `min validates correctly`() {
        validate { min(5, 5, "value") }  // equal is ok
        validate { min(10, 5, "value") } // above is ok

        val ex = assertFailsWith<ValidationException> {
            validate { min(4, 5, "value") }
        }
        assertEquals("value must be >= 5", ex.errors[0].message)
    }

    @Test
    fun `max validates correctly`() {
        validate { max(100, 100, "value") }  // equal is ok

        val ex = assertFailsWith<ValidationException> {
            validate { max(101, 100, "value") }
        }
        assertEquals("value must be <= 100", ex.errors[0].message)
    }

    @Test
    fun `min and max work for Long values`() {
        validate { min(5L, 5L, "v") }
        val ex = assertFailsWith<ValidationException> {
            validate { max(101L, 100L, "v") }
        }
        assertEquals(1, ex.errors.size)
    }

    @Test
    fun `range validates correctly`() {
        validate { range(5, 1..10, "v") }

        val ex = assertFailsWith<ValidationException> {
            validate { range(11, 1..10, "v") }
        }
        assertEquals(1, ex.errors.size)
    }

    @Test
    fun `matches validates regex pattern`() {
        validate { matches("abc123", Regex("[a-z]+\\d+"), "code") }

        val ex = assertFailsWith<ValidationException> {
            validate { matches("!!!", Regex("[a-z]+"), "code") }
        }
        assertEquals("code is invalid", ex.errors[0].message)
    }

    @Test
    fun `matches fails on null`() {
        val ex = assertFailsWith<ValidationException> {
            validate { matches(null, Regex(".*"), "field") }
        }
        assertEquals(1, ex.errors.size)
    }

    @Test
    fun `length validates string length`() {
        validate { length("abc", min = 1, max = 5, field = "name") }

        val ex = assertFailsWith<ValidationException> {
            validate { length("ab", min = 3, max = 10, field = "name") }
        }
        assertTrue(ex.errors[0].message.contains("between"))
    }

    @Test
    fun `require with lambda builds custom error`() {
        val ex = assertFailsWith<ValidationException> {
            validate {
                require(false) { "email" to "must be valid" }
            }
        }
        assertEquals("email", ex.errors[0].field)
        assertEquals("must be valid", ex.errors[0].message)
    }

    @Test
    fun `require passes when condition is true`() {
        validate {
            require(true) { "field" to "error" }
        }
    }

    @Test
    fun `custom message overrides default`() {
        val ex = assertFailsWith<ValidationException> {
            validate { notEmpty("", "name", "Please provide a name") }
        }
        assertEquals("Please provide a name", ex.errors[0].message)
    }

    // ── ValidationResult ─────────────────────────────────────────────

    @Test
    fun `validationResult returns result without throwing`() {
        val result = validationResult {
            notEmpty("", "name")
            min(-1, 0, "qty")
        }
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
    }

    @Test
    fun `validationResult isValid when no errors`() {
        val result = validationResult { notEmpty("ok", "name") }
        assertTrue(result.isValid)
    }

    @Test
    fun `ValidationResult toJson produces correct format`() {
        val result = ValidationResult(listOf(
            FieldError("name", "required"),
            FieldError("age", "must be positive")
        ))
        val json = result.toJson()
        assertEquals(
            """{"errors":[{"field":"name","message":"required"},{"field":"age","message":"must be positive"}]}""",
            json
        )
    }

    @Test
    fun `ValidationResult throwIfInvalid does nothing when valid`() {
        ValidationResult(emptyList()).throwIfInvalid()
    }

    @Test
    fun `ValidationResult throwIfInvalid throws when invalid`() {
        assertFailsWith<ValidationException> {
            ValidationResult(listOf(FieldError("x", "bad"))).throwIfInvalid()
        }
    }

    // ── ValidationException response ─────────────────────────────────

    @Test
    fun `ValidationException is an HttpException with status 400`() {
        val ex = ValidationException(listOf(FieldError("f", "m")))
        assertEquals(400, ex.status)
    }

    @Test
    fun `ValidationException toValidationResponse returns 400 with JSON body`() {
        val ex = ValidationException(listOf(FieldError("name", "required")))
        val response = ex.toValidationResponse()
        assertEquals(400, response.statusCode)
        assertEquals("application/json", response.headers["Content-Type"])
        assertTrue(response.body.contains(""""field":"name""""))
        assertTrue(response.body.contains(""""message":"required""""))
    }
}
