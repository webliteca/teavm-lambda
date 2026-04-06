package ca.weblite.teavmlambda.impl.js.auth;

import ca.weblite.teavmlambda.api.auth.JwtValidator;
import ca.weblite.teavmlambda.api.auth.JwtValidatorFactory;
import ca.weblite.teavmlambda.api.auth.JwtValidatorProvider;

/**
 * Node.js/TeaVM implementation of {@link JwtValidatorProvider}.
 * Discovered via ServiceLoader.
 */
public class JsJwtValidatorProvider implements JwtValidatorProvider {

    @Override
    public JwtValidator create(String secret, String issuer, String algorithm) {
        return new JsJwtValidator(secret, issuer, algorithm);
    }

    /**
     * Registers this provider with the JwtValidatorFactory.
     * Call this early in your application startup (e.g. in Main or from a PlatformAdapter).
     */
    public static void install() {
        JwtValidatorFactory.setProvider(new JsJwtValidatorProvider());
    }
}
