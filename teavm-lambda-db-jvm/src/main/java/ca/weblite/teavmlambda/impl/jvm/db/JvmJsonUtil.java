package ca.weblite.teavmlambda.impl.jvm.db;

import ca.weblite.teavmlambda.api.db.DbRow;
import ca.weblite.teavmlambda.api.db.JsonProvider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight JSON provider for JVM that parses simple JSON objects into DbRow instances.
 * Handles the common case of flat JSON objects with string, number, boolean, and null values.
 */
public class JvmJsonUtil implements JsonProvider {

    @Override
    public DbRow parse(String json) {
        if (json == null || json.isBlank()) {
            return new JdbcDbRow(Map.of());
        }
        Map<String, Object> map = parseObject(json.trim());
        return new JdbcDbRow(map);
    }

    private static Map<String, Object> parseObject(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return map;
        }
        String inner = json.substring(1, json.length() - 1).trim();
        if (inner.isEmpty()) {
            return map;
        }

        int i = 0;
        while (i < inner.length()) {
            i = skipWhitespace(inner, i);
            if (i >= inner.length()) break;

            if (inner.charAt(i) != '"') break;
            int keyEnd = findClosingQuote(inner, i + 1);
            String key = unescape(inner.substring(i + 1, keyEnd));
            i = keyEnd + 1;

            i = skipWhitespace(inner, i);
            if (i >= inner.length() || inner.charAt(i) != ':') break;
            i++;
            i = skipWhitespace(inner, i);

            Object value;
            if (inner.charAt(i) == '"') {
                int valEnd = findClosingQuote(inner, i + 1);
                value = unescape(inner.substring(i + 1, valEnd));
                i = valEnd + 1;
            } else if (inner.startsWith("null", i)) {
                value = null;
                i += 4;
            } else if (inner.startsWith("true", i)) {
                value = Boolean.TRUE;
                i += 4;
            } else if (inner.startsWith("false", i)) {
                value = Boolean.FALSE;
                i += 5;
            } else {
                int numEnd = i;
                while (numEnd < inner.length() && ",}".indexOf(inner.charAt(numEnd)) < 0
                        && !Character.isWhitespace(inner.charAt(numEnd))) {
                    numEnd++;
                }
                String numStr = inner.substring(i, numEnd);
                if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                    value = Double.parseDouble(numStr);
                } else {
                    try {
                        value = Long.parseLong(numStr);
                    } catch (NumberFormatException e) {
                        value = Double.parseDouble(numStr);
                    }
                }
                i = numEnd;
            }

            map.put(key, value);

            i = skipWhitespace(inner, i);
            if (i < inner.length() && inner.charAt(i) == ',') {
                i++;
            }
        }
        return map;
    }

    private static int skipWhitespace(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static int findClosingQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '\\') {
                i++; // skip escaped char
            } else if (s.charAt(i) == '"') {
                return i;
            }
        }
        return s.length();
    }

    private static String unescape(String s) {
        if (s.indexOf('\\') < 0) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(++i);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append('\\').append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
