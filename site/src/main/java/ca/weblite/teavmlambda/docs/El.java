package ca.weblite.teavmlambda.docs;

import ca.weblite.teavmreact.core.React;
import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.Style;
import org.teavm.jso.JSObject;

public final class El {

    private El() {}

    public static ReactElement classed(String tag, String className, ReactElement... children) {
        JSObject props = React.createObject();
        React.setProperty(props, "className", className);
        JSObject arr = React.createArray();
        for (ReactElement c : children) {
            React.arrayPush(arr, c);
        }
        return React.createElementFromArray(tag, props, arr);
    }

    public static ReactElement classedText(String tag, String className, String text) {
        JSObject props = React.createObject();
        React.setProperty(props, "className", className);
        return React.createElementWithText(tag, props, text);
    }

    public static ReactElement div(String className, ReactElement... children) {
        return classed("div", className, children);
    }

    public static ReactElement span(String className, ReactElement... children) {
        return classed("span", className, children);
    }

    public static ReactElement section(String className, ReactElement... children) {
        return classed("section", className, children);
    }

    public static ReactElement nav(String className, ReactElement... children) {
        return classed("nav", className, children);
    }

    public static ReactElement table(String className, ReactElement... children) {
        return classed("table", className, children);
    }

    public static ReactElement p(String className, String text) {
        return classedText("p", className, text);
    }

    public static ReactElement p(String className, ReactElement... children) {
        return classed("p", className, children);
    }
}
