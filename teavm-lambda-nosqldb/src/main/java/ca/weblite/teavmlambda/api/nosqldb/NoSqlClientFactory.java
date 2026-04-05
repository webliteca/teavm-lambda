package ca.weblite.teavmlambda.api.nosqldb;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory that creates NoSqlClient instances based on a connection URI.
 * The URI scheme determines which provider implementation is used:
 *
 * <ul>
 *   <li>{@code dynamodb://us-east-1} - Amazon DynamoDB in us-east-1</li>
 *   <li>{@code dynamodb://us-east-1/table-prefix} - DynamoDB with table name prefix</li>
 *   <li>{@code dynamodb://localhost:8000} - DynamoDB Local</li>
 *   <li>{@code firestore://my-project-id} - Google Cloud Firestore</li>
 * </ul>
 *
 * Providers are registered via {@link #register(NoSqlProvider)}. Implementation
 * modules call this at class-load time.
 */
public final class NoSqlClientFactory {

    private static final List<NoSqlProvider> providers = new ArrayList<>();

    private NoSqlClientFactory() {
    }

    /**
     * Registers a NoSQL provider. Called by implementation modules.
     */
    public static void register(NoSqlProvider provider) {
        providers.add(provider);
    }

    /**
     * Creates a NoSqlClient from a connection URI.
     *
     * @param uri connection URI whose scheme selects the provider
     * @return a configured NoSqlClient
     * @throws IllegalArgumentException if no provider matches the URI scheme
     */
    public static NoSqlClient create(String uri) {
        String scheme = extractScheme(uri);
        for (NoSqlProvider provider : providers) {
            if (provider.getScheme().equals(scheme)) {
                return provider.create(uri);
            }
        }
        throw new IllegalArgumentException(
                "No NoSQL provider registered for scheme: " + scheme
                + ". Available schemes: " + availableSchemes()
                + ". Ensure the corresponding module (teavm-lambda-dynamodb or "
                + "teavm-lambda-firestore) is on the classpath.");
    }

    static String extractScheme(String uri) {
        int idx = uri.indexOf("://");
        if (idx <= 0) {
            throw new IllegalArgumentException(
                    "Invalid NoSQL connection URI: " + uri
                    + ". Expected format: scheme://host (e.g. dynamodb://us-east-1)");
        }
        return uri.substring(0, idx);
    }

    private static String availableSchemes() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(providers.get(i).getScheme());
        }
        sb.append("]");
        return sb.toString();
    }
}
