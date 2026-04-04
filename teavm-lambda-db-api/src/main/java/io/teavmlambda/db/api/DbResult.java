package io.teavmlambda.db.api;

import java.util.List;

/**
 * Platform-neutral database query result.
 */
public interface DbResult {

    List<DbRow> getRows();

    int getRowCount();
}
