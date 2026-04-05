package ca.weblite.teavmlambda.api.image;

/**
 * SPI for creating platform-specific {@link ImageProcessor} instances.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader} or set
 * explicitly via {@link ImageProcessorFactory#setProvider(ImageProcessorProvider)}.
 */
public interface ImageProcessorProvider {

    /**
     * Creates a new ImageProcessor instance.
     *
     * @return an ImageProcessor instance
     */
    ImageProcessor create();
}
