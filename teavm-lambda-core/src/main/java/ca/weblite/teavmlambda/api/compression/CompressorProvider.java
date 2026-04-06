package ca.weblite.teavmlambda.api.compression;

/**
 * SPI for creating platform-specific {@link Compressor} instances.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader} or set
 * explicitly via {@link CompressorFactory#setProvider(CompressorProvider)}.
 */
public interface CompressorProvider {

    /**
     * Creates a new Compressor instance.
     */
    Compressor create();
}
