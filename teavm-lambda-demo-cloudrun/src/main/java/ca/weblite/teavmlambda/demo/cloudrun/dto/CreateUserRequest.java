package ca.weblite.teavmlambda.demo.cloudrun.dto;

import ca.weblite.teavmlambda.api.db.DbRow;
import ca.weblite.teavmlambda.api.db.Json;

/**
 * DTO for user creation requests.
 */
public class CreateUserRequest {

    private final String name;
    private final String email;

    public CreateUserRequest(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public static CreateUserRequest fromJson(String json) {
        DbRow row = Json.parse(json);
        return new CreateUserRequest(row.getString("name"), row.getString("email"));
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
}
