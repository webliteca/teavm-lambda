package io.teavmlambda.dynamodb;

import io.teavmlambda.nosqldb.NoSqlClient;
import io.teavmlambda.nosqldb.NoSqlClientFactory;
import io.teavmlambda.nosqldb.NoSqlProvider;
import org.teavm.jso.JSObject;

/**
 * NoSQL provider for Amazon DynamoDB.
 *
 * <p>Connection URI formats:</p>
 * <ul>
 *   <li>{@code dynamodb://us-east-1} - DynamoDB in us-east-1, no table prefix</li>
 *   <li>{@code dynamodb://us-east-1/myprefix_} - DynamoDB with table name prefix</li>
 *   <li>{@code dynamodb://localhost:8000} - DynamoDB Local (auto-detected by port)</li>
 *   <li>{@code dynamodb://localhost:8000/myprefix_} - DynamoDB Local with prefix</li>
 * </ul>
 */
public class DynamoNoSqlProvider implements NoSqlProvider {

    static {
        NoSqlClientFactory.register(new DynamoNoSqlProvider());
    }

    /**
     * Forces class initialization, which triggers provider registration.
     * Call this from your main() before using NoSqlClientFactory.
     */
    public static void init() {
        // static initializer does the work
    }

    @Override
    public String getScheme() {
        return "dynamodb";
    }

    @Override
    public NoSqlClient create(String uri) {
        // Parse: dynamodb://host-or-region[/prefix]
        String remainder = uri.substring("dynamodb://".length());
        String hostOrRegion;
        String tablePrefix = "";

        int slashIdx = remainder.indexOf('/');
        if (slashIdx >= 0) {
            hostOrRegion = remainder.substring(0, slashIdx);
            tablePrefix = remainder.substring(slashIdx + 1);
        } else {
            hostOrRegion = remainder;
        }

        String region;
        String endpoint = null;

        if (hostOrRegion.contains(":")) {
            // Has port - treat as local endpoint (e.g. localhost:8000)
            endpoint = "http://" + hostOrRegion;
            region = "us-east-1";
        } else {
            region = hostOrRegion;
        }

        JSObject docClient = DynamoJsBridge.createClient(region, endpoint);
        return new DynamoNoSqlClient(docClient, tablePrefix);
    }
}
