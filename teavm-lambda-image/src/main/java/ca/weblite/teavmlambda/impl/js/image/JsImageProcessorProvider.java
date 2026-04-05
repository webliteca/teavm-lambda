package ca.weblite.teavmlambda.impl.js.image;

import ca.weblite.teavmlambda.api.image.ImageProcessor;
import ca.weblite.teavmlambda.api.image.ImageProcessorFactory;
import ca.weblite.teavmlambda.api.image.ImageProcessorProvider;

/**
 * Node.js/TeaVM implementation of {@link ImageProcessorProvider}.
 * Creates sharp-backed ImageProcessor instances.
 */
public class JsImageProcessorProvider implements ImageProcessorProvider {

    @Override
    public ImageProcessor create() {
        return new JsImageProcessor();
    }

    /**
     * Registers this provider with the ImageProcessorFactory.
     * Call this early in your application startup (e.g. in Main or from a PlatformAdapter).
     */
    public static void install() {
        ImageProcessorFactory.setProvider(new JsImageProcessorProvider());
    }
}
