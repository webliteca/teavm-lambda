package ca.weblite.teavmlambda.api.image;

/**
 * Options for image processing operations.
 * <p>
 * All fields are optional. Only fields that are explicitly set will be applied
 * during processing. Use the static factory methods or fluent setters to build options.
 * <p>
 * Example:
 * <pre>
 * ImageOptions opts = ImageOptions.resize(200, 200).format(ImageFormat.JPEG).quality(80);
 * byte[] result = processor.process(inputBytes, opts);
 * </pre>
 */
public class ImageOptions {

    private Integer width;
    private Integer height;
    private boolean preserveAspectRatio = true;
    private ImageFormat format;
    private Integer quality;

    public ImageOptions() {
    }

    /**
     * Creates options for resizing to the given dimensions.
     * Aspect ratio is preserved by default.
     */
    public static ImageOptions resize(int width, int height) {
        ImageOptions opts = new ImageOptions();
        opts.width = width;
        opts.height = height;
        return opts;
    }

    /**
     * Creates options for converting to the given format.
     */
    public static ImageOptions ofFormat(ImageFormat format) {
        ImageOptions opts = new ImageOptions();
        opts.format = format;
        return opts;
    }

    public ImageOptions width(int width) {
        this.width = width;
        return this;
    }

    public ImageOptions height(int height) {
        this.height = height;
        return this;
    }

    public ImageOptions preserveAspectRatio(boolean preserve) {
        this.preserveAspectRatio = preserve;
        return this;
    }

    public ImageOptions format(ImageFormat format) {
        this.format = format;
        return this;
    }

    /**
     * Sets the compression quality (1-100) for lossy formats (JPEG, WEBP).
     * Ignored for lossless formats like PNG.
     */
    public ImageOptions quality(int quality) {
        this.quality = quality;
        return this;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public boolean isPreserveAspectRatio() {
        return preserveAspectRatio;
    }

    public ImageFormat getFormat() {
        return format;
    }

    public Integer getQuality() {
        return quality;
    }
}
