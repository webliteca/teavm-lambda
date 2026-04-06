package ca.weblite.teavmlambda.api.compression;

import java.util.ServiceLoader;

/**
 * Factory for creating {@link Compressor} instances in a platform-neutral way.
 * <p>
 * Discovers the appropriate implementation via {@link ServiceLoader}.
 * On Node.js/TeaVM, uses the zlib module via JSO. On JVM, uses java.util.zip.
 */
public final class CompressorFactory {

    private static volatile CompressorProvider provider;

    private CompressorFactory() {
    }

    /**
     * Explicitly sets the compressor provider.
     */
    public static void setProvider(CompressorProvider provider) {
        CompressorFactory.provider = provider;
    }

    /**
     * Returns true if a CompressorProvider has been set or discovered.
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
     * Creates a new Compressor instance.
     */
    public static Compressor create() {
        return getProvider().create();
    }

    private static CompressorProvider getProvider() {
        CompressorProvider p = provider;
        if (p != null) {
            return p;
        }
        synchronized (CompressorFactory.class) {
            p = provider;
            if (p != null) {
                return p;
            }
            try {
                ServiceLoader<CompressorProvider> sl = ServiceLoader.load(CompressorProvider.class);
                for (CompressorProvider found : sl) {
                    provider = found;
                    return found;
                }
            } catch (Exception ignored) {
            }
            throw new IllegalStateException(
                    "No CompressorProvider found. Add teavm-lambda-compression (for Node.js) or "
                    + "teavm-lambda-compression-jvm (for JVM) to your dependencies.");
        }
    }
}
