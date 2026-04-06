package ca.weblite.teavmlambda.demo.features.lambda.entity;

import ca.weblite.teavmlambda.api.db.DbRow;
import ca.weblite.teavmlambda.api.json.JsonBuilder;

import java.util.List;

public final class Item {

    private final int id;
    private final String name;
    private final String description;
    private final int quantity;

    public Item(int id, String name, String description, int quantity) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
    }

    public static Item fromRow(DbRow row) {
        return new Item(
                row.getInt("id"),
                row.getString("name"),
                row.has("description") && !row.isNull("description") ? row.getString("description") : null,
                row.getInt("quantity"));
    }

    public String toJson() {
        return JsonBuilder.object()
                .put("id", id)
                .put("name", name)
                .put("description", description)
                .put("quantity", quantity)
                .build();
    }

    public static String toJsonArray(List<Item> items) {
        JsonBuilder array = JsonBuilder.array();
        for (Item item : items) {
            array.add(item.toJson());
        }
        return array.build();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
}
