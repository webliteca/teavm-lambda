package com.example.crud.repository;

import ca.weblite.teavmlambda.api.annotation.Inject;
import ca.weblite.teavmlambda.api.annotation.Repository;
import ca.weblite.teavmlambda.api.annotation.Singleton;
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DbResult;
import ca.weblite.teavmlambda.api.db.DbRow;

@Repository
@Singleton
public class UserRepository {

    private final Database db;

    @Inject
    public UserRepository(Database db) {
        this.db = db;
    }

    public String findAllJson() {
        return db.query("SELECT * FROM users ORDER BY created_at DESC").toJsonArray();
    }

    public String findByIdJson(String id) {
        DbResult result = db.query("SELECT * FROM users WHERE id = $1", id);
        if (result.getRowCount() == 0) return null;
        return result.getRows().get(0).toJson();
    }

    public void create(String id, String name, String email) {
        db.query("INSERT INTO users (id, name, email) VALUES ($1, $2, $3)",
                id, name, email);
    }

    public boolean deleteById(String id) {
        DbResult result = db.query("DELETE FROM users WHERE id = $1 RETURNING id", id);
        return result.getRowCount() > 0;
    }
}
