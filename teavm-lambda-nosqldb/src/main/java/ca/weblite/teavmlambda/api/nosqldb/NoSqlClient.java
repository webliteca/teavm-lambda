package ca.weblite.teavmlambda.api.nosqldb;

import java.util.List;

/**
 * Implementation-agnostic interface for NoSQL document database operations.
 * Analogous to JDBC's Connection/Statement for relational databases.
 *
 * Implementations are provided by teavm-lambda-dynamodb and teavm-lambda-firestore.
 */
public interface NoSqlClient {

    /**
     * Retrieves a single document by its ID.
     *
     * @param collection the collection/table name
     * @param id         the document ID / partition key
     * @return the document as a JSON string, or null if not found
     */
    String get(String collection, String id);

    /**
     * Inserts or replaces a document.
     *
     * @param collection the collection/table name
     * @param id         the document ID / partition key
     * @param json       the document content as a JSON string
     */
    void put(String collection, String id, String json);

    /**
     * Deletes a document by its ID.
     *
     * @param collection the collection/table name
     * @param id         the document ID / partition key
     */
    void delete(String collection, String id);

    /**
     * Queries documents in a collection where a field matches a condition.
     *
     * @param collection the collection/table name
     * @param field      the field name to filter on
     * @param operator   the comparison operator (e.g. "=", "<", ">", "<=", ">=", "begins_with")
     * @param value      the value to compare against
     * @return matching documents as JSON strings
     */
    List<String> query(String collection, String field, String operator, String value);

    /**
     * Lists all documents in a collection.
     *
     * @param collection the collection/table name
     * @return all documents as JSON strings
     */
    List<String> list(String collection);
}
