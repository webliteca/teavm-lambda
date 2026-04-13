package ca.weblite.teavmlambda.demo.kotlin.todo

import ca.weblite.teavmlambda.api.Container
import ca.weblite.teavmlambda.api.Platform
import ca.weblite.teavmlambda.api.Router
import ca.weblite.teavmlambda.api.db.Database
import ca.weblite.teavmlambda.api.db.DatabaseFactory
import ca.weblite.teavmlambda.generated.GeneratedContainer
import ca.weblite.teavmlambda.generated.GeneratedRouter

/**
 * Todo App Entry Point
 *
 * This is a simple todo list API built with teavm-lambda in Kotlin.
 * It demonstrates:
 *  - Resource routing with @Path, @GET, @POST, @PUT, @DELETE
 *  - JSON serialization/deserialization
 *  - PostgreSQL database access
 *  - Dependency injection with @Component and @Service
 *
 * Build & run:
 *   # TeaVM/Node.js (default)
 *   mvn clean package
 *   sam local start-api --template template.yaml
 *
 *   # Or JVM with embedded HTTP server
 *   mvn clean package -P jvm-server
 *   java -jar target/teavm-lambda-demo-kotlin-todo-*-shaded.jar
 */

fun main() {
    val dbUrl = Platform.env("DATABASE_URL", "postgresql://demo:demo@localhost:5432/demo")

    // Create container and register external dependencies
    val container: Container = GeneratedContainer()
    container.register(Database::class.java, DatabaseFactory.create(dbUrl))

    val router: Router = GeneratedRouter(container)
    Platform.start(router)
}
