package ca.weblite.teavmlambda.demo.features.lambda.service;

import ca.weblite.teavmlambda.api.annotation.Inject;
import ca.weblite.teavmlambda.api.annotation.Service;
import ca.weblite.teavmlambda.api.annotation.Singleton;
import ca.weblite.teavmlambda.demo.features.lambda.dto.CreateItemRequest;
import ca.weblite.teavmlambda.demo.features.lambda.dto.UpdateItemRequest;
import ca.weblite.teavmlambda.demo.features.lambda.entity.Item;
import ca.weblite.teavmlambda.demo.features.lambda.repository.ItemRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@Singleton
public class ItemService {

    private final ItemRepository repository;

    @Inject
    public ItemService(ItemRepository repository) {
        this.repository = repository;
    }

    public List<Item> listItems() {
        var result = repository.findAll();
        List<Item> items = new ArrayList<>();
        for (var row : result.getRows()) {
            items.add(Item.fromRow(row));
        }
        return items;
    }

    public Item getItem(String id) {
        var result = repository.findById(id);
        if (result.getRows().isEmpty()) return null;
        return Item.fromRow(result.getRows().get(0));
    }

    public Item createItem(CreateItemRequest request) {
        var result = repository.create(
                request.getName(), request.getDescription(), request.getQuantity());
        return Item.fromRow(result.getRows().get(0));
    }

    public Item updateItem(String id, UpdateItemRequest request) {
        var result = repository.update(id, request.getName(), request.getDescription(), request.getQuantity());
        if (result.getRows().isEmpty()) return null;
        return Item.fromRow(result.getRows().get(0));
    }

    public boolean deleteItem(String id) {
        var result = repository.deleteById(id);
        return result.getRowCount() > 0;
    }
}
