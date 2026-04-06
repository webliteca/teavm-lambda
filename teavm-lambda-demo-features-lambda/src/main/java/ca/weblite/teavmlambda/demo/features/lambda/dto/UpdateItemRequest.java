package ca.weblite.teavmlambda.demo.features.lambda.dto;

import ca.weblite.teavmlambda.api.json.JsonReader;

public final class UpdateItemRequest {

    private final String name;
    private final String description;
    private final Integer quantity;

    public UpdateItemRequest(String name, String description, Integer quantity) {
        this.name = name;
        this.description = description;
        this.quantity = quantity;
    }

    public static UpdateItemRequest fromJson(String json) {
        JsonReader reader = JsonReader.parse(json);
        return new UpdateItemRequest(
                reader.getString("name"),
                reader.getString("description"),
                reader.has("quantity") ? reader.getInt("quantity", 0) : null);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getQuantity() { return quantity; }
}
