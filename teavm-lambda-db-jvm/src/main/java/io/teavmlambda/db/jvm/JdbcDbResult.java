package io.teavmlambda.db.jvm;

import io.teavmlambda.db.api.DbResult;
import io.teavmlambda.db.api.DbRow;

import java.util.List;

/**
 * DbResult backed by a pre-materialized list of JDBC rows.
 */
public class JdbcDbResult implements DbResult {

    private final List<DbRow> rows;

    public JdbcDbResult(List<DbRow> rows) {
        this.rows = rows;
    }

    @Override
    public List<DbRow> getRows() {
        return rows;
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }
}
