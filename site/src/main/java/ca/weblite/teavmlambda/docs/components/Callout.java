package ca.weblite.teavmlambda.docs.components;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder;
import ca.weblite.teavmreact.html.DomBuilder.*;

import static ca.weblite.teavmreact.html.Html.*;

public class Callout {

    public static ReactElement note(String title, ReactElement... content) {
        return callout("note", title, content);
    }

    public static ReactElement pitfall(String title, ReactElement... content) {
        return callout("pitfall", title, content);
    }

    public static ReactElement deepDive(String title, ReactElement... content) {
        return callout("deepdive", title, content);
    }

    private static ReactElement callout(String type, String title, ReactElement[] content) {
        DomBuilder box = Div.create().className("callout callout-" + type)
            .child(Div.create().className("callout-title").text(title));
        for (ReactElement child : content) {
            box.child(child);
        }
        return box.build();
    }
}
