package io.teavmlambda.firestore;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Low-level JavaScript interop for the Google Cloud Firestore Node.js SDK
 * ({@code @google-cloud/firestore}).
 */
final class FirestoreJsBridge {

    private FirestoreJsBridge() {
    }

    /**
     * Creates a Firestore client for the given project ID.
     * If projectId is empty, the SDK uses the default project from the environment.
     */
    @JSBody(params = {"projectId"}, script =
            "var Firestore = require('@google-cloud/firestore').Firestore;"
            + "var config = {};"
            + "if (projectId) { config.projectId = projectId; }"
            + "return new Firestore(config);")
    static native JSObject createClient(String projectId);

    @JSBody(params = {"db", "collection", "docId"}, script =
            "return db.collection(collection).doc(docId).get();")
    static native JSPromise<JSObject> getDocument(JSObject db, String collection, String docId);

    @JSBody(params = {"db", "collection", "docId", "data"}, script =
            "return db.collection(collection).doc(docId).set(data);")
    static native JSPromise<JSObject> setDocument(JSObject db, String collection, String docId, JSObject data);

    @JSBody(params = {"db", "collection", "docId"}, script =
            "return db.collection(collection).doc(docId).delete();")
    static native JSPromise<JSObject> deleteDocument(JSObject db, String collection, String docId);

    @JSBody(params = {"db", "collection"}, script =
            "return db.collection(collection).get();")
    static native JSPromise<JSObject> listDocuments(JSObject db, String collection);

    @JSBody(params = {"db", "collection", "field", "operator", "value"}, script =
            "return db.collection(collection).where(field, operator, value).get();")
    static native JSPromise<JSObject> queryDocuments(JSObject db, String collection,
            String field, String operator, String value);

    /**
     * Checks whether a Firestore DocumentSnapshot exists.
     */
    @JSBody(params = {"snapshot"}, script = "return snapshot.exists;")
    static native boolean snapshotExists(JSObject snapshot);

    /**
     * Extracts the data from a Firestore DocumentSnapshot as a plain JS object.
     */
    @JSBody(params = {"snapshot"}, script = "return snapshot.data();")
    static native JSObject snapshotData(JSObject snapshot);

    /**
     * Extracts the document ID from a Firestore DocumentSnapshot.
     */
    @JSBody(params = {"snapshot"}, script = "return snapshot.id;")
    static native String snapshotId(JSObject snapshot);

    /**
     * Returns the docs array from a Firestore QuerySnapshot.
     */
    @JSBody(params = {"querySnapshot"}, script = "return querySnapshot.docs;")
    static native JSObject querySnapshotDocs(JSObject querySnapshot);
}
