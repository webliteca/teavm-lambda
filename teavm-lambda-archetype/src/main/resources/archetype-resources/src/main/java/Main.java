package ${package};

import ca.weblite.teavmlambda.api.Platform;
import ca.weblite.teavmlambda.generated.GeneratedContainer;
import ca.weblite.teavmlambda.generated.GeneratedRouter;

public class Main {

    public static void main(String[] args) throws Exception {
        var container = new GeneratedContainer();
        var router = new GeneratedRouter(container);
        Platform.start(router);
    }
}
