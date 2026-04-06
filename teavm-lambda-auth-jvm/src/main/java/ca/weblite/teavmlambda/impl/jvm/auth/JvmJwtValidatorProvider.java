package ca.weblite.teavmlambda.impl.jvm.auth;

import ca.weblite.teavmlambda.api.auth.JwtValidator;
import ca.weblite.teavmlambda.api.auth.JwtValidatorProvider;

/**
 * JVM implementation of {@link JwtValidatorProvider}.
 * Discovered via ServiceLoader.
 */
public class JvmJwtValidatorProvider implements JwtValidatorProvider {

    @Override
    public JwtValidator create(String secret, String issuer, String algorithm) {
        return new JvmJwtValidator(secret, issuer, algorithm);
    }
}
