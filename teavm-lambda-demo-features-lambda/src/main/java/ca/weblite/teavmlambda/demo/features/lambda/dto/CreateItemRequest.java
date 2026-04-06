package ca.weblite.teavmlambda.demo.features.lambda.dto;

import ca.weblite.teavmlambda.api.json.JsonReader;

public final class CreateItemRequest {

    private final String name;
    private final String description;
    private final int quantity;

    public CreateItemRequest(String name, String description, int quantity) {
        this.name = name;
        this.description = description;
        this.quantity = quantity;
    }

    public static CreateItemRequest fromJson(String json) {
        JsonReader reader = JsonReader.parse(json);
        return new CreateItemRequest(
                reader.getString("name"),
                reader.getString("description"),
                reader.getInt("quantity", 0));
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
}
