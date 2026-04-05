package ca.weblite.teavmlambda.impl.js.db;

import ca.weblite.teavmlambda.api.db.DbRow;
import ca.weblite.teavmlambda.api.db.JsonProvider;

/**
 * JSON provider backed by JavaScript JSON.parse via JSO.
 */
public class JsJsonProvider implements JsonProvider {

    @Override
    public DbRow parse(String json) {
        return new JsDbRow(JsUtil.parseJson(json));
    }
}
