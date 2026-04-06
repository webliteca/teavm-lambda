package ca.weblite.teavmlambda.demo.features.lambda.repository;

import ca.weblite.teavmlambda.api.annotation.Inject;
import ca.weblite.teavmlambda.api.annotation.Repository;
import ca.weblite.teavmlambda.api.annotation.Singleton;
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DbResult;

@Repository
@Singleton
public class ItemRepository {

    private final Database db;

    @Inject
    public ItemRepository(Database db) {
        this.db = db;
    }

    public DbResult findAll() {
        return db.query("SELECT id, name, description, quantity FROM items ORDER BY id");
    }

    public DbResult findById(String id) {
        return db.query("SELECT id, name, description, quantity FROM items WHERE id = $1", id);
    }

    public DbResult create(String name, String description, int quantity) {
        return db.query(
                "INSERT INTO items (name, description, quantity) VALUES ($1, $2, $3) RETURNING id, name, description, quantity",
                name, description, String.valueOf(quantity));
    }

    public DbResult update(String id, String name, String description, Integer quantity) {
        StringBuilder sql = new StringBuilder("UPDATE items SET ");
        java.util.List<String> params = new java.util.ArrayList<>();
        int paramIdx = 1;
        boolean first = true;

        if (name != null) {
            if (!first) sql.append(", ");
            sql.append("name = $").append(paramIdx++);
            params.add(name);
            first = false;
        }
        if (description != null) {
            if (!first) sql.append(", ");
            sql.append("description = $").append(paramIdx++);
            params.add(description);
            first = false;
        }
        if (quantity != null) {
            if (!first) sql.append(", ");
            sql.append("quantity = $").append(paramIdx++);
            params.add(String.valueOf(quantity));
            first = false;
        }

        if (first) {
            // No fields to update
            return findById(id);
        }

        sql.append(" WHERE id = $").append(paramIdx);
        params.add(id);
        sql.append(" RETURNING id, name, description, quantity");

        return db.query(sql.toString(), params.toArray(new String[0]));
    }

    public DbResult deleteById(String id) {
        return db.query("DELETE FROM items WHERE id = $1", id);
    }
}
