package ca.weblite.teavmlambda.docs.components;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.hooks.Hooks;
import ca.weblite.teavmreact.html.DomBuilder.*;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

public class CodeTabs {

    public static ReactElement create(String javaCode, String kotlinCode) {
        return component(props -> render(props, javaCode, kotlinCode), "CodeTabs");
    }

    private static ReactElement render(JSObject props, String javaCode, String kotlinCode) {
        var activeTab = Hooks.useState("java");
        boolean isJava = activeTab.getString().equals("java");

        return Div.create().className("code-tabs")
            .child(Div.create().className("code-tabs-header")
                .child(button("Java")
                    .className("code-tab-btn" + (isJava ? " active" : ""))
                    .onClick(e -> activeTab.setString("java"))
                    .build())
                .child(button("Kotlin")
                    .className("code-tab-btn" + (!isJava ? " active" : ""))
                    .onClick(e -> activeTab.setString("kotlin"))
                    .build()))
            .child(isJava
                ? CodeBlock.create(javaCode, "java")
                : CodeBlock.create(kotlinCode, "kotlin"))
            .build();
    }
}
