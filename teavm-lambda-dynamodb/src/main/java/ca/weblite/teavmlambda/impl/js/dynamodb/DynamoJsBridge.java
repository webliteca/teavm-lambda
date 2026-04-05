package ca.weblite.teavmlambda.impl.js.dynamodb;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Low-level JavaScript interop for the AWS SDK v3 DynamoDB Document Client.
 * Uses @aws-sdk/lib-dynamodb which handles automatic marshalling/unmarshalling
 * of native JavaScript types to DynamoDB AttributeValue format.
 */
final class DynamoJsBridge {

    private DynamoJsBridge() {
    }

    /**
     * Creates a DynamoDBDocumentClient configured for the given region.
     * If endpoint contains a port number (e.g. localhost:8000), it is treated
     * as a DynamoDB Local endpoint.
     */
    @JSBody(params = {"region", "endpoint"}, script =
            "var client = require('@aws-sdk/client-dynamodb');"
            + "var lib = require('@aws-sdk/lib-dynamodb');"
            + "var config = { region: region };"
            + "if (endpoint) { config.endpoint = endpoint; }"
            + "var ddb = new client.DynamoDBClient(config);"
            + "return lib.DynamoDBDocumentClient.from(ddb);")
    static native JSObject createClient(String region, String endpoint);

    @JSBody(params = {"docClient", "table", "key"}, script =
            "var lib = require('@aws-sdk/lib-dynamodb');"
            + "return docClient.send(new lib.GetCommand({ TableName: table, Key: key }));")
    static native JSPromise<JSObject> getItem(JSObject docClient, String table, JSObject key);

    @JSBody(params = {"docClient", "table", "item"}, script =
            "var lib = require('@aws-sdk/lib-dynamodb');"
            + "return docClient.send(new lib.PutCommand({ TableName: table, Item: item }));")
    static native JSPromise<JSObject> putItem(JSObject docClient, String table, JSObject item);

    @JSBody(params = {"docClient", "table", "key"}, script =
            "var lib = require('@aws-sdk/lib-dynamodb');"
            + "return docClient.send(new lib.DeleteCommand({ TableName: table, Key: key }));")
    static native JSPromise<JSObject> deleteItem(JSObject docClient, String table, JSObject key);

    @JSBody(params = {"docClient", "table"}, script =
            "var lib = require('@aws-sdk/lib-dynamodb');"
            + "return docClient.send(new lib.ScanCommand({ TableName: table }));")
    static native JSPromise<JSObject> scan(JSObject docClient, String table);

    @JSBody(params = {"docClient", "table", "keyCondition", "exprAttrNames", "exprAttrValues"}, script =
            "var lib = require('@aws-sdk/lib-dynamodb');"
            + "return docClient.send(new lib.QueryCommand({"
            + "  TableName: table,"
            + "  KeyConditionExpression: keyCondition,"
            + "  ExpressionAttributeNames: exprAttrNames,"
            + "  ExpressionAttributeValues: exprAttrValues"
            + "}));")
    static native JSPromise<JSObject> query(JSObject docClient, String table,
            String keyCondition, JSObject exprAttrNames, JSObject exprAttrValues);

    @JSBody(params = {"docClient", "table", "filterExpr", "exprAttrNames", "exprAttrValues"}, script =
            "var lib = require('@aws-sdk/lib-dynamodb');"
            + "return docClient.send(new lib.ScanCommand({"
            + "  TableName: table,"
            + "  FilterExpression: filterExpr,"
            + "  ExpressionAttributeNames: exprAttrNames,"
            + "  ExpressionAttributeValues: exprAttrValues"
            + "}));")
    static native JSPromise<JSObject> scanWithFilter(JSObject docClient, String table,
            String filterExpr, JSObject exprAttrNames, JSObject exprAttrValues);
}
