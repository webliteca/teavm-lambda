package io.teavmlambda.db;

import io.teavmlambda.db.api.Database;
import io.teavmlambda.db.api.DbResult;

/**
 * Adapts the existing JSO-based Db class to the platform-neutral Database interface.
 */
public class JsDatabase implements Database {

    private final Db db;

    public JsDatabase(Db db) {
        this.db = db;
    }

    public JsDatabase(String connectionUrl) {
        this(new Db(PgPool.create(connectionUrl)));
    }

    @Override
    public DbResult query(String sql) {
        return new JsDbResult(db.query(sql));
    }

    @Override
    public DbResult query(String sql, String... params) {
        return new JsDbResult(db.query(sql, params));
    }

    @Override
    public void close() {
        // PgPool doesn't expose close through Db, but we could add it
    }

    /** Returns the underlying Db instance for code that needs direct JS interop. */
    public Db unwrap() {
        return db;
    }
}
