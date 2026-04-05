package ca.weblite.teavmlambda.api.image;

import java.util.ServiceLoader;

/**
 * Factory for creating {@link ImageProcessor} instances in a platform-neutral way.
 * <p>
 * Discovers the appropriate implementation via {@link ServiceLoader}.
 * On Node.js/TeaVM, this creates a sharp-backed processor; on JVM, an ImageIO-backed one.
 * <p>
 * Usage:
 * <pre>
 * ImageProcessor processor = ImageProcessorFactory.create();
 * byte[] result = processor.process(inputBytes, ImageOptions.resize(200, 200));
 * </pre>
 */
public final class ImageProcessorFactory {

    private static volatile ImageProcessorProvider provider;

    private ImageProcessorFactory() {
    }

    /**
     * Explicitly sets the image processor provider.
     * Useful for testing or when ServiceLoader is not available (e.g. under TeaVM).
     */
    public static void setProvider(ImageProcessorProvider provider) {
        ImageProcessorFactory.provider = provider;
    }

    /**
     * Returns true if an ImageProcessorProvider has been set or discovered via ServiceLoader.
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
     * Creates a new ImageProcessor instance.
     *
     * @return an ImageProcessor instance
     * @throws IllegalStateException if no provider is available
     */
    public static ImageProcessor create() {
        return getProvider().create();
    }

    private static ImageProcessorProvider getProvider() {
        ImageProcessorProvider p = provider;
        if (p != null) {
            return p;
        }
        synchronized (ImageProcessorFactory.class) {
            p = provider;
            if (p != null) {
                return p;
            }
            try {
                ServiceLoader<ImageProcessorProvider> sl = ServiceLoader.load(ImageProcessorProvider.class);
                for (ImageProcessorProvider found : sl) {
                    provider = found;
                    return found;
                }
            } catch (Exception ignored) {
            }
            throw new IllegalStateException(
                    "No ImageProcessorProvider found. Add teavm-lambda-image (for Node.js) or "
                    + "teavm-lambda-image-jvm (for JVM) to your dependencies.");
        }
    }
}
