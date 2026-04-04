package io.teavmlambda.db;

import io.teavmlambda.db.api.DbRow;
import io.teavmlambda.db.api.JsonUtil;

/**
 * JSON utility implementation backed by JavaScript JSON.parse/JSON.stringify via JSO.
 */
public class JsJsonUtil implements JsonUtil {

    @Override
    public DbRow parseJson(String json) {
        return new JsDbRow(JsUtil.parseJson(json));
    }

    @Override
    public String toJson(DbRow row) {
        return row.toJson();
    }
}
