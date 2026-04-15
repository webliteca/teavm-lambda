package ca.weblite.teavmlambda.docs;

import ca.weblite.teavmreact.core.ReactContext;
import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.hooks.Hooks;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import static ca.weblite.teavmreact.html.Html.*;

public class Router {

    public static final ReactContext ROUTE_CTX = ReactContext.create("");

    @JSBody(script = "var h = window.location.hash || ''; return h.replace(/^#\\/?/, '');")
    private static native String getCurrentHash();

    @JSBody(params = {"cb"}, script =
        "window.__routerCb = function() { cb(); };" +
        "window.addEventListener('hashchange', window.__routerCb);")
    private static native void addHashChangeListener(HashChangeCallback cb);

    @JSBody(script = "if (window.__routerCb) { window.removeEventListener('hashchange', window.__routerCb); }")
    private static native void removeHashChangeListener();

    @JSBody(script = "window.scrollTo(0, 0);")
    private static native void scrollToTop();

    @JSFunctor
    public interface HashChangeCallback extends JSObject {
        void onHashChange();
    }

    public static ReactElement create(Route[] routes) {
        return component(props -> render(props, routes), "Router");
    }

    private static ReactElement render(JSObject props, Route[] routes) {
        var currentPath = Hooks.useState(getCurrentHash());

        Hooks.useEffectOnMount(() -> {
            addHashChangeListener(() -> {
                String newPath = getCurrentHash();
                currentPath.setString(newPath);
                scrollToTop();
            });
            return () -> removeHashChangeListener();
        });

        String path = currentPath.getString();
        Route matched = null;
        for (Route route : routes) {
            if (route.path.equals(path)) {
                matched = route;
                break;
            }
        }

        ReactElement pageContent;
        boolean fullWidth;
        if (matched != null) {
            pageContent = component(matched.component, "Page");
            fullWidth = matched.fullWidth;
        } else {
            pageContent = div(
                h1("Page Not Found"),
                p("The page you're looking for doesn't exist."),
                a("#/").build(text("Go Home"))
            );
            fullWidth = false;
        }

        return ROUTE_CTX.provide(path,
            component(layoutProps -> ca.weblite.teavmlambda.docs.layout.Layout.render(layoutProps, pageContent, fullWidth), "Layout")
        );
    }
}
