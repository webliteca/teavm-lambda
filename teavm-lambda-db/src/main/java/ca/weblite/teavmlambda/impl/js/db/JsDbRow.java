package ca.weblite.teavmlambda.impl.js.db;

import ca.weblite.teavmlambda.api.db.DbRow;
import org.teavm.jso.JSObject;

/**
 * Adapts a JSObject (from Node.js pg result row) to the platform-neutral DbRow interface.
 */
public class JsDbRow implements DbRow {

    private final JSObject obj;

    public JsDbRow(JSObject obj) {
        this.obj = obj;
    }

    @Override
    public String getString(String column) {
        return JsUtil.getStringProperty(obj, column);
    }

    @Override
    public int getInt(String column) {
        return JsUtil.getIntProperty(obj, column);
    }

    @Override
    public double getDouble(String column) {
        return JsUtil.getDoubleProperty(obj, column);
    }

    @Override
    public boolean getBoolean(String column) {
        return JsUtil.hasProperty(obj, column) && !"false".equals(JsUtil.getStringProperty(obj, column))
                && !"0".equals(JsUtil.getStringProperty(obj, column));
    }

    @Override
    public boolean has(String column) {
        return JsUtil.hasProperty(obj, column);
    }

    @Override
    public boolean isNull(String column) {
        return !JsUtil.hasProperty(obj, column);
    }

    @Override
    public String toJson() {
        return JsUtil.toJson(obj);
    }

    /** Returns the underlying JSObject for code that needs direct JS interop. */
    public JSObject unwrap() {
        return obj;
    }
}
