package ca.weblite.teavmlambda.docs.pages.learn;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder.Div;
import ca.weblite.teavmreact.html.DomBuilder.Section;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

import ca.weblite.teavmlambda.docs.El;
import ca.weblite.teavmlambda.docs.components.CodeBlock;
import ca.weblite.teavmlambda.docs.components.CodeTabs;
import ca.weblite.teavmlambda.docs.components.Callout;

public class ValidationPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Validation"))
            .child(p("teavm-lambda includes a compile-time validation system for request "
                + "parameters and body fields. Annotate your handler parameters with "
                + "validation constraints and the annotation processor generates the "
                + "validation logic at compile time -- no runtime reflection needed."))
            .child(sectionAnnotations())
            .child(sectionParameterValidation())
            .child(sectionValidationResult())
            .child(sectionProblemDetail())
            .child(sectionFullExample())
            .build();
    }

    private static ReactElement sectionAnnotations() {
        return Section.create().className("doc-section")
            .child(h2("Validation Annotations"))
            .child(p("Apply these annotations to handler method parameters to enforce "
                + "constraints. The annotation processor generates validation code that "
                + "runs before your handler method is invoked."))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Annotation"),
                            El.classedText("th", "", "Applies To"),
                            El.classedText("th", "", "Description")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("@NotNull")),
                            El.classedText("td", "", "Any parameter"),
                            El.classedText("td", "", "Parameter must not be null")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("@NotEmpty")),
                            El.classedText("td", "", "String"),
                            El.classedText("td", "", "String must not be null or empty")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("@Min(value)")),
                            El.classedText("td", "", "int, long, double"),
                            El.classedText("td", "", "Numeric value must be >= the specified minimum")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("@Max(value)")),
                            El.classedText("td", "", "int, long, double"),
                            El.classedText("td", "", "Numeric value must be <= the specified maximum")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("@Pattern(regex)")),
                            El.classedText("td", "", "String"),
                            El.classedText("td", "", "String must match the given regular expression")
                        )
                    )
                )
            ))
            .build();
    }

    private static ReactElement sectionParameterValidation() {
        String javaCode = """
@Path("/users")
@Component
@Singleton
public class UserResource {

    @POST
    public Response createUser(
            @Body @NotNull String body,
            @QueryParam("name") @NotEmpty String name,
            @QueryParam("email") @Pattern("[^@]+@[^@]+\\\\.[^@]+") String email,
            @QueryParam("age") @Min(0) @Max(150) int age) {
        // This code only runs if all validations pass
        return Response.ok("{\\"created\\":\\"" + name + "\\"}");
    }
}""";

        String kotlinCode = """
app {
    routes {
        post("/users") {
            val name = queryParam("name").notEmpty()
            val email = queryParam("email").pattern("[^@]+@[^@]+\\.[^@]+")
            val age = queryParam("age").toInt().min(0).max(150)
            val body = body().notNull()

            // This code only runs if all validations pass
            ok(json { "created" to name })
        }
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("Parameter Validation"))
            .child(p("Add validation annotations directly to your handler method parameters. "
                + "When a request fails validation, the framework automatically returns "
                + "a 400 Bad Request response with a ProblemDetail body describing "
                + "the validation errors."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionValidationResult() {
        String javaCode = """
// Manual validation in handler code
ValidationResult result = new ValidationResult();

if (name == null || name.isBlank()) {
    result.addError("name", "Name is required");
}
if (age < 0 || age > 150) {
    result.addError("age", "Age must be between 0 and 150");
}
if (email != null && !email.matches("[^@]+@[^@]+\\\\.[^@]+")) {
    result.addError("email", "Invalid email format");
}

if (result.hasErrors()) {
    return result.toResponse();  // Returns 400 with ProblemDetail
}

// Proceed with valid data...""";

        String kotlinCode = """
// Manual validation in handler code
val result = ValidationResult()

if (name.isNullOrBlank()) {
    result.addError("name", "Name is required")
}
if (age < 0 || age > 150) {
    result.addError("age", "Age must be between 0 and 150")
}
if (email != null && !email.matches(Regex("[^@]+@[^@]+\\.[^@]+"))) {
    result.addError("email", "Invalid email format")
}

if (result.hasErrors()) {
    return result.toResponse()  // Returns 400 with ProblemDetail
}

// Proceed with valid data...""";

        return Section.create().className("doc-section")
            .child(h2("ValidationResult"))
            .child(p("For complex validation logic that goes beyond simple annotations, "
                + "use ValidationResult to collect errors programmatically. Call "
                + "toResponse() to generate a standard 400 error response."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionProblemDetail() {
        String responseExample = """
{
    "type": "about:blank",
    "title": "Bad Request",
    "status": 400,
    "detail": "Validation failed",
    "errors": [
        {
            "field": "name",
            "message": "Name is required"
        },
        {
            "field": "age",
            "message": "Age must be between 0 and 150"
        }
    ]
}""";

        String javaCode = """
// Create a ProblemDetail manually
ProblemDetail problem = ProblemDetail.builder()
    .type("https://example.com/errors/not-found")
    .title("Not Found")
    .status(404)
    .detail("User with id 42 was not found")
    .build();

return Response.status(404)
    .header("Content-Type", "application/problem+json")
    .body(problem.toJson());""";

        String kotlinCode = """
// Create a ProblemDetail manually
val problem = ProblemDetail.builder()
    .type("https://example.com/errors/not-found")
    .title("Not Found")
    .status(404)
    .detail("User with id 42 was not found")
    .build()

return status(404)
    .header("Content-Type", "application/problem+json")
    .body(problem.toJson())""";

        return Section.create().className("doc-section")
            .child(h2("ProblemDetail Responses"))
            .child(p("Validation errors are returned as RFC 9457 Problem Detail objects. "
                + "The response uses the application/problem+json content type and "
                + "includes a structured list of field-level errors."))
            .child(CodeBlock.create(responseExample, "json"))
            .child(p("You can also create ProblemDetail responses manually for custom "
                + "error scenarios:"))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionFullExample() {
        String javaCode = """
@Path("/products")
@Component
@Singleton
public class ProductResource {

    @POST
    public Response createProduct(
            @Body @NotNull String body,
            @QueryParam("name") @NotEmpty String name,
            @QueryParam("price") @Min(0) double price,
            @QueryParam("sku") @Pattern("[A-Z]{3}-[0-9]{4}") String sku) {
        // All validations passed
        return Response.status(201)
            .body("{\\"name\\":\\"" + name + "\\","
                + "\\"price\\":" + price + ","
                + "\\"sku\\":\\"" + sku + "\\"}");
    }

    @PUT
    @Path("/{id}")
    public Response updateProduct(
            @PathParam("id") @NotEmpty String id,
            @QueryParam("price") @Min(0) double price) {
        return Response.ok("{\\"updated\\":\\"" + id + "\\"}");
    }
}""";

        String kotlinCode = """
app {
    routes {
        post("/products") {
            val body = body().notNull()
            val name = queryParam("name").notEmpty()
            val price = queryParam("price").toDouble().min(0.0)
            val sku = queryParam("sku").pattern("[A-Z]{3}-[0-9]{4}")

            // All validations passed
            status(201, json {
                "name" to name
                "price" to price
                "sku" to sku
            })
        }

        put("/products/{id}") {
            val id = pathParam("id").notEmpty()
            val price = queryParam("price").toDouble().min(0.0)
            ok(json { "updated" to id })
        }
    }
}""";

        return Section.create().className("doc-section")
            .child(h2("Full Example"))
            .child(p("Here is a complete resource showing validation on both creation "
                + "and update endpoints:"))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(Callout.note("Compile-time validation",
                p("The annotation processor generates all validation logic at compile time. "
                    + "There is no runtime reflection overhead. Invalid requests are rejected "
                    + "before your handler code executes.")))
            .child(Callout.pitfall("Annotation order",
                p("When combining multiple annotations on a single parameter, @NotNull "
                    + "and @NotEmpty are checked first. If the value is null, @Min, @Max, "
                    + "and @Pattern checks are skipped to avoid NullPointerExceptions.")))
            .build();
    }
}
