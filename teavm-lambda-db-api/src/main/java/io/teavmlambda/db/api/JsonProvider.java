package io.teavmlambda.db.api;

/**
 * SPI for platform-specific JSON parsing.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader} or set
 * explicitly via {@link Json#setProvider(JsonProvider)}.
 */
public interface JsonProvider {

    /**
     * Parses a JSON object string into a {@link DbRow}.
     *
     * @param json a JSON object string
     * @return a DbRow whose fields correspond to the JSON properties
     */
    DbRow parse(String json);
}
