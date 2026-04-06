package ca.weblite.teavmlambda.api.auth;

/**
 * Platform-neutral interface for validating JWT tokens.
 * <p>
 * On Node.js, backed by the {@code crypto} module via JSO interop.
 * On JVM, backed by {@code javax.crypto} / {@code java.security}.
 * <p>
 * Implementations are discovered via {@link JwtValidatorFactory}.
 */
public interface JwtValidator {

    /**
     * Validates a JWT token string and returns the decoded claims.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return the decoded claims if the token is valid
     * @throws JwtValidationException if the token is invalid, expired, or has a bad signature
     */
    JwtClaims validate(String token) throws JwtValidationException;
}
