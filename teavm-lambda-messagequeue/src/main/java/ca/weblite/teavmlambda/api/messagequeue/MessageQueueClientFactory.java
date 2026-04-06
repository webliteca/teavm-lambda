package ca.weblite.teavmlambda.api.messagequeue;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Factory that creates MessageQueueClient instances based on a connection URI.
 * The URI scheme determines which provider implementation is used:
 *
 * <ul>
 *   <li>{@code sqs://us-east-1} - Amazon SQS in us-east-1</li>
 *   <li>{@code sqs://localhost:9324} - ElasticMQ / SQS-compatible local endpoint</li>
 *   <li>{@code pubsub://my-project-id} - Google Cloud Pub/Sub</li>
 * </ul>
 *
 * Providers are discovered via {@link ServiceLoader} or registered manually
 * via {@link #register(MessageQueueProvider)}.
 */
public final class MessageQueueClientFactory {

    private static final List<MessageQueueProvider> providers = new ArrayList<>();
    private static volatile boolean serviceLoaderChecked = false;

    private MessageQueueClientFactory() {
    }

    /**
     * Registers a message queue provider. Called by implementation modules.
     */
    public static void register(MessageQueueProvider provider) {
        providers.add(provider);
    }

    /**
     * Creates a MessageQueueClient from a connection URI.
     *
     * @param uri connection URI whose scheme selects the provider
     * @return a configured MessageQueueClient
     * @throws IllegalArgumentException if no provider matches the URI scheme
     */
    public static MessageQueueClient create(String uri) {
        String scheme = extractScheme(uri);
        loadServiceProviders();
        for (MessageQueueProvider provider : providers) {
            if (provider.getScheme().equals(scheme)) {
                return provider.create(uri);
            }
        }
        throw new IllegalArgumentException(
                "No MessageQueue provider registered for scheme: " + scheme
                + ". Available schemes: " + availableSchemes()
                + ". Ensure the corresponding module (teavm-lambda-sqs or "
                + "teavm-lambda-pubsub) is on the classpath.");
    }

    static String extractScheme(String uri) {
        int idx = uri.indexOf("://");
        if (idx <= 0) {
            throw new IllegalArgumentException(
                    "Invalid MessageQueue connection URI: " + uri
                    + ". Expected format: scheme://host (e.g. sqs://us-east-1)");
        }
        return uri.substring(0, idx);
    }

    private static void loadServiceProviders() {
        if (serviceLoaderChecked) return;
        synchronized (MessageQueueClientFactory.class) {
            if (serviceLoaderChecked) return;
            try {
                ServiceLoader<MessageQueueProvider> sl = ServiceLoader.load(MessageQueueProvider.class);
                for (MessageQueueProvider p : sl) {
                    boolean found = false;
                    for (MessageQueueProvider existing : providers) {
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
