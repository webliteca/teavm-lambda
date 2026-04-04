package io.teavmlambda.dynamodb;

import io.teavmlambda.nosqldb.AsyncBridge;
import io.teavmlambda.nosqldb.JsUtil;
import io.teavmlambda.nosqldb.NoSqlClient;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

import java.util.ArrayList;
import java.util.List;

/**
 * DynamoDB implementation of NoSqlClient.
 *
 * <p>Uses the AWS SDK v3 DynamoDB Document Client under the hood, which
 * automatically marshals/unmarshals between native JS types and DynamoDB
 * AttributeValue format.</p>
 *
 * <p>Each "collection" maps to a DynamoDB table name. An optional table prefix
 * can be configured via the connection URI. The document ID maps to a partition
 * key named "id".</p>
 */
public class DynamoNoSqlClient implements NoSqlClient {

    private static final String PARTITION_KEY = "id";

    private final JSObject docClient;
    private final String tablePrefix;

    DynamoNoSqlClient(JSObject docClient, String tablePrefix) {
        this.docClient = docClient;
        this.tablePrefix = tablePrefix;
    }

    private String tableName(String collection) {
        return tablePrefix.isEmpty() ? collection : tablePrefix + collection;
    }

    @Override
    public String get(String collection, String id) {
        JSObject key = JsUtil.newObject();
        JsUtil.setProperty(key, PARTITION_KEY, id);

        JSObject result = AsyncBridge.await(
                DynamoJsBridge.getItem(docClient, tableName(collection), key));

        if (!JsUtil.hasProperty(result, "Item")) {
            return null;
        }
        JSObject item = JsUtil.getObjectProperty(result, "Item");
        return JsUtil.toJson(item);
    }

    @Override
    public void put(String collection, String id, String json) {
        JSObject item = JsUtil.parseJson(json);
        JsUtil.setProperty(item, PARTITION_KEY, id);

        AsyncBridge.awaitVoid(
                DynamoJsBridge.putItem(docClient, tableName(collection), item));
    }

    @Override
    public void delete(String collection, String id) {
        JSObject key = JsUtil.newObject();
        JsUtil.setProperty(key, PARTITION_KEY, id);

        AsyncBridge.awaitVoid(
                DynamoJsBridge.deleteItem(docClient, tableName(collection), key));
    }

    @Override
    public List<String> query(String collection, String field, String operator, String value) {
        JSObject exprAttrNames = JsUtil.newObject();
        JsUtil.setProperty(exprAttrNames, "#f", field);

        JSObject exprAttrValues = JsUtil.newObject();
        JsUtil.setProperty(exprAttrValues, ":v", value);

        String filterExpr = toFilterExpression(operator);

        JSObject result = AsyncBridge.await(
                DynamoJsBridge.scanWithFilter(docClient, tableName(collection),
                        filterExpr, exprAttrNames, exprAttrValues));

        return extractItems(result);
    }

    @Override
    public List<String> list(String collection) {
        JSObject result = AsyncBridge.await(
                DynamoJsBridge.scan(docClient, tableName(collection)));
        return extractItems(result);
    }

    private static String toFilterExpression(String operator) {
        if ("begins_with".equals(operator)) {
            return "begins_with(#f, :v)";
        }
        return "#f " + operator + " :v";
    }

    private static List<String> extractItems(JSObject result) {
        List<String> items = new ArrayList<>();
        if (JsUtil.hasProperty(result, "Items")) {
            JSObject arr = JsUtil.getObjectProperty(result, "Items");
            int len = JsUtil.arrayLength(arr);
            for (int i = 0; i < len; i++) {
                items.add(JsUtil.toJson(JsUtil.arrayGet(arr, i)));
            }
        }
        return items;
    }
}
