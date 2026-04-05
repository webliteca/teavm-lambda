package ca.weblite.teavmlambda.api.objectstore;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Factory that creates ObjectStoreClient instances based on a connection URI.
 * The URI scheme determines which provider implementation is used:
 *
 * <ul>
 *   <li>{@code s3://us-east-1} - Amazon S3 in us-east-1</li>
 *   <li>{@code s3://localhost:9000} - MinIO / S3-compatible local endpoint</li>
 *   <li>{@code gcs://my-project-id} - Google Cloud Storage</li>
 * </ul>
 *
 * Providers are discovered via {@link ServiceLoader} or registered manually
 * via {@link #register(ObjectStoreProvider)}.
 */
public final class ObjectStoreClientFactory {

    private static final List<ObjectStoreProvider> providers = new ArrayList<>();
    private static volatile boolean serviceLoaderChecked = false;

    private ObjectStoreClientFactory() {
    }

    /**
     * Registers an object store provider. Called by implementation modules.
     */
    public static void register(ObjectStoreProvider provider) {
        providers.add(provider);
    }

    /**
     * Creates an ObjectStoreClient from a connection URI.
     *
     * @param uri connection URI whose scheme selects the provider
     * @return a configured ObjectStoreClient
     * @throws IllegalArgumentException if no provider matches the URI scheme
     */
    public static ObjectStoreClient create(String uri) {
        String scheme = extractScheme(uri);
        loadServiceProviders();
        for (ObjectStoreProvider provider : providers) {
            if (provider.getScheme().equals(scheme)) {
                return provider.create(uri);
            }
        }
        throw new IllegalArgumentException(
                "No ObjectStore provider registered for scheme: " + scheme
                + ". Available schemes: " + availableSchemes()
                + ". Ensure the corresponding module (teavm-lambda-s3 or "
                + "teavm-lambda-gcs) is on the classpath.");
    }

    static String extractScheme(String uri) {
        int idx = uri.indexOf("://");
        if (idx <= 0) {
            throw new IllegalArgumentException(
                    "Invalid ObjectStore connection URI: " + uri
                    + ". Expected format: scheme://host (e.g. s3://us-east-1)");
        }
        return uri.substring(0, idx);
    }

    private static void loadServiceProviders() {
        if (serviceLoaderChecked) return;
        synchronized (ObjectStoreClientFactory.class) {
            if (serviceLoaderChecked) return;
            try {
                ServiceLoader<ObjectStoreProvider> sl = ServiceLoader.load(ObjectStoreProvider.class);
                for (ObjectStoreProvider p : sl) {
                    boolean found = false;
                    for (ObjectStoreProvider existing : providers) {
                        if (existing.getScheme().equals(p.getScheme())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        providers.add(p);
                    }
                }
            } catch (Exception ignored) {
            }
            serviceLoaderChecked = true;
        }
    }

    private static String availableSchemes() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(providers.get(i).getScheme());
        }
        sb.append("]");
        return sb.toString();
    }
}
