package ca.weblite.teavmlambda.impl.js.firestore;

import ca.weblite.teavmlambda.api.nosqldb.AsyncBridge;
import ca.weblite.teavmlambda.api.nosqldb.JsUtil;
import ca.weblite.teavmlambda.api.nosqldb.NoSqlClient;
import org.teavm.jso.JSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Firestore implementation of NoSqlClient.
 *
 * <p>Each "collection" maps directly to a Firestore collection.
 * The document ID is the Firestore document ID.</p>
 *
 * <p>On read, the returned JSON includes an "id" field with the document ID.</p>
 */
public class FirestoreNoSqlClient implements NoSqlClient {

    private final JSObject db;

    FirestoreNoSqlClient(JSObject db) {
        this.db = db;
    }

    @Override
    public String get(String collection, String id) {
        JSObject snapshot = AsyncBridge.await(
                FirestoreJsBridge.getDocument(db, collection, id));

        if (!FirestoreJsBridge.snapshotExists(snapshot)) {
            return null;
        }

        JSObject data = FirestoreJsBridge.snapshotData(snapshot);
        JsUtil.setProperty(data, "id", id);
        return JsUtil.toJson(data);
    }

    @Override
    public void put(String collection, String id, String json) {
        JSObject data = JsUtil.parseJson(json);
        AsyncBridge.awaitVoid(
                FirestoreJsBridge.setDocument(db, collection, id, data));
    }

    @Override
    public void delete(String collection, String id) {
        AsyncBridge.awaitVoid(
                FirestoreJsBridge.deleteDocument(db, collection, id));
    }

    @Override
    public List<String> query(String collection, String field, String operator, String value) {
        JSObject querySnapshot = AsyncBridge.await(
                FirestoreJsBridge.queryDocuments(db, collection, field, operator, value));
        return extractDocs(querySnapshot);
    }

    @Override
    public List<String> list(String collection) {
        JSObject querySnapshot = AsyncBridge.await(
                FirestoreJsBridge.listDocuments(db, collection));
        return extractDocs(querySnapshot);
    }

    private static List<String> extractDocs(JSObject querySnapshot) {
        List<String> results = new ArrayList<>();
        JSObject docs = FirestoreJsBridge.querySnapshotDocs(querySnapshot);
        int len = JsUtil.arrayLength(docs);
        for (int i = 0; i < len; i++) {
            JSObject docSnapshot = JsUtil.arrayGet(docs, i);
            JSObject data = FirestoreJsBridge.snapshotData(docSnapshot);
            String docId = FirestoreJsBridge.snapshotId(docSnapshot);
            JsUtil.setProperty(data, "id", docId);
            results.add(JsUtil.toJson(data));
        }
        return results;
    }
}
