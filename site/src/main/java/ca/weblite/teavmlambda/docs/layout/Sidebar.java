package ca.weblite.teavmlambda.docs.layout;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmlambda.docs.Router;
import ca.weblite.teavmreact.html.DomBuilder;
import ca.weblite.teavmreact.html.DomBuilder.*;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

public class Sidebar {

    public static ReactElement render(JSObject props, boolean open, Runnable onClose) {
        String currentPath = Router.ROUTE_CTX.useString();

        return Nav.create().className("sidebar" + (open ? " open" : ""))
            .child(sidebarSection("Learn",
                link("Quick Start", "learn/quick-start", currentPath, onClose),
                link("Installation", "learn/installation", currentPath, onClose),
                link("Routing", "learn/routing", currentPath, onClose),
                link("Dependency Injection", "learn/dependency-injection", currentPath, onClose),
                link("Request & Response", "learn/request-response", currentPath, onClose),
                link("Middleware", "learn/middleware", currentPath, onClose),
                link("Database", "learn/database", currentPath, onClose),
                link("Cloud Services", "learn/cloud-services", currentPath, onClose),
                link("Security", "learn/security", currentPath, onClose),
                link("Validation", "learn/validation", currentPath, onClose),
                link("Deployment", "learn/deployment", currentPath, onClose)
            ))
            .child(sidebarSection("Reference",
                link("Annotations", "reference/annotations", currentPath, onClose),
                link("Core API", "reference/core-api", currentPath, onClose),
                link("Kotlin DSL", "reference/kotlin-dsl", currentPath, onClose),
                link("Environment Variables", "reference/environment", currentPath, onClose)
            ))
            .build();
    }

    private static ReactElement sidebarSection(String title, ReactElement... links) {
        DomBuilder section = Div.create().className("sidebar-section")
            .child(P.create().className("sidebar-section-title").text(title));
        for (ReactElement link : links) {
            section.child(link);
        }
        return section.build();
    }

    private static ReactElement link(String label, String path, String currentPath, Runnable onClose) {
        boolean active = currentPath.equals(path);
        return A.create()
            .href("#/" + path)
            .className("sidebar-link" + (active ? " active" : ""))
            .onClick(e -> onClose.run())
            .text(label)
            .build();
    }
}
