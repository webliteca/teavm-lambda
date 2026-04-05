package ca.weblite.teavmlambda.api.db;

import java.util.List;

/**
 * Platform-neutral database query result.
 */
public interface DbResult {

    List<DbRow> getRows();

    int getRowCount();

    /**
     * Serializes all rows as a JSON array string.
     * Convenience method replacing manual iteration with JsUtil.toJson().
     */
    default String toJsonArray() {
        List<DbRow> rows = getRows();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(rows.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }
}
