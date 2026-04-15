package ca.weblite.teavmlambda.docs.layout;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.hooks.Hooks;
import ca.weblite.teavmreact.html.DomBuilder.*;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

public class Layout {

    public static ReactElement render(JSObject props, ReactElement pageContent, boolean fullWidth) {
        var sidebarOpen = Hooks.useState(false);

        return Div.create().className("layout")
            .child(component(headerProps -> Header.render(headerProps, () -> sidebarOpen.setBool(!sidebarOpen.getBool())), "Header"))
            .child(Div.create().className("layout-body")
                .child(component(sidebarProps -> Sidebar.render(sidebarProps, sidebarOpen.getBool(), () -> sidebarOpen.setBool(false)), "Sidebar"))
                .child(Main.create().className(fullWidth ? "content-full" : "content")
                    .child(pageContent)))
            .child(component(footerProps -> Footer.render(footerProps, fullWidth), "Footer"))
            .build();
    }
}
