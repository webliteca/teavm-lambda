package io.teavmlambda.db;

import io.teavmlambda.db.api.DbResult;
import io.teavmlambda.db.api.DbRow;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;

import java.util.AbstractList;
import java.util.List;

/**
 * Adapts a PgResult (Node.js pg query result) to the platform-neutral DbResult interface.
 */
public class JsDbResult implements DbResult {

    private final PgResult pgResult;

    public JsDbResult(PgResult pgResult) {
        this.pgResult = pgResult;
    }

    @Override
    public List<DbRow> getRows() {
        JSArray<JSObject> rows = pgResult.getRows();
        return new AbstractList<DbRow>() {
            @Override
            public DbRow get(int index) {
                return new JsDbRow(rows.get(index));
            }

            @Override
            public int size() {
                return rows.getLength();
            }
        };
    }

    @Override
    public int getRowCount() {
        return pgResult.getRowCount();
    }

    /** Returns the underlying PgResult for code that needs direct JS interop. */
    public PgResult unwrap() {
        return pgResult;
    }
}
