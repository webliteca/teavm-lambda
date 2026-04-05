package ca.weblite.teavmlambda.api.nosqldb;

/**
 * SPI interface for NoSQL database providers. Each implementation module
 * (DynamoDB, Firestore) registers a provider that can create clients
 * from a connection URI.
 */
public interface NoSqlProvider {

    /**
     * Returns the URI scheme this provider handles (e.g. "dynamodb", "firestore").
     */
    String getScheme();

    /**
     * Creates a NoSqlClient from the given connection URI.
     *
     * @param uri the full connection URI (e.g. "dynamodb://us-east-1" or "firestore://my-project")
     * @return a configured NoSqlClient
     */
    NoSqlClient create(String uri);
}
