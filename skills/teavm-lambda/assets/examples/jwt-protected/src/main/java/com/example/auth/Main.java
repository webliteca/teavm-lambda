package com.example.auth;

import ca.weblite.teavmlambda.api.Container;
import ca.weblite.teavmlambda.api.MiddlewareRouter;
import ca.weblite.teavmlambda.api.Platform;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.api.auth.JwtValidator;
import ca.weblite.teavmlambda.api.auth.JwtValidatorFactory;
import ca.weblite.teavmlambda.api.middleware.CorsMiddleware;
import ca.weblite.teavmlambda.generated.GeneratedContainer;
import ca.weblite.teavmlambda.generated.GeneratedRouter;

public class Main {
    public static void main(String[] args) {
        Container container = new GeneratedContainer();
        container.register(JwtValidator.class, JwtValidatorFactory.create());

        Router router = new MiddlewareRouter(new GeneratedRouter(container))
                .use(CorsMiddleware.builder()
                        .allowHeaders("Content-Type", "Authorization")
                        .build());
        Platform.start(router);
    }
}
