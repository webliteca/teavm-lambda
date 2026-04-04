package io.teavmlambda.db;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;

public interface PgResult extends JSObject {

    @JSProperty
    JSArray<JSObject> getRows();

    @JSProperty
    int getRowCount();
}
