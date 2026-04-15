package ca.weblite.teavmlambda.docs.components;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.hooks.Hooks;
import ca.weblite.teavmreact.html.DomBuilder.*;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

public class CodeBlock {

    @JSBody(script = "if (typeof Prism !== 'undefined') { Prism.highlightAll(); }")
    private static native void highlightAll();

    public static ReactElement create(String code, String language) {
        return component(props -> render(props, code, language), "CodeBlock");
    }

    private static ReactElement render(JSObject props, String code, String language) {
        Hooks.useEffectOnMount(() -> {
            highlightAll();
            return null;
        });

        return Div.create().className("code-block")
            .child(Pre.create()
                .child(Code.create()
                    .className("language-" + language)
                    .text(code)))
            .build();
    }
}
