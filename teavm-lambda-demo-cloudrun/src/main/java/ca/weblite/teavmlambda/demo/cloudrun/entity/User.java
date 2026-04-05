package ca.weblite.teavmlambda.demo.cloudrun.entity;

import ca.weblite.teavmlambda.api.db.DbRow;

/**
 * User entity mapped to the {@code users} database table.
 */
public class User {

    private int id;
    private String name;
    private String email;

    public User() {
    }

    public User(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public static User fromRow(DbRow row) {
        return new User(
                row.getInt("id"),
                row.getString("name"),
                row.getString("email")
        );
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String toJson() {
        return "{\"id\":" + id
                + ",\"name\":\"" + escapeJson(name)
                + "\",\"email\":\"" + escapeJson(email) + "\"}";
    }

    public static String toJsonArray(java.util.List<User> users) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(users.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
