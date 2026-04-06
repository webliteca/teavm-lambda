package ca.weblite.teavmlambda.api.compression;

/**
 * Platform-neutral interface for HTTP response compression.
 * <p>
 * On Node.js, backed by the zlib module via JSO.
 * On JVM, backed by {@code java.util.zip}.
 */
public interface Compressor {

    /**
     * Compresses data using gzip encoding.
     *
     * @param data the uncompressed data
     * @return the gzip-compressed data
     */
    String gzip(String data);

    /**
     * Compresses data using deflate encoding.
     *
     * @param data the uncompressed data
     * @return the deflate-compressed data
     */
    String deflate(String data);

    /**
     * Returns true if this compressor supports the given encoding.
     */
    default boolean supports(String encoding) {
        return "gzip".equalsIgnoreCase(encoding) || "deflate".equalsIgnoreCase(encoding);
    }
}
