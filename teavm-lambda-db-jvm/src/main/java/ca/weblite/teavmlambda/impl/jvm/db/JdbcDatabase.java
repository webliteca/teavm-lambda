package ca.weblite.teavmlambda.impl.jvm.db;

import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DbResult;
import ca.weblite.teavmlambda.api.db.DbRow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC-based Database implementation for JVM deployment.
 * <p>
 * Accepts PostgreSQL-style connection URLs (postgresql://user:pass@host:port/db)
 * and converts them to JDBC format. Also supports native JDBC URLs.
 * <p>
 * Uses pg-style $1, $2 parameter placeholders and converts them to JDBC ? placeholders.
 */
public class JdbcDatabase implements Database {

    private final String jdbcUrl;
    private Connection connection;

    public JdbcDatabase(String connectionUrl) {
        this.jdbcUrl = toJdbcUrl(connectionUrl);
    }

    @Override
    public DbResult query(String sql) {
        return query(sql, new String[0]);
    }

    @Override
    public DbResult query(String sql, String... params) {
        String jdbcSql = convertPlaceholders(sql);
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(jdbcSql);
            for (int i = 0; i < params.length; i++) {
                stmt.setString(i + 1, params[i]);
            }

            boolean hasResultSet = stmt.execute();
            if (hasResultSet) {
                ResultSet rs = stmt.getResultSet();
                List<DbRow> rows = materialize(rs);
                rs.close();
                stmt.close();
                return new JdbcDbResult(rows);
            } else {
                int updateCount = stmt.getUpdateCount();
                stmt.close();
                return new JdbcDbResult(new ArrayList<>());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            connection = null;
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(jdbcUrl);
        }
        return connection;
    }

    private static List<DbRow> materialize(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        String[] columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = meta.getColumnLabel(i + 1);
        }

        List<DbRow> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < columnCount; i++) {
                map.put(columnNames[i], rs.getObject(i + 1));
            }
            rows.add(new JdbcDbRow(map));
        }
        return rows;
    }

    /**
     * Converts PostgreSQL-style connection URL to JDBC format.
     * Input:  postgresql://user:pass@host:port/db
     * Output: jdbc:postgresql://host:port/db?user=user&password=pass
     */
    static String toJdbcUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Connection URL must not be null or empty");
        }
        if (url.startsWith("jdbc:")) {
            return url;
        }
        if (!url.startsWith("postgresql://")) {
            throw new IllegalArgumentException("Unsupported URL scheme: " + url);
        }

        String rest = url.substring("postgresql://".length());
        String userInfo = null;
        int atIdx = rest.indexOf('@');
        if (atIdx >= 0) {
            userInfo = rest.substring(0, atIdx);
            rest = rest.substring(atIdx + 1);
        }

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(rest);
        if (userInfo != null) {
            int colonIdx = userInfo.indexOf(':');
            if (colonIdx >= 0) {
                String user = userInfo.substring(0, colonIdx);
                String pass = userInfo.substring(colonIdx + 1);
                char separator = rest.contains("?") ? '&' : '?';
                jdbc.append(separator).append("user=").append(user)
                        .append("&password=").append(pass);
            } else {
                char separator = rest.contains("?") ? '&' : '?';
                jdbc.append(separator).append("user=").append(userInfo);
            }
        }
        return jdbc.toString();
    }

    /**
     * Converts pg-style $1, $2 placeholders to JDBC ? placeholders.
     */
    static String convertPlaceholders(String sql) {
        return sql.replaceAll("\\$\\d+", "?");
    }
}
