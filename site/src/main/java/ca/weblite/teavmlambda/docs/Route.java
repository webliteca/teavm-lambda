package ca.weblite.teavmlambda.docs;

import ca.weblite.teavmreact.core.RenderFunction;

public class Route {
    public final String path;
    public final RenderFunction component;
    public final boolean fullWidth;

    public Route(String path, RenderFunction component) {
        this(path, component, false);
    }

    public Route(String path, RenderFunction component, boolean fullWidth) {
        this.path = path;
        this.component = component;
        this.fullWidth = fullWidth;
    }
}
