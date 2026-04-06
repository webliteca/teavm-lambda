package ca.weblite.teavmlambda.api.json;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight JSON object reader with typed accessors.
 * Self-contained -- no external dependencies.
 * <p>
 * Usage:
 * <pre>
 * JsonReader reader = JsonReader.parse(body);
 * String name = reader.getString("name");
 * int age = reader.getInt("age", 0);
 * boolean active = reader.getBoolean("active", false);
 * </pre>
 */
public final class JsonReader {

    private final Map<String, Object> map;

    private JsonReader(Map<String, Object> map) {
        this.map = map;
    }

    /**
     * Parses a JSON object string into a JsonReader.
     */
    public static JsonReader parse(String json) {
        if (json == null || json.isBlank()) {
            return new JsonReader(new LinkedHashMap<>());
        }
        return new JsonReader(parseObject(json.trim(), new int[]{0}));
    }

    public String getString(String key) {
        Object val = map.get(key);
        if (val == null) return null;
        return String.valueOf(val);
    }

    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(val));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(String.valueOf(val));
    }

    public boolean has(String key) {
        return map.containsKey(key) && map.get(key) != null;
    }

    // --- Minimal recursive-descent JSON parser ---

    private static Map<String, Object> parseObject(String json, int[] pos) {
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace(json, pos);
        if (pos[0] >= json.length() || json.charAt(pos[0]) != '{') return result;
        pos[0]++; // skip '{'
        skipWhitespace(json, pos);
        if (pos[0] < json.length() && json.charAt(pos[0]) == '}') {
            pos[0]++;
            return result;
        }
        while (pos[0] < json.length()) {
            skipWhitespace(json, pos);
            String key = parseString(json, pos);
            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ':') pos[0]++;
            skipWhitespace(json, pos);
            Object value = parseValue(json, pos);
            result.put(key, value);
            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
            } else {
                break;
            }
        }
        if (pos[0] < json.length() && json.charAt(pos[0]) == '}') pos[0]++;
        return result;
    }

    private static Object parseValue(String json, int[] pos) {
        skipWhitespace(json, pos);
        if (pos[0] >= json.length()) return null;
        char c = json.charAt(pos[0]);
        if (c == '"') return parseString(json, pos);
        if (c == '{') return parseObject(json, pos);
        if (c == '[') return parseArray(json, pos);
        if (c == 't' || c == 'f') return parseBoolean(json, pos);
        if (c == 'n') return parseNull(json, pos);
        return parseNumber(json, pos);
    }

    private static String parseString(String json, int[] pos) {
        if (pos[0] >= json.length() || json.charAt(pos[0]) != '"') return "";
        pos[0]++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]);
            if (c == '\\' && pos[0] + 1 < json.length()) {
                pos[0]++;
                char esc = json.charAt(pos[0]);
                switch (esc) {
                    case '"': case '\\': case '/': sb.append(esc); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u':
                        if (pos[0] + 4 < json.length()) {
                            String hex = json.substring(pos[0] + 1, pos[0] + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos[0] += 4;
                        }
                        break;
                    default: sb.append(esc);
                }
            } else if (c == '"') {
                pos[0]++; // skip closing quote
                return sb.toString();
            } else {
                sb.append(c);
            }
            pos[0]++;
        }
        return sb.toString();
    }

    private static Number parseNumber(String json, int[] pos) {
        int start = pos[0];
        boolean isDouble = false;
        if (pos[0] < json.length() && json.charAt(pos[0]) == '-') pos[0]++;
        while (pos[0] < json.length() && Character.isDigit(json.charAt(pos[0]))) pos[0]++;
        if (pos[0] < json.length() && json.charAt(pos[0]) == '.') {
            isDouble = true;
            pos[0]++;
            while (pos[0] < json.length() && Character.isDigit(json.charAt(pos[0]))) pos[0]++;
        }
        if (pos[0] < json.length() && (json.charAt(pos[0]) == 'e' || json.charAt(pos[0]) == 'E')) {
            isDouble = true;
            pos[0]++;
            if (pos[0] < json.length() && (json.charAt(pos[0]) == '+' || json.charAt(pos[0]) == '-')) pos[0]++;
            while (pos[0] < json.length() && Character.isDigit(json.charAt(pos[0]))) pos[0]++;
        }
        String numStr = json.substring(start, pos[0]);
        if (isDouble) return Double.parseDouble(numStr);
        long val = Long.parseLong(numStr);
        if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) return (int) val;
        return val;
    }

    private static Boolean parseBoolean(String json, int[] pos) {
        if (json.startsWith("true", pos[0])) { pos[0] += 4; return Boolean.TRUE; }
        if (json.startsWith("false", pos[0])) { pos[0] += 5; return Boolean.FALSE; }
        return Boolean.FALSE;
    }

    private static Object parseNull(String json, int[] pos) {
        if (json.startsWith("null", pos[0])) { pos[0] += 4; }
        return null;
    }

    private static Object parseArray(String json, int[] pos) {
        // Parse but return as string representation for simplicity
        int start = pos[0];
        int depth = 0;
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) { pos[0]++; break; } }
            else if (c == '"') { parseString(json, pos); continue; }
            pos[0]++;
        }
        return json.substring(start, pos[0]);
    }

    private static void skipWhitespace(String json, int[] pos) {
        while (pos[0] < json.length() && Character.isWhitespace(json.charAt(pos[0]))) pos[0]++;
    }
}
