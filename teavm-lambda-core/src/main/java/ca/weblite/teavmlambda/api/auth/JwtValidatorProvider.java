package ca.weblite.teavmlambda.api.auth;

/**
 * SPI for creating platform-specific {@link JwtValidator} instances.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader} or set
 * explicitly via {@link JwtValidatorFactory#setProvider(JwtValidatorProvider)}.
 */
public interface JwtValidatorProvider {

    /**
     * Creates a new JwtValidator configured from environment variables.
     * <p>
     * Expected environment variables:
     * <ul>
     *   <li>{@code JWT_SECRET} — HMAC secret key (for HS256/HS384/HS512)</li>
     *   <li>{@code JWT_PUBLIC_KEY} — PEM-encoded public key (for RS256/ES256)</li>
     *   <li>{@code JWT_ISSUER} — expected issuer (optional)</li>
     *   <li>{@code JWT_ALGORITHM} — algorithm name, defaults to HS256 (optional)</li>
     * </ul>
     *
     * @param secret     the HMAC secret or PEM public key
     * @param issuer     the expected issuer, or {@code null} to skip issuer validation
     * @param algorithm  the algorithm name (e.g. "HS256", "RS256"), or {@code null} for default
     * @return a configured JwtValidator
     */
    JwtValidator create(String secret, String issuer, String algorithm);
}
