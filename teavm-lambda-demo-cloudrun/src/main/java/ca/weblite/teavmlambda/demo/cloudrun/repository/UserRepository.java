package ca.weblite.teavmlambda.demo.cloudrun.repository;

import ca.weblite.teavmlambda.api.annotation.Inject;
import ca.weblite.teavmlambda.api.annotation.Repository;
import ca.weblite.teavmlambda.api.annotation.Singleton;
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DbResult;
import ca.weblite.teavmlambda.demo.cloudrun.entity.User;

import java.util.ArrayList;
import java.util.List;

@Repository
@Singleton
public class UserRepository {

    private final Database db;

    @Inject
    public UserRepository(Database db) {
        this.db = db;
    }

    public List<User> findAll() {
        DbResult result = db.query("SELECT id, name, email FROM users ORDER BY id");
        List<User> users = new ArrayList<>();
        for (int i = 0; i < result.getRowCount(); i++) {
            users.add(User.fromRow(result.getRows().get(i)));
        }
        return users;
    }

    public User findById(String id) {
        DbResult result = db.query("SELECT id, name, email FROM users WHERE id = $1", id);
        if (result.getRowCount() == 0) {
            return null;
        }
        return User.fromRow(result.getRows().get(0));
    }

    public User create(String name, String email) {
        DbResult result = db.query(
                "INSERT INTO users (name, email) VALUES ($1, $2) RETURNING id, name, email",
                name, email);
        return User.fromRow(result.getRows().get(0));
    }

    public boolean deleteById(String id) {
        DbResult result = db.query("DELETE FROM users WHERE id = $1 RETURNING id", id);
        return result.getRowCount() > 0;
    }
}
