package ca.weblite.teavmlambda.docs.layout;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

public class Footer {

    public static ReactElement render(JSObject props, boolean fullWidth) {
        DomBuilder footer = DomBuilder.Footer.create();
        footer.className("footer" + (fullWidth ? " footer-full" : ""));
        footer.child(p(
            text("Built with "),
            a("teavm-react")
                .href("https://github.com/webliteca/teavm-react")
                .target("_blank")
                .build(),
            text(" \u2014 a serverless Java framework compiled via TeaVM. "),
            a("MIT License")
                .href("https://opensource.org/licenses/MIT")
                .target("_blank")
                .build()
        ));
        return footer.build();
    }
}
