package io.teavmlambda.db;

import io.teavmlambda.db.api.DbRow;
import io.teavmlambda.db.api.JsonProvider;

/**
 * JSON provider backed by JavaScript JSON.parse via JSO.
 */
public class JsJsonProvider implements JsonProvider {

    @Override
    public DbRow parse(String json) {
        return new JsDbRow(JsUtil.parseJson(json));
    }
}
