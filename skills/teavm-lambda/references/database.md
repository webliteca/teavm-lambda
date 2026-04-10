# Database Access (PostgreSQL)

> Read this when the user needs to connect to PostgreSQL, run queries, or implement the repository pattern.

## Connection

```java
Database db = DatabaseFactory.create("postgresql://user:pass@host:5432/dbname");
```

Register on the container before constructing GeneratedRouter:

```java
Container container = new GeneratedContainer();
container.register(Database.class, DatabaseFactory.create(dbUrl));
Router router = new GeneratedRouter(container);
```

Environment variable: `DATABASE_URL`

```java
String dbUrl = Platform.env("DATABASE_URL", "postgresql://demo:demo@localhost:5432/demo");
```

**Required modules**:
- Always: `teavm-lambda-db-api`
- Node.js: `teavm-lambda-db` (uses pg driver via JSO)
- JVM: `teavm-lambda-db-jvm` (uses JDBC) + `org.postgresql:postgresql:42.7.3`

## Queries

```java
// Simple query
DbResult result = db.query("SELECT * FROM users ORDER BY created_at DESC");

// Parameterized query — use $1, $2, etc. (NOT ?)
DbResult result = db.query(
    "SELECT * FROM users WHERE id = $1",
    userId
);

// Insert
db.query(
    "INSERT INTO users (id, name, email) VALUES ($1, $2, $3)",
    id, name, email
);

// Update
db.query(
    "UPDATE users SET name = $1 WHERE id = $2",
    newName, id
);

// Delete
db.query("DELETE FROM users WHERE id = $1", id);
```

**All parameters are strings.** Even numeric values pass as strings — PostgreSQL handles the cast.

## Reading results

```java
DbResult result = db.query("SELECT * FROM users");

// Row count
int count = result.getRowCount();

// Iterate rows
for (DbRow row : result.getRows()) {
    String id = row.getString("id");
    String name = row.getString("name");
    int age = row.getInt("age");
    double score = row.getDouble("score");
    boolean active = row.getBoolean("active");

    // Check column existence/nullability
    if (row.has("email") && !row.isNull("email")) {
        String email = row.getString("email");
    }
}

// Serialize to JSON
String json = result.toJsonArray();  // [{"id":"1","name":"Alice"}, ...]
String rowJson = result.getRows().get(0).toJson();  // {"id":"1","name":"Alice"}
```

## Repository pattern

```java
@Repository
@Singleton
public class UserRepository {
    private final Database db;

    @Inject
    public UserRepository(Database db) {
        this.db = db;
    }

    public List<DbRow> findAll() {
        return db.query("SELECT * FROM users ORDER BY created_at DESC").getRows();
    }

    public DbRow findById(String id) {
        DbResult result = db.query("SELECT * FROM users WHERE id = $1", id);
        return result.getRowCount() > 0 ? result.getRows().get(0) : null;
    }

    public void create(String id, String name, String email) {
        db.query(
            "INSERT INTO users (id, name, email) VALUES ($1, $2, $3)",
            id, name, email
        );
    }

    public boolean deleteById(String id) {
        DbResult result = db.query("DELETE FROM users WHERE id = $1 RETURNING id", id);
        return result.getRowCount() > 0;
    }
}
```

## Entity pattern

Entities are simple POJOs with static factory methods for mapping from DbRow and JSON:

```java
public class User {
    private final String id;
    private final String name;
    private final String email;

    public User(String id, String name, String email) {
        this.id = id; this.name = name; this.email = email;
    }

    public static User fromRow(DbRow row) {
        return new User(row.getString("id"), row.getString("name"), row.getString("email"));
    }

    public static User fromJson(String json) {
        JsonReader r = JsonReader.parse(json);
        return new User(r.getString("id"), r.getString("name"), r.getString("email"));
    }

    public String toJson() {
        return JsonBuilder.object()
                .put("id", id)
                .put("name", name)
                .put("email", email)
                .build();
    }

    public static String toJsonArray(List<User> users) {
        JsonBuilder arr = JsonBuilder.array();
        for (User u : users) arr.add(u.toJson());
        return arr.build();
    }

    // getters ...
}
```

## Cleanup

```java
db.close();  // close the connection
```
