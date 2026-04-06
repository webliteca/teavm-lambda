package ca.weblite.teavmlambda.demo.auth;

import ca.weblite.teavmlambda.api.Container;
import ca.weblite.teavmlambda.api.Platform;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.api.auth.JwtValidator;
import ca.weblite.teavmlambda.api.auth.JwtValidatorFactory;
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DatabaseFactory;
import ca.weblite.teavmlambda.generated.GeneratedContainer;
import ca.weblite.teavmlambda.generated.GeneratedRouter;

/**
 * Application entry point for the auth demo.
 * <p>
 * Registers both Database and JwtValidator in the container. The JwtValidator
 * reads {@code JWT_SECRET} from the environment for HMAC-SHA256 token validation.
 */
public class Main {

    public static void main(String[] args) {
        String dbUrl = Platform.env("DATABASE_URL", "postgresql://demo:demo@localhost:5432/demo");

        // Create container and register external dependencies
        Container container = new GeneratedContainer();
        container.register(Database.class, DatabaseFactory.create(dbUrl));
        container.register(JwtValidator.class, JwtValidatorFactory.create());

        Router router = new GeneratedRouter(container);
        Platform.start(router);
    }
}
