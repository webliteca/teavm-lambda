package com.example.cloudrun;

import ca.weblite.teavmlambda.api.Container;
import ca.weblite.teavmlambda.api.Platform;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.generated.GeneratedContainer;
import ca.weblite.teavmlambda.generated.GeneratedRouter;

public class Main {
    public static void main(String[] args) {
        Container container = new GeneratedContainer();
        Router router = new GeneratedRouter(container);
        Platform.start(router);
    }
}
