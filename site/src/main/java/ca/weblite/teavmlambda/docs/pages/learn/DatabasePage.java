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

public class DatabasePage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Database"))
            .child(p("teavm-lambda provides a platform-neutral database API for PostgreSQL. "
                + "The same code works on both TeaVM (Node.js pg driver) and JVM (JDBC) "
                + "targets. The API uses parameterized queries with PostgreSQL-style "
                + "positional parameters."))
            .child(sectionGettingStarted())
            .child(sectionQuerying())
            .child(sectionInsertUpdateDelete())
            .child(sectionTransactions())
            .child(sectionConfiguration())
            .build();
    }

    private static ReactElement sectionGettingStarted() {
        String javaCode = """
Database db = DatabaseFactory.create();
DbResult result = db.query(
    "SELECT * FROM users WHERE id = $1",
    userId
);
for (DbRow row : result.getRows()) {
    String name = row.getString("name");
    int age = row.getInt("age");
}""";

        String kotlinCode = """
val db = DatabaseFactory.create()
val result = db.query(
    "SELECT * FROM users WHERE id = $1",
    userId
)
result.rows.forEach { row ->
    val name = row.getString("name")
    val age = row.getInt("age")
}""";

        return Section.create().className("doc-section")
            .child(h2("Getting Started"))
            .child(p("Use DatabaseFactory.create() to obtain a Database instance. "
                + "The factory uses ServiceLoader to discover the correct implementation "
                + "for your target platform -- the Node.js pg driver when compiled with "
                + "TeaVM, or JDBC when running on the JVM."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(Callout.pitfall("Parameter syntax",
                p("PostgreSQL uses $1, $2, $3 style positional parameters. "
                    + "Do NOT use ? placeholders -- they are not supported by this API.")))
            .build();
    }

    private static ReactElement sectionQuerying() {
        String javaCode = """
// Query with multiple parameters
DbResult result = db.query(
    "SELECT * FROM users WHERE age >= $1 AND city = $2",
    minAge, city
);

// Reading results
for (DbRow row : result.getRows()) {
    int id = row.getInt("id");
    String name = row.getString("name");
    String email = row.getString("email");
    boolean active = row.getBoolean("active");
}

// Check row count
int count = result.getRowCount();""";

        String kotlinCode = """
// Query with multiple parameters
val result = db.query(
    "SELECT * FROM users WHERE age >= $1 AND city = $2",
    minAge, city
)

// Reading results
result.rows.forEach { row ->
    val id = row.getInt("id")
    val name = row.getString("name")
    val email = row.getString("email")
    val active = row.getBoolean("active")
}

// Check row count
val count = result.rowCount""";

        return Section.create().className("doc-section")
            .child(h2("Querying"))
            .child(p("The query method returns a DbResult containing the matched rows. "
                + "Each DbRow provides typed accessor methods for reading column values."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(El.div("doc-table-wrapper",
                El.table("doc-table",
                    El.classed("thead", "",
                        El.classed("tr", "",
                            El.classedText("th", "", "Method"),
                            El.classedText("th", "", "Return Type"),
                            El.classedText("th", "", "Description")
                        )
                    ),
                    El.classed("tbody", "",
                        El.classed("tr", "",
                            El.classed("td", "", code("getString(col)")),
                            El.classed("td", "", code("String")),
                            El.classedText("td", "", "Text and varchar columns")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getInt(col)")),
                            El.classed("td", "", code("int")),
                            El.classedText("td", "", "Integer columns")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getLong(col)")),
                            El.classed("td", "", code("long")),
                            El.classedText("td", "", "Bigint columns")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getDouble(col)")),
                            El.classed("td", "", code("double")),
                            El.classedText("td", "", "Numeric and float columns")
                        ),
                        El.classed("tr", "",
                            El.classed("td", "", code("getBoolean(col)")),
                            El.classed("td", "", code("boolean")),
                            El.classedText("td", "", "Boolean columns")
                        )
                    )
                )
            ))
            .build();
    }

    private static ReactElement sectionInsertUpdateDelete() {
        String javaCode = """
// Insert
db.query(
    "INSERT INTO users (name, email, age) VALUES ($1, $2, $3)",
    "Alice", "alice@example.com", 30
);

// Update
db.query(
    "UPDATE users SET email = $1 WHERE id = $2",
    "newemail@example.com", userId
);

// Delete
db.query(
    "DELETE FROM users WHERE id = $1",
    userId
);""";

        String kotlinCode = """
// Insert
db.query(
    "INSERT INTO users (name, email, age) VALUES ($1, $2, $3)",
    "Alice", "alice@example.com", 30
)

// Update
db.query(
    "UPDATE users SET email = $1 WHERE id = $2",
    "newemail@example.com", userId
)

// Delete
db.query(
    "DELETE FROM users WHERE id = $1",
    userId
)""";

        return Section.create().className("doc-section")
            .child(h2("Insert, Update, Delete"))
            .child(p("Write operations use the same query method. All parameters "
                + "are safely bound to prevent SQL injection."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionTransactions() {
        String javaCode = """
Database db = DatabaseFactory.create();
db.begin();
try {
    db.query(
        "UPDATE accounts SET balance = balance - $1 WHERE id = $2",
        amount, fromId
    );
    db.query(
        "UPDATE accounts SET balance = balance + $1 WHERE id = $2",
        amount, toId
    );
    db.commit();
} catch (Exception e) {
    db.rollback();
    throw e;
}""";

        String kotlinCode = """
val db = DatabaseFactory.create()
db.begin()
try {
    db.query(
        "UPDATE accounts SET balance = balance - $1 WHERE id = $2",
        amount, fromId
    )
    db.query(
        "UPDATE accounts SET balance = balance + $1 WHERE id = $2",
        amount, toId
    )
    db.commit()
} catch (e: Exception) {
    db.rollback()
    throw e
}""";

        return Section.create().className("doc-section")
            .child(h2("Transactions"))
            .child(p("Use begin(), commit(), and rollback() for transaction support. "
                + "Always wrap transactional code in a try-catch block to ensure "
                + "rollback on failure."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionConfiguration() {
        return Section.create().className("doc-section")
            .child(h2("Configuration"))
            .child(p("The database connection is configured via the DATABASE_URL "
                + "environment variable. This follows the standard PostgreSQL "
                + "connection string format."))
            .child(CodeBlock.create(
                "DATABASE_URL=postgresql://user:password@host:5432/dbname",
                "bash"))
            .child(Callout.note("Environment variable",
                p("Set DATABASE_URL in your deployment environment. For local "
                    + "development, use Docker Compose to run PostgreSQL and set "
                    + "the variable in your docker-compose.yml or .env file.")))
            .build();
    }
}
