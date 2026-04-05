package ca.weblite.teavmlambda.impl.jvm.db;

import ca.weblite.teavmlambda.api.db.DbRow;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DbRow backed by a Map of column name to value, populated from a JDBC ResultSet.
 */
public class JdbcDbRow implements DbRow {

    private final Map<String, Object> columns;

    public JdbcDbRow(Map<String, Object> columns) {
        this.columns = columns;
    }

    @Override
    public String getString(String column) {
        Object val = columns.get(column);
        return val != null ? String.valueOf(val) : null;
    }

    @Override
    public int getInt(String column) {
        Object val = columns.get(column);
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(String.valueOf(val));
    }

    @Override
    public double getDouble(String column) {
        Object val = columns.get(column);
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        return Double.parseDouble(String.valueOf(val));
    }

    @Override
    public boolean getBoolean(String column) {
        Object val = columns.get(column);
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(String.valueOf(val));
    }

    @Override
    public boolean has(String column) {
        return columns.containsKey(column);
    }

    @Override
    public boolean isNull(String column) {
        return !columns.containsKey(column) || columns.get(column) == null;
    }

    @Override
    public String toJson() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : columns.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object val = entry.getValue();
            if (val == null) {
                sb.append("null");
            } else if (val instanceof Number) {
                sb.append(val);
            } else if (val instanceof Boolean) {
                sb.append(val);
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(val))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
