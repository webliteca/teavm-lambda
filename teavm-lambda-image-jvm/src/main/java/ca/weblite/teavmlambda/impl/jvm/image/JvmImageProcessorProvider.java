package ca.weblite.teavmlambda.impl.jvm.image;

import ca.weblite.teavmlambda.api.image.ImageProcessor;
import ca.weblite.teavmlambda.api.image.ImageProcessorFactory;
import ca.weblite.teavmlambda.api.image.ImageProcessorProvider;

/**
 * JVM implementation of {@link ImageProcessorProvider}.
 * Creates ImageIO-backed ImageProcessor instances.
 */
public class JvmImageProcessorProvider implements ImageProcessorProvider {

    @Override
    public ImageProcessor create() {
        return new JvmImageProcessor();
    }

    /**
     * Registers this provider with the ImageProcessorFactory.
     * Call this early in your application startup.
     */
    public static void install() {
        ImageProcessorFactory.setProvider(new JvmImageProcessorProvider());
    }
}
