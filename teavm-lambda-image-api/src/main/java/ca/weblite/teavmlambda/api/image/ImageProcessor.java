package ca.weblite.teavmlambda.api.image;

/**
 * Platform-neutral image processor.
 * <p>
 * Supports resizing, format conversion, and compression quality control.
 * On Node.js, backed by the sharp library via JSO interop.
 * On JVM, backed by javax.imageio.ImageIO.
 */
public interface ImageProcessor {

    /**
     * Processes an image according to the given options.
     *
     * @param input the raw image bytes (PNG, JPEG, or WEBP)
     * @param options the processing options (resize, format, quality)
     * @return the processed image bytes
     * @throws RuntimeException if the image cannot be decoded or processed
     */
    byte[] process(byte[] input, ImageOptions options);
}
