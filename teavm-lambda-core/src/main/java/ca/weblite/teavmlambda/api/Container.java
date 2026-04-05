package ca.weblite.teavmlambda.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight dependency injection container.
 * <p>
 * Stores factories (suppliers) keyed by class. Components marked
 * {@link ca.weblite.teavmlambda.api.annotation.Singleton @Singleton} are
 * cached after first creation. The annotation processor generates a
 * {@code GeneratedContainer} subclass that registers all discovered
 * components at compile time.
 * <p>
 * External dependencies (e.g. {@code Database}) can be registered manually
 * before the generated wiring runs:
 * <pre>
 * container.register(Database.class, db);
 * </pre>
 */
public class Container {

    @FunctionalInterface
    public interface Factory<T> {
        T create();
    }

    private final Map<String, Factory<?>> factories = new HashMap<>();
    private final Map<String, Object> singletons = new HashMap<>();

    /**
     * Registers a factory for the given type.
     */
    public <T> void register(Class<T> type, Factory<T> factory) {
        factories.put(type.getName(), factory);
    }

    /**
     * Registers an existing instance (singleton) for the given type.
     */
    public <T> void register(Class<T> type, T instance) {
        singletons.put(type.getName(), instance);
    }

    /**
     * Registers a factory that caches the result after first invocation.
     */
    public <T> void registerSingleton(Class<T> type, Factory<T> factory) {
        String key = type.getName();
        factories.put(key, () -> {
            Object existing = singletons.get(key);
            if (existing != null) {
                return existing;
            }
            T instance = factory.create();
            singletons.put(key, instance);
            return instance;
        });
    }

    /**
     * Retrieves an instance of the given type.
     *
     * @throws IllegalStateException if no factory or instance is registered
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        String key = type.getName();

        // Check singletons first
        Object singleton = singletons.get(key);
        if (singleton != null) {
            return (T) singleton;
        }

        // Try factory
        Factory<?> factory = factories.get(key);
        if (factory != null) {
            return (T) factory.create();
        }

        throw new IllegalStateException(
                "No binding found for " + type.getName()
                + ". Register it with container.register() or annotate it with @Component/@Service/@Repository.");
    }

    /**
     * Returns true if the container has a binding for the given type.
     */
    public boolean has(Class<?> type) {
        String key = type.getName();
        return singletons.containsKey(key) || factories.containsKey(key);
    }
}
