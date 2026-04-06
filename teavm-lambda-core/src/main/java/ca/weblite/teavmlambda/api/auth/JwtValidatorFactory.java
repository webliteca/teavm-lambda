package ca.weblite.teavmlambda.api.auth;

import ca.weblite.teavmlambda.api.Platform;

import java.util.ServiceLoader;

/**
 * Factory for creating {@link JwtValidator} instances in a platform-neutral way.
 * <p>
 * Discovers the appropriate implementation via {@link ServiceLoader}.
 * On Node.js/TeaVM, uses the crypto module via JSO interop.
 * On JVM, uses {@code javax.crypto} / {@code java.security}.
 * <p>
 * Usage:
 * <pre>
 * JwtValidator validator = JwtValidatorFactory.create();
 * </pre>
 */
public final class JwtValidatorFactory {

    private static volatile JwtValidatorProvider provider;

    private JwtValidatorFactory() {
    }

    /**
     * Explicitly sets the JWT validator provider.
     * Useful for testing or when ServiceLoader is not available (e.g. under TeaVM).
     */
    public static void setProvider(JwtValidatorProvider provider) {
        JwtValidatorFactory.provider = provider;
    }

    /**
     * Returns true if a JwtValidatorProvider has been set or discovered via ServiceLoader.
     */
    public static boolean isAvailable() {
        if (provider != null) return true;
        try {
            getProvider();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Creates a new JwtValidator using environment variables for configuration.
     * <p>
     * Reads {@code JWT_SECRET}, {@code JWT_PUBLIC_KEY}, {@code JWT_ISSUER},
     * and {@code JWT_ALGORITHM} from the environment via {@link Platform#env(String)}.
     *
     * @return a configured JwtValidator
     * @throws IllegalStateException if no provider is available or no secret/key is configured
     */
    public static JwtValidator create() {
        String secret = Platform.env("JWT_SECRET");
        if (secret == null || secret.isEmpty()) {
            secret = Platform.env("JWT_PUBLIC_KEY");
        }
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException(
                    "JWT_SECRET or JWT_PUBLIC_KEY environment variable must be set for JWT authentication.");
        }

        String issuer = Platform.env("JWT_ISSUER");
        if (issuer != null && issuer.isEmpty()) {
            issuer = null;
        }

        String algorithm = Platform.env("JWT_ALGORITHM");
        if (algorithm != null && algorithm.isEmpty()) {
            algorithm = null;
        }

        return getProvider().create(secret, issuer, algorithm);
    }

    /**
     * Creates a new JwtValidator with explicit configuration.
     *
     * @param secret    the HMAC secret or PEM public key
     * @param issuer    the expected issuer, or {@code null} to skip issuer validation
     * @param algorithm the algorithm name (e.g. "HS256", "RS256"), or {@code null} for default
     * @return a configured JwtValidator
     */
    public static JwtValidator create(String secret, String issuer, String algorithm) {
        return getProvider().create(secret, issuer, algorithm);
    }

    private static JwtValidatorProvider getProvider() {
        JwtValidatorProvider p = provider;
        if (p != null) {
            return p;
        }
        synchronized (JwtValidatorFactory.class) {
            p = provider;
            if (p != null) {
                return p;
            }
            try {
                ServiceLoader<JwtValidatorProvider> sl = ServiceLoader.load(JwtValidatorProvider.class);
                for (JwtValidatorProvider found : sl) {
                    provider = found;
                    return found;
                }
            } catch (Exception ignored) {
            }
            throw new IllegalStateException(
                    "No JwtValidatorProvider found. Add teavm-lambda-auth (for Node.js) or "
                    + "teavm-lambda-auth-jvm (for JVM) to your dependencies.");
        }
    }
}
